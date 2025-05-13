package org.ton.ton4j.tlb;

import java.io.Serializable;
import lombok.Builder;
import lombok.Data;
import org.ton.ton4j.cell.Cell;
import org.ton.ton4j.cell.CellBuilder;
import org.ton.ton4j.cell.CellSlice;

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
