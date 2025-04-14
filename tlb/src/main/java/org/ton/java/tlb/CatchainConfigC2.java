package org.ton.java.tlb;

import java.io.Serializable;
import lombok.Builder;
import lombok.Data;
import org.ton.java.cell.Cell;
import org.ton.java.cell.CellBuilder;
import org.ton.java.cell.CellSlice;

@Builder
@Data
public class CatchainConfigC2 implements CatchainConfig, Serializable {
  int magic;
  int flags;
  boolean shuffleMcValidators;
  long mcCatchainLifetime;
  long shardCatchainLifetime;
  long shardValidatorsLifetime;
  long shardValidatorsNum;

  public Cell toCell() {
    return CellBuilder.beginCell()
        .storeUint(0xc2, 8)
        .storeUint(flags, 7)
        .storeBit(shuffleMcValidators)
        .storeUint(mcCatchainLifetime, 32)
        .storeUint(shardCatchainLifetime, 32)
        .storeUint(shardValidatorsLifetime, 32)
        .storeUint(shardValidatorsNum, 32)
        .endCell();
  }

  public static CatchainConfigC2 deserialize(CellSlice cs) {
    return CatchainConfigC2.builder()
        .magic(cs.loadUint(8).intValue())
        .flags(cs.loadUint(7).intValue())
        .shuffleMcValidators(cs.loadBit())
        .mcCatchainLifetime(cs.loadUint(32).longValue())
        .shardCatchainLifetime(cs.loadUint(32).longValue())
        .shardValidatorsLifetime(cs.loadUint(32).longValue())
        .shardValidatorsNum(cs.loadUint(32).longValue())
        .build();
  }
}
