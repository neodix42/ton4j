package org.ton.java.tlb;

import java.math.BigInteger;
import lombok.Builder;
import lombok.Data;
import org.ton.java.cell.Cell;
import org.ton.java.cell.CellBuilder;
import org.ton.java.cell.CellSlice;

@Builder
@Data
public class AccountStateFrozen implements AccountState {
  int magic;
  BigInteger stateHash;

  @Override
  public Cell toCell() {
    return CellBuilder.beginCell().storeUint(0b01, 2).storeUint(stateHash, 256).endCell();
  }

  public static AccountStateFrozen deserialize(CellSlice cs) {
    return AccountStateFrozen.builder()
        .magic(cs.loadUint(2).intValue())
        .stateHash(cs.getRestBits() >= 256 ? cs.loadUint(256) : null)
        .build();
  }
}
