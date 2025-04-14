package org.ton.java.tlb;

import lombok.Builder;
import lombok.Data;
import org.ton.java.cell.Cell;
import org.ton.java.cell.CellBuilder;
import org.ton.java.cell.CellSlice;

/**
 *
 *
 * <pre>
 * param_limits#c3 underload:# soft_limit:# { underload <= soft_limit }
 *   hard_limit:# { soft_limit <= hard_limit } = ParamLimits;
 *
 * block_limits#5d bytes:ParamLimits gas:ParamLimits lt_delta:ParamLimits
 *   = BlockLimits;
 *
 * config_mc_block_limits#_ BlockLimits = ConfigParam 22;
 * </pre>
 */
@Builder
@Data
public class ConfigParams22 {
  BlockLimits configMcBlockLimits;

  public Cell toCell() {
    return CellBuilder.beginCell().storeCell(configMcBlockLimits.toCell()).endCell();
  }

  public static ConfigParams22 deserialize(CellSlice cs) {
    return ConfigParams22.builder().configMcBlockLimits(BlockLimits.deserialize(cs)).build();
  }
}
