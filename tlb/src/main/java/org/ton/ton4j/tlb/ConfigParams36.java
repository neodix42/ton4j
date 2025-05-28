package org.ton.ton4j.tlb;

import lombok.Builder;
import lombok.Data;
import org.ton.ton4j.cell.Cell;
import org.ton.ton4j.cell.CellBuilder;
import org.ton.ton4j.cell.CellSlice;

import java.io.Serializable;

/** _ next_validators:ValidatorSet = ConfigParam 36; */
@Builder
@Data
public class ConfigParams36 implements Serializable {
  ValidatorSet nextValidatorSet;

  public Cell toCell() {
    return CellBuilder.beginCell().storeCell(nextValidatorSet.toCell()).endCell();
  }

  public static ConfigParams36 deserialize(CellSlice cs) {
    return ConfigParams36.builder().nextValidatorSet(ValidatorSet.deserialize(cs)).build();
  }
}
