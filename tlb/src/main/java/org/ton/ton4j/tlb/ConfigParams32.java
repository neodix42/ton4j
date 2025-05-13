package org.ton.ton4j.tlb;

import lombok.Builder;
import lombok.Data;
import org.ton.ton4j.cell.Cell;
import org.ton.ton4j.cell.CellBuilder;
import org.ton.ton4j.cell.CellSlice;

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
