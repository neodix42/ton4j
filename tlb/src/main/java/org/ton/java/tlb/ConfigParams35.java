package org.ton.java.tlb;

import java.io.Serializable;
import lombok.Builder;
import lombok.Data;
import org.ton.java.cell.Cell;
import org.ton.java.cell.CellBuilder;
import org.ton.java.cell.CellSlice;

/** _ cur_temp_validators:ValidatorSet = ConfigParam 35; */
@Builder
@Data
public class ConfigParams35 implements Serializable {
  ValidatorSet currTempValidatorSet;

  public Cell toCell() {
    return CellBuilder.beginCell().storeCell(currTempValidatorSet.toCell()).endCell();
  }

  public static ConfigParams35 deserialize(CellSlice cs) {
    return ConfigParams35.builder().currTempValidatorSet(ValidatorSet.deserialize(cs)).build();
  }
}
