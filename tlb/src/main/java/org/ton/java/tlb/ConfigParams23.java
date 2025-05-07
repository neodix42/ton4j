package org.ton.java.tlb;

import java.io.Serializable;
import lombok.Builder;
import lombok.Data;
import org.ton.java.cell.Cell;
import org.ton.java.cell.CellBuilder;
import org.ton.java.cell.CellSlice;

/**
 *
 *
 * <pre>
 * param_limits#c3 underload:# soft_limit:# { underload &lt;= soft_limit }
 *   hard_limit:# { soft_limit &lt;= hard_limit } = ParamLimits;
 *
 * block_limits#5d bytes:ParamLimits gas:ParamLimits lt_delta:ParamLimits
 *   = BlockLimits;
 *
 * config_block_limits#_ BlockLimits = ConfigParam 23;
 * </pre>
 */
@Builder
@Data
public class ConfigParams23 implements Serializable {
  BlockLimits configBlockLimits;

  public Cell toCell() {
    return CellBuilder.beginCell().storeCell(configBlockLimits.toCell()).endCell();
  }

  public static ConfigParams23 deserialize(CellSlice cs) {
    return ConfigParams23.builder().configBlockLimits(BlockLimits.deserialize(cs)).build();
  }
}
