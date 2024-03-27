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

import ks2004.demo.model.ExposureSnapshot;
import ks2004.demo.model.HoldingSnapshot;
import ks2004.demo.model.PriceSnapshot;
import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.api.common.state.MapState;
import org.apache.flink.api.common.state.MapStateDescriptor;
import org.apache.flink.api.common.state.ValueState;
import org.apache.flink.api.common.state.ValueStateDescriptor;
import org.apache.flink.api.common.typeinfo.BasicTypeInfo;
import org.apache.flink.api.common.typeinfo.TypeHint;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.api.java.typeutils.ListTypeInfo;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.connector.file.sink.FileSink;
import org.apache.flink.connector.kafka.source.KafkaSource;
import org.apache.flink.connector.kafka.source.enumerator.initializer.OffsetsInitializer;
import org.apache.flink.core.fs.Path;
import org.apache.flink.runtime.state.KeyedStateFunction;
import org.apache.flink.streaming.api.datastream.*;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.co.CoMapFunction;
import org.apache.flink.streaming.api.functions.co.KeyedBroadcastProcessFunction;
import org.apache.flink.streaming.api.functions.co.RichCoMapFunction;
import org.apache.flink.util.Collector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;
import java.util.Map;

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
	public static void main(String[] args) throws Exception {


		// Sets up the execution environment, which is the main entry point
		// to building Flink applications.
		final StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment()
				.setParallelism(1);

		String groupId = String.valueOf(System.currentTimeMillis());
		KafkaSource<PriceSnapshot> priceSrc = KafkaSource.<PriceSnapshot>builder()
				.setBootstrapServers("localhost:51932")
				.setTopics("local.price")
				.setGroupId(groupId)
				.setStartingOffsets(OffsetsInitializer.earliest())
				.setValueOnlyDeserializer(new JsonDeserializationSchema<>(PriceSnapshot.class))
				.build();

		KafkaSource<HoldingSnapshot> holdingSrc = KafkaSource.<HoldingSnapshot>builder()
				.setBootstrapServers("localhost:51932")
				.setTopics("local.holding")
				.setGroupId(groupId)
				.setStartingOffsets(OffsetsInitializer.earliest())
				.setValueOnlyDeserializer(new JsonDeserializationSchema<>(HoldingSnapshot.class))
				.build();


		KeyedStream<HoldingSnapshot, String> holdingStream = env.fromSource(holdingSrc, WatermarkStrategy.noWatermarks(), "holdingStream")
				.keyBy(HoldingSnapshot::getAccountCode);

		DataStreamSource<PriceSnapshot> priceStream = env.fromSource(priceSrc, WatermarkStrategy.noWatermarks(), "priceStream");
		BroadcastStream<PriceSnapshot> broadcast = priceStream.broadcast(
				new MapStateDescriptor<>("PriceSnapshotState", BasicTypeInfo.STRING_TYPE_INFO, TypeInformation.of(new TypeHint<>() {})));
//		DataStream<PriceSnapshot> broadcast = priceStream.broadcast();

		BroadcastConnectedStream<HoldingSnapshot, PriceSnapshot> connect = holdingStream.connect(broadcast);

		SingleOutputStreamOperator<ExposureSnapshot> map = connect.process(new ExposureCalculate());
		map.print();


		// Execute program, beginning computation.
		env.execute("JoinStream");
	}

	public static class ExposureCalculate
			extends KeyedBroadcastProcessFunction<String, HoldingSnapshot, PriceSnapshot, ExposureSnapshot> {

		private static final Logger LOG = LoggerFactory.getLogger(ExposureCalculate.class);

		private final MapStateDescriptor<String, HoldingSnapshot> holdingSnapshotStateDescriptor =
				new MapStateDescriptor<>(
						"HoldingSnapshotState",
						BasicTypeInfo.STRING_TYPE_INFO,
						TypeInformation.of(new TypeHint<>() {}));

		// identical to our ruleStateDescriptor above
		private final MapStateDescriptor<String, PriceSnapshot> priceSnapshotStateDescriptor =
				new MapStateDescriptor<>(
						"PriceSnapshotState",
						BasicTypeInfo.STRING_TYPE_INFO,
						TypeInformation.of(new TypeHint<>() {}));


//		@Override
//		public ExposureSnapshot map1(HoldingSnapshot holdingSnapshot) throws Exception {
//			LOG.info("--->>> holdingSnapshot from {}", holdingSnapshotState.value());
//			holdingSnapshotState.update(holdingSnapshot);
//			LOG.info("--->>> holdingSnapshot to {}", holdingSnapshotState.value());
//
//			return new ExposureSnapshot();
//		}
//
//		@Override
//		public ExposureSnapshot map2(PriceSnapshot priceSnapshot) throws Exception {
//			LOG.info("--->>> priceSnapshot from {}", priceSnapshotState.value());
//			priceSnapshotState.update(priceSnapshot);
//			LOG.info("--->>> priceSnapshot from {}", priceSnapshotState.value());
//
//			return new ExposureSnapshot();
//		}

//		@Override
//		public void open(Configuration parameters) throws Exception {
//			this.priceSnapshotState = getRuntimeContext()
//					.getState(new ValueStateDescriptor<>("prices", PriceSnapshot.class));
//			this.holdingSnapshotState = getRuntimeContext()
//					.getState(new ValueStateDescriptor<>("quantities", HoldingSnapshot.class));
//		}


		@Override
		public void processBroadcastElement(
				final PriceSnapshot value,
				KeyedBroadcastProcessFunction<String, HoldingSnapshot, PriceSnapshot, ExposureSnapshot>.Context ctx,
				Collector<ExposureSnapshot> out) throws Exception {

			MapState<String, HoldingSnapshot> mapState = getRuntimeContext().getMapState(holdingSnapshotStateDescriptor);
			ctx.getBroadcastState(priceSnapshotStateDescriptor).put(value.getAsOf(), value);
			ctx.applyToKeyedState(holdingSnapshotStateDescriptor, new KeyedStateFunction<String, MapState<String, HoldingSnapshot>>() {
				@Override
				public void process(String s, MapState<String, HoldingSnapshot> keyedState) throws Exception {
					LOG.info("processBroadcastElement {} : {} : {}", s, value, keyedState.entries());
				}
			});
//			if (mapState.isEmpty()) {
//				LOG.info("processBroadcastElement: priceSnapshot: {} accountCodes: {}", value, Collections.emptyList());
//			}
//			else {
//				LOG.info("processBroadcastElement: priceSnapshot: {} accountCodes: {}", value, mapState.entries());
//				out.collect(new ExposureSnapshot());
//
//			}

		}
		@Override
		public void processElement(
				HoldingSnapshot value,
				KeyedBroadcastProcessFunction<String, HoldingSnapshot, PriceSnapshot, ExposureSnapshot>.ReadOnlyContext ctx,
				Collector<ExposureSnapshot> out) throws Exception {
			Iterable<Map.Entry<String, PriceSnapshot>> entries = ctx.getBroadcastState(priceSnapshotStateDescriptor).immutableEntries();
			MapState<String, HoldingSnapshot> mapState = getRuntimeContext().getMapState(holdingSnapshotStateDescriptor);
			mapState.put(value.getAccountCode(), value);
			LOG.info("processElement: priceSnapshot: {} accountCode: {}", entries, mapState.entries());

			out.collect(new ExposureSnapshot());
		}
	}


}
