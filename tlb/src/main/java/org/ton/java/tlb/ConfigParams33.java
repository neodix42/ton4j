package org.ton.java.tlb;

import java.io.Serializable;
import lombok.Builder;
import lombok.Data;
import org.ton.java.cell.Cell;
import org.ton.java.cell.CellBuilder;
import org.ton.java.cell.CellSlice;

/** _ prev_temp_validators:ValidatorSet = ConfigParam 33; */
@Builder
@Data
public class ConfigParams33 implements Serializable {
  ValidatorSet prevTempValidatorSet;

  public Cell toCell() {
    return CellBuilder.beginCell().storeCell(prevTempValidatorSet.toCell()).endCell();
  }

  public static ConfigParams33 deserialize(CellSlice cs) {
    return ConfigParams33.builder().prevTempValidatorSet(ValidatorSet.deserialize(cs)).build();
  }
}
