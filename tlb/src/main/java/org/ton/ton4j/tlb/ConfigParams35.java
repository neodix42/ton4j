package org.ton.ton4j.tlb;

import java.io.Serializable;
import lombok.Builder;
import lombok.Data;
import org.ton.ton4j.cell.Cell;
import org.ton.ton4j.cell.CellBuilder;
import org.ton.ton4j.cell.CellSlice;

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
