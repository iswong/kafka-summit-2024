/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ks2004.demo;

import static ks2004.demo.operator.MultiplyQtyAndPrice.PRICE_SNAPSHOT_STATE;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import ks2004.demo.model.ExposureSnapshot;
import ks2004.demo.model.HoldingSnapshot;
import ks2004.demo.model.PriceSnapshot;
import ks2004.demo.operator.CalculatePercentage;
import ks2004.demo.operator.MultiplyQtyAndPrice;
import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.connector.file.sink.FileSink;
import org.apache.flink.connector.kafka.source.KafkaSource;
import org.apache.flink.connector.kafka.source.enumerator.initializer.OffsetsInitializer;
import org.apache.flink.core.fs.Path;
import org.apache.flink.streaming.api.datastream.BroadcastConnectedStream;
import org.apache.flink.streaming.api.datastream.BroadcastStream;
import org.apache.flink.streaming.api.datastream.KeyedStream;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Skeleton for a Flink DataStream Job.
 *
 * <p>For a tutorial how to write a Flink application, check the
 * tutorials and examples on the <a href="https://flink.apache.org">Flink Website</a>.
 *
 * <p>To package your application into a JAR file for execution, run
 * 'mvn clean package' on the command line.
 *
 * <p>If you change the name of the main class (with the public static void main(String[] args))
 * method, change the respective entry in the POM.xml file (simply search for 'mainClass').
 */
public class KafkaStreamJob {

  private static final Logger LOG = LoggerFactory.getLogger(MultiplyQtyAndPrice.class);

  public static void main(final String[] args) throws Exception {

    // Sets up the execution environment, which is the main entry point
    // to building Flink applications.
    final StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment()
        .enableCheckpointing(10000L)
        .setParallelism(1);

    final String groupId = String.valueOf(System.currentTimeMillis());

    final KafkaSource<PriceSnapshot> priceSrc = KafkaSource.<PriceSnapshot>builder()
        .setBootstrapServers("localhost:51932")
        .setTopics("local.price")
        .setGroupId(groupId)
        .setStartingOffsets(OffsetsInitializer.earliest())
        .setValueOnlyDeserializer(new JsonDeserializationSchema<>(PriceSnapshot.class))
        .build();

    final KafkaSource<HoldingSnapshot> holdingSrc = KafkaSource.<HoldingSnapshot>builder()
        .setBootstrapServers("localhost:51932")
        .setTopics("local.holding")
        .setGroupId(groupId)
        .setStartingOffsets(OffsetsInitializer.earliest())
        .setValueOnlyDeserializer(new JsonDeserializationSchema<>(HoldingSnapshot.class))
        .build();

    final KeyedStream<HoldingSnapshot, String> holdingStream =
        env.fromSource(holdingSrc, WatermarkStrategy.noWatermarks(), "holdingStream")
            .keyBy(HoldingSnapshot::getAccountCode);

    final BroadcastStream<PriceSnapshot> priceStream =
        env.fromSource(priceSrc, WatermarkStrategy.noWatermarks(), "priceStream")
            .broadcast(PRICE_SNAPSHOT_STATE);

    final BroadcastConnectedStream<HoldingSnapshot, PriceSnapshot> jointHoldingPrice =
        holdingStream
            .connect(priceStream);

    final SingleOutputStreamOperator<ExposureSnapshot> marketValuedHoldingStream =
        jointHoldingPrice
            .process(new MultiplyQtyAndPrice());

    final KeyedStream<ExposureSnapshot, String> groupedByParent =
        marketValuedHoldingStream.keyBy(ExposureSnapshot::getParentAccount);

    final SingleOutputStreamOperator<ExposureSnapshot> exposureStream =
        groupedByParent.flatMap(new CalculatePercentage());

    final FileSink<ExposureSnapshot> sink = FileSink.<ExposureSnapshot>forBulkFormat(
            new Path("file:///Users/chriswong/Projects/kafka-summit-2024/output"),
            outputStream ->
                CsvBulkWriter.<ExposureSnapshot>builder()
                    .stream(outputStream)
                    .converter(KafkaStreamJob::toCsv)
                    .build())
        .withBucketAssigner(new CustomBucketAssigner())
        .withRollingPolicy(new OnEventCheckpointRollingPolicy())
        .build();

    exposureStream.sinkTo(sink);

    // Execute program, beginning computation.
    env.execute("JoinStream");
  }

  static List<String[]> toCsv(final ExposureSnapshot exposureSnapshot) {
    final ArrayList<String[]> lines = new ArrayList<>(
        exposureSnapshot.getExposures().stream()
            .map(exposure -> new String[]{
                exposureSnapshot.getAccountCode(),
                exposureSnapshot.getParentAccount(),
                exposure.getSymbol(),
                String.valueOf(exposure.getSodEmvPercent())
            })
            .collect(Collectors.toList()));
    return lines;
  }


}
