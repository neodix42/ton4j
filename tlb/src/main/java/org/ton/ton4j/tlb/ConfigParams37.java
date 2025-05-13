package org.ton.ton4j.tlb;

import lombok.Builder;
import lombok.Data;
import org.ton.ton4j.cell.Cell;
import org.ton.ton4j.cell.CellBuilder;
import org.ton.ton4j.cell.CellSlice;

import java.io.Serializable;

/** _ next_temp_validators:ValidatorSet = ConfigParam 37; */
@Builder
@Data
public class ConfigParams37 implements Serializable {
  ValidatorSet nextTempValidatorSet;

  public Cell toCell() {
    return CellBuilder.beginCell().storeCell(nextTempValidatorSet.toCell()).endCell();
  }

  public static ConfigParams37 deserialize(CellSlice cs) {
    return ConfigParams37.builder().nextTempValidatorSet(ValidatorSet.deserialize(cs)).build();
  }
}
