package org.ton.java.tlb;

import lombok.Builder;
import lombok.Data;
import org.ton.java.cell.Cell;
import org.ton.java.cell.CellBuilder;
import org.ton.java.cell.CellSlice;

@Builder
@Data
public class ConfigParams32 {
  ValidatorSet prevValidatorSet;

  public Cell toCell() {
    return CellBuilder.beginCell().storeCell(prevValidatorSet.toCell()).endCell();
  }

  public static ConfigParams32 deserialize(CellSlice cs) {
    return ConfigParams32.builder().prevValidatorSet(ValidatorSet.deserialize(cs)).build();
  }
}
