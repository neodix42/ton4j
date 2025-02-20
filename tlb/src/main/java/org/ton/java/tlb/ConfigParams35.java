package org.ton.java.tlb;

import lombok.Builder;
import lombok.Data;
import org.ton.java.cell.Cell;
import org.ton.java.cell.CellBuilder;
import org.ton.java.cell.CellSlice;

@Builder
@Data
public class ConfigParams35 {
  ValidatorSet currTempValidatorSet;

  public Cell toCell() {
    return CellBuilder.beginCell().storeCell(currTempValidatorSet.toCell()).endCell();
  }

  public static ConfigParams35 deserialize(CellSlice cs) {
    return ConfigParams35.builder().currTempValidatorSet(ValidatorSet.deserialize(cs)).build();
  }
}
