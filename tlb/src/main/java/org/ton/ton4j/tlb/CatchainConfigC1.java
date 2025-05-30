package org.ton.ton4j.tlb;

import java.io.Serializable;
import lombok.Builder;
import lombok.Data;
import org.ton.ton4j.cell.Cell;
import org.ton.ton4j.cell.CellBuilder;
import org.ton.ton4j.cell.CellSlice;

@Builder
@Data
public class CatchainConfigC1 implements CatchainConfig, Serializable {
  int magic;
  long mcCatchainLifetime;
  long shardCatchainLifetime;
  long shardValidatorsLifetime;
  long shardValidatorsNum;

  public Cell toCell() {
    return CellBuilder.beginCell()
        .storeUint(0xc1, 8)
        .storeUint(mcCatchainLifetime, 32)
        .storeUint(shardCatchainLifetime, 32)
        .storeUint(shardValidatorsLifetime, 32)
        .storeUint(shardValidatorsNum, 32)
        .endCell();
  }

  public static CatchainConfigC1 deserialize(CellSlice cs) {
    return CatchainConfigC1.builder()
        .magic(cs.loadUint(8).intValue())
        .mcCatchainLifetime(cs.loadUint(32).longValue())
        .shardCatchainLifetime(cs.loadUint(32).longValue())
        .shardValidatorsLifetime(cs.loadUint(32).longValue())
        .shardValidatorsNum(cs.loadUint(32).longValue())
        .build();
  }
}
