package org.ton.ton4j.tlb;

import lombok.Builder;
import lombok.Data;
import org.ton.ton4j.cell.Cell;
import org.ton.ton4j.cell.CellBuilder;
import org.ton.ton4j.cell.CellSlice;

/**
 *
 *
 * <pre>
 * _ fees:CurrencyCollection create:CurrencyCollection = ShardFeeCreated;
 * </pre>
 */
@Builder
@Data
public class ShardFeeCreated {

  CurrencyCollection fees;
  CurrencyCollection create;

  public Cell toCell() {
    return CellBuilder.beginCell().storeCell(fees.toCell()).storeCell(create.toCell()).endCell();
  }

  public static ShardFeeCreated deserialize(CellSlice cs) {
    return ShardFeeCreated.builder()
        .fees(CurrencyCollection.deserialize(cs))
        .create(CurrencyCollection.deserialize(cs))
        .build();
  }
}
