package org.ton.ton4j.tlb;

import java.io.Serializable;
import java.math.BigInteger;
import lombok.Builder;
import lombok.Data;
import org.ton.ton4j.cell.Cell;
import org.ton.ton4j.cell.CellBuilder;
import org.ton.ton4j.cell.CellSlice;

@Builder
@Data
public class AccountStateFrozen implements AccountState, Serializable {
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
