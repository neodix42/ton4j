package org.ton.ton4j.tlb;

import java.io.Serializable;
import lombok.Builder;
import lombok.Data;
import org.ton.ton4j.cell.Cell;
import org.ton.ton4j.cell.CellBuilder;
import org.ton.ton4j.cell.CellSlice;
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
public class DepthBalanceInfo implements Serializable {
  int depth;
  CurrencyCollection currencies;

  public Cell toCell() {
    return CellBuilder.beginCell().storeUint(depth, 5).storeCell(currencies.toCell()).endCell();
  }

  public static DepthBalanceInfo deserialize(CellSlice cs) {
    if (cs.type == CellType.PRUNED_BRANCH) {
      return null;
    }
    return DepthBalanceInfo.builder()
        .depth(cs.loadUint(5).intValue()) // tlb #<= 60
        .currencies(CurrencyCollection.deserialize(cs))
        .build();
  }
}
