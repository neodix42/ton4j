package org.ton.ton4j.tlb;

import lombok.Builder;
import lombok.Data;
import org.ton.ton4j.cell.Cell;
import org.ton.ton4j.cell.CellBuilder;
import org.ton.ton4j.cell.CellSlice;

import java.io.Serializable;

/**
 *
 *
 * <pre>
 *   catchain_config#c1
 *   mc_catchain_lifetime:uint32
 *   shard_catchain_lifetime:uint32
 *   shard_validators_lifetime:uint32
 *   shard_validators_num:uint32 = CatchainConfig;
 *   _ CatchainConfig = ConfigParam 28;
 * </pre>
 */
@Builder
@Data
public class ConfigParams28 implements Serializable {
  CatchainConfig catchainConfig;

  public Cell toCell() {
    return CellBuilder.beginCell().storeCell(catchainConfig.toCell()).endCell();
  }

  public static ConfigParams28 deserialize(CellSlice cs) {
    return ConfigParams28.builder().catchainConfig(CatchainConfig.deserialize(cs)).build();
  }
}
