package org.ton.ton4j.tlb;

import java.io.Serializable;
import java.math.BigInteger;
import lombok.Builder;
import lombok.Data;
import org.ton.ton4j.cell.Cell;
import org.ton.ton4j.cell.CellBuilder;
import org.ton.ton4j.cell.CellSlice;

/** _ gas_used:uint32 vm_steps:uint32 = NewBounceComputePhaseInfo; */
@Builder
@Data
public class NewBounceComputePhaseInfo implements Serializable {
  BigInteger gasUsed;
  BigInteger vmSteps;

  public Cell toCell() {
    return CellBuilder.beginCell().storeUint(gasUsed, 32).storeUint(vmSteps, 32).endCell();
  }

  public static NewBounceComputePhaseInfo deserialize(CellSlice cs) {
    return NewBounceComputePhaseInfo.builder()
        .gasUsed(cs.loadUint(32))
        .vmSteps(cs.loadUint(32))
        .build();
  }
}
