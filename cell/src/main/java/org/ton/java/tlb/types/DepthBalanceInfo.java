package org.ton.java.tlb.types;

import lombok.Builder;
import lombok.Data;
import org.ton.java.cell.Cell;
import org.ton.java.cell.CellBuilder;
import org.ton.java.cell.CellSlice;
import org.ton.java.utils.Utils;

/**
 *
 *
 * <pre>{@code
 * depth_balance$_ split_depth:(#<= 30) balance:CurrencyCollection = DepthBalanceInfo;
 * }</pre>
 */
@Builder
@Data
public class DepthBalanceInfo {
  int depth;
  CurrencyCollection currencies;

  public Cell toCell() {
    return CellBuilder.beginCell()
        .storeUint(depth, Utils.log2Ceil(depth))
        .storeCell(currencies.toCell())
        .endCell();
  }

  public static DepthBalanceInfo deserialize(CellSlice cs) {
    return DepthBalanceInfo.builder()
        .depth(cs.loadUint(5).intValue()) // tlb #<= 60
        .currencies(CurrencyCollection.deserialize(cs))
        .build();
  }
}
