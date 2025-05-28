package org.ton.ton4j.tlb;

import lombok.Builder;
import lombok.Data;
import org.ton.ton4j.cell.Cell;
import org.ton.ton4j.cell.CellBuilder;
import org.ton.ton4j.cell.CellSlice;

@Builder
@Data
public class AccountStateActive implements AccountState {
  int magic;
  StateInit stateInit;

  @Override
  public Cell toCell() {
    return CellBuilder.beginCell().storeUint(1, 1).storeCell(stateInit.toCell()).endCell();
  }

  public static AccountStateActive deserialize(CellSlice cs) {
    return AccountStateActive.builder()
        .magic(cs.loadUint(1).intValue())
        .stateInit(StateInit.deserialize(cs))
        .build();
  }
}
