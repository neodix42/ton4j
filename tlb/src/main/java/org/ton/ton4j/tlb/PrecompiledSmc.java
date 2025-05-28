package org.ton.ton4j.tlb;

import lombok.Builder;
import lombok.Data;
import org.ton.ton4j.cell.Cell;
import org.ton.ton4j.cell.CellBuilder;
import org.ton.ton4j.cell.CellSlice;

import java.io.Serializable;
import java.math.BigInteger;

@Builder
@Data
public class PrecompiledSmc implements Serializable {
  int magic;
  BigInteger gasUsage;

  public Cell toCell() {
    return CellBuilder.beginCell().storeUint(0xb0, 8).storeUint(gasUsage, 64).endCell();
  }

  public static PrecompiledSmc deserialize(CellSlice cs) {
    return PrecompiledSmc.builder()
        .magic(cs.loadUint(8).intValue())
        .gasUsage(cs.loadUint(64))
        .build();
  }
}
