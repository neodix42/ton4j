package org.ton.ton4j.exporter.lazy;

import java.io.Serializable;
import lombok.Builder;
import lombok.Data;
import org.ton.ton4j.cell.Cell;
import org.ton.ton4j.cell.CellBuilder;
import org.ton.ton4j.cell.CellType;

/**
 *
 *
 * <pre>{@code
 * depth_balance$_ split_depth:(#<= 30) balance:CurrencyCollection = DepthBalanceInfo;
 * }</pre>
 */
@Builder
@Data
public class DepthBalanceInfoLazy implements Serializable {
  int depth;
  CurrencyCollectionLazy currencies;

  public Cell toCell() {
    return CellBuilder.beginCell().storeUint(depth, 5).storeCell(currencies.toCell()).endCell();
  }

  public static DepthBalanceInfoLazy deserialize(CellSliceLazy cs) {
    if (cs.type == CellType.PRUNED_BRANCH) {
      return null;
    }
    return DepthBalanceInfoLazy.builder()
        .depth(cs.loadUint(5).intValue()) // tlb #<= 60
        .currencies(CurrencyCollectionLazy.deserialize(cs))
        .build();
  }
}
