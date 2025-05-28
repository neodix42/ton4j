package org.ton.ton4j.tlb;

import java.io.Serializable;
import lombok.Builder;
import lombok.Data;
import org.ton.ton4j.cell.Cell;
import org.ton.ton4j.cell.CellBuilder;
import org.ton.ton4j.cell.CellSlice;

/**
 *
 *
 * <pre>
 *   validators_elected_for:uint32
 *   elections_start_before:uint32
 *   elections_end_before:uint32
 *   stake_held_for:uint32 = ConfigParam 15;
 *   </pre>
 */
@Builder
@Data
public class ConfigParams15 implements Serializable {
  long validatorsElectedFor;
  long electionsStartBefore;
  long electionsEndBefore;
  long stakeHeldFor;

  public Cell toCell() {
    return CellBuilder.beginCell()
        .storeUint(validatorsElectedFor, 32)
        .storeUint(electionsStartBefore, 32)
        .storeUint(electionsEndBefore, 32)
        .storeUint(stakeHeldFor, 32)
        .endCell();
  }

  public static ConfigParams15 deserialize(CellSlice cs) {
    return ConfigParams15.builder()
        .validatorsElectedFor(cs.loadUint(32).longValue())
        .electionsStartBefore(cs.loadUint(32).longValue())
        .electionsEndBefore(cs.loadUint(32).longValue())
        .stakeHeldFor(cs.loadUint(32).longValue())
        .build();
  }
}
