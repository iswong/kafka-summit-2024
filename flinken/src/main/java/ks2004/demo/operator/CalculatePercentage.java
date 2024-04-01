package ks2004.demo.operator;

import java.util.List;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.stream.Collectors;
import ks2004.demo.model.ExposureSnapshot;
import ks2004.demo.model.ExposureSnapshot.Exposure;
import org.apache.flink.api.common.functions.RichFlatMapFunction;
import org.apache.flink.api.common.state.MapState;
import org.apache.flink.api.common.state.MapStateDescriptor;
import org.apache.flink.api.common.state.ValueState;
import org.apache.flink.api.common.state.ValueStateDescriptor;
import org.apache.flink.api.common.typeinfo.BasicTypeInfo;
import org.apache.flink.api.common.typeinfo.TypeHint;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.util.Collector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CalculatePercentage extends
    RichFlatMapFunction<ExposureSnapshot, ExposureSnapshot> {

  private static final Logger LOG = LoggerFactory.getLogger(MultiplyQtyAndPrice.class);

  private transient MapState<String, ExposureSnapshot> exposureMapByAccountCode;
  private transient ValueState<Double> totalSodEmvState;
  private transient ValueState<Double> totalSodTmv;
  private transient ValueState<Double> totalExpectedEmv;
  private transient ValueState<Double> totalExpectedTmv;

  @Override
  public void open(final Configuration parameters) {
    totalSodEmvState = getRuntimeContext()
        .getState(
            new ValueStateDescriptor<>(
                "CalculatePercentage.TotalSodEmv",
                BasicTypeInfo.DOUBLE_TYPE_INFO));

    exposureMapByAccountCode = getRuntimeContext()
        .getMapState(
            new MapStateDescriptor<>(
                "CalculatePercentage.ExposureMap",
                BasicTypeInfo.STRING_TYPE_INFO,
                TypeInformation.of(new TypeHint<>() {
                }))
        );

  }

  @Override
  public void flatMap(
      final ExposureSnapshot incoming,
      final Collector<ExposureSnapshot> collector)
      throws Exception {

    LOG.info("Child account {} of {} updated. Recalculate exposure ...",
        incoming.getAccountCode(),
        incoming.getParentAccount());

    final double existingTotalSodEmv;

    final double prevTotalSodEmv = Optional.ofNullable(totalSodEmvState.value()).orElse(0d);

    if (exposureMapByAccountCode.contains(incoming.getAccountCode())) {
      final ExposureSnapshot existing = exposureMapByAccountCode.get(incoming.getAccountCode());
      existingTotalSodEmv = existing.getTotalSodEmv();
    } else {
      existingTotalSodEmv = 0d;
    }

    final double currentTotalSodEmv =
        prevTotalSodEmv - existingTotalSodEmv + incoming.getTotalSodEmv();

    totalSodEmvState.update(currentTotalSodEmv);
    exposureMapByAccountCode.put(incoming.getAccountCode(), incoming);

    for (final Entry<String, ExposureSnapshot> entry : exposureMapByAccountCode.entries()) {

      final String accountCode = entry.getKey();
      final ExposureSnapshot exposureSnapshot = entry.getValue();

      final List<Exposure> updatedExposures = exposureSnapshot.getExposures().stream()
          .map(e -> {
            e.setSodEmvPercent(e.getSodEmv() / currentTotalSodEmv);
            e.setSodEmvPercent(e.getSodTmv() / currentTotalSodEmv);
            e.setExpectedEmvPercent(e.getExpectedEmv() / currentTotalSodEmv);
            e.setExpectedTmvPercent(e.getExpectedTmv() / currentTotalSodEmv);
            return e;
          })
          .collect(Collectors.toList());

      collector.collect(
          exposureSnapshot.toBuilder()
              .exposures(updatedExposures)
              .build());
    }
  }
}
