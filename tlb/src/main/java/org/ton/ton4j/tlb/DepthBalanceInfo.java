package org.ton.ton4j.tlb;

import java.io.Serializable;
import lombok.Builder;
import lombok.Data;
import org.ton.ton4j.cell.Cell;
import org.ton.ton4j.cell.CellBuilder;
import org.ton.ton4j.cell.CellSlice;
import org.ton.ton4j.utils.Utils;

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
