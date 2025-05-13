package org.ton.ton4j.tlb;

import lombok.Builder;
import lombok.Data;
import org.ton.ton4j.cell.Cell;
import org.ton.ton4j.cell.CellBuilder;
import org.ton.ton4j.cell.CellSlice;

import java.io.Serializable;

/** _ cur_validators:ValidatorSet = ConfigParam 34; */
@Builder
@Data
public class ConfigParams34 implements Serializable {
  ValidatorSet currValidatorSet;

  public Cell toCell() {
    return CellBuilder.beginCell().storeCell(currValidatorSet.toCell()).endCell();
  }

  public static ConfigParams34 deserialize(CellSlice cs) {
    return ConfigParams34.builder().currValidatorSet(ValidatorSet.deserialize(cs)).build();
  }
}
