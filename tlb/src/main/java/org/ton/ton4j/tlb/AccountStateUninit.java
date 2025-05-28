package org.ton.ton4j.tlb;

import java.io.Serializable;
import lombok.Builder;
import lombok.Data;
import org.ton.ton4j.cell.Cell;
import org.ton.ton4j.cell.CellBuilder;
import org.ton.ton4j.cell.CellSlice;

@Builder
@Data
public class AccountStateUninit implements AccountState, Serializable {
  int magic;

  @Override
  public Cell toCell() {
    return CellBuilder.beginCell().storeUint(0, 2).endCell();
  }

  public static AccountStateUninit deserialize(CellSlice cs) {
    return AccountStateUninit.builder().magic(cs.loadUint(2).intValue()).build();
  }
}
