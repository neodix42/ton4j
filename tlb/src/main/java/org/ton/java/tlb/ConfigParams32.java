package org.ton.java.tlb;

import lombok.Builder;
import lombok.Data;
import org.ton.java.cell.Cell;
import org.ton.java.cell.CellBuilder;
import org.ton.java.cell.CellSlice;

import java.io.Serializable;

/** _ prev_validators:ValidatorSet = ConfigParam 32; */
@Builder
@Data
public class ConfigParams32 implements Serializable {
  ValidatorSet prevValidatorSet;

  public Cell toCell() {
    return CellBuilder.beginCell().storeCell(prevValidatorSet.toCell()).endCell();
  }

  public static ConfigParams32 deserialize(CellSlice cs) {
    return ConfigParams32.builder().prevValidatorSet(ValidatorSet.deserialize(cs)).build();
  }
}
