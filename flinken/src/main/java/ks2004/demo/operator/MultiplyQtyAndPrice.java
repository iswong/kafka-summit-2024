package ks2004.demo.operator;

import java.util.List;
import java.util.concurrent.atomic.DoubleAdder;
import java.util.stream.Collectors;
import ks2004.demo.model.ExposureSnapshot;
import ks2004.demo.model.ExposureSnapshot.Exposure;
import ks2004.demo.model.HoldingSnapshot;
import ks2004.demo.model.PriceSnapshot;
import org.apache.flink.api.common.state.MapState;
import org.apache.flink.api.common.state.MapStateDescriptor;
import org.apache.flink.api.common.typeinfo.BasicTypeInfo;
import org.apache.flink.api.common.typeinfo.TypeHint;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.streaming.api.functions.co.KeyedBroadcastProcessFunction;
import org.apache.flink.util.Collector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MultiplyQtyAndPrice
    extends
    KeyedBroadcastProcessFunction<String, HoldingSnapshot, PriceSnapshot, ExposureSnapshot> {

  public static final MapStateDescriptor<String, PriceSnapshot> PRICE_SNAPSHOT_STATE =
      new MapStateDescriptor<>(
          "MultiplyQtyAndPrice.PriceSnapshotState",
          BasicTypeInfo.STRING_TYPE_INFO,
          TypeInformation.of(new TypeHint<>() {
          }));

  public static final MapStateDescriptor<String, HoldingSnapshot> HOLDING_SNAPSHOT_STATE =
      new MapStateDescriptor<>(
          "MultiplyQtyAndPrice.HoldingSnapshotState",
          BasicTypeInfo.STRING_TYPE_INFO,
          TypeInformation.of(new TypeHint<>() {
          }));

  private static final Logger LOG = LoggerFactory.getLogger(MultiplyQtyAndPrice.class);
  private static final String LATEST = "_LATEST_";


  @Override
  public void processBroadcastElement(
      final PriceSnapshot priceSnapshot,
      KeyedBroadcastProcessFunction<String, HoldingSnapshot, PriceSnapshot, ExposureSnapshot>.Context ctx,
      Collector<ExposureSnapshot> out) throws Exception {

    ctx.getBroadcastState(PRICE_SNAPSHOT_STATE).put(LATEST, priceSnapshot);

    ctx.applyToKeyedState(HOLDING_SNAPSHOT_STATE,

        (s, holdingSnapshotStates) -> {

          if (holdingSnapshotStates.contains(s)) {

            LOG.info("processBroadcastElement {} : {} : {}", s, priceSnapshot,
                holdingSnapshotStates.entries());

            final HoldingSnapshot holdingSnapshot = holdingSnapshotStates.get(s);

            out.collect(apply(priceSnapshot, holdingSnapshot));
          }
        });

  }

  @Override
  public void processElement(
      final HoldingSnapshot holdingSnapshot,
      final KeyedBroadcastProcessFunction<String, HoldingSnapshot, PriceSnapshot, ExposureSnapshot>.ReadOnlyContext ctx,
      final Collector<ExposureSnapshot> out) throws Exception {

    final PriceSnapshot priceSnapshot = ctx.getBroadcastState(PRICE_SNAPSHOT_STATE)
        .get(LATEST);

    final MapState<String, HoldingSnapshot> holdingSnapshotHandle =
        getRuntimeContext()
            .getMapState(HOLDING_SNAPSHOT_STATE);

    holdingSnapshotHandle.put(holdingSnapshot.getAccountCode(), holdingSnapshot);

    LOG.info("processElement: priceSnapshot: {} accountCode: {}",
        priceSnapshot,
        holdingSnapshotHandle.entries());

    if (priceSnapshot != null) {
      out.collect(apply(priceSnapshot, holdingSnapshot));
    }
  }

  ExposureSnapshot apply(final PriceSnapshot prices, final HoldingSnapshot holdings) {

    final DoubleAdder totalSodEmv = new DoubleAdder();
    final DoubleAdder totalSodTmv = new DoubleAdder();
    final DoubleAdder totalExpectedEmv = new DoubleAdder();
    final DoubleAdder totalExpectedTmv = new DoubleAdder();

    final List<Exposure> exposures = holdings.getHoldings().stream()
        .map(h -> this.apply(prices, h))
        .peek(e -> {
          totalSodEmv.add(e.getSodEmv());
          totalSodTmv.add(e.getSodTmv());
          totalExpectedEmv.add(e.getExpectedEmv());
          totalExpectedTmv.add(e.getExpectedTmv());
        })
        .collect(Collectors.toList());

    return ExposureSnapshot.builder()
        .parentAccount(holdings.getParentAccount())
        .accountCode(holdings.getAccountCode())
        .exposures(exposures)
        .totalSodEmv(totalSodEmv.doubleValue())
        .totalSodTmv(totalSodTmv.doubleValue())
        .totalExpectedEmv(totalExpectedEmv.doubleValue())
        .totalExpectedTmv(totalExpectedTmv.doubleValue())
        .build();
  }

  Exposure apply(final PriceSnapshot prices,
      final HoldingSnapshot.Holding holding) {

    final double sodQty = holding.getSodQty();
    final double orderQty = holding.getOrderQty();
    final double expectedQty = sodQty + orderQty;

    final double price = prices.getPrices().getOrDefault(holding.getSymbol(), 0d);

    return Exposure.builder()
        .sodEmv(sodQty * price)
        .sodTmv(sodQty * price)
        .expectedEmv(expectedQty * price)
        .expectedTmv(expectedQty * price)
        .build();
  }

}
