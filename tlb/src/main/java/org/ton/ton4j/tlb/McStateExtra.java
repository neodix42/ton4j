package org.ton.ton4j.tlb;

import static java.util.Objects.isNull;

import java.io.Serializable;
import lombok.Builder;
import lombok.Data;
import org.ton.ton4j.cell.Cell;
import org.ton.ton4j.cell.CellBuilder;
import org.ton.ton4j.cell.CellSlice;

/**
 *
 *
 * <pre>{@code
 * masterchain_state_extra#cc26
 *   shard_hashes:ShardHashes
 *   config:ConfigParams
 *   ^[ flags:(## 16) { flags <= 1 }
 *      validator_info:ValidatorInfo
 *      prev_blocks:OldMcBlocksInfo
 *      after_key_block:Bool
 *      last_key_block:(Maybe ExtBlkRef)
 *      block_create_stats:(flags . 0)?BlockCreateStats ]
 *   global_balance:CurrencyCollection
 * = McStateExtra;
 * }</pre>
 */
@Builder
@Data
public class McStateExtra implements Serializable {
  long magic;
  ShardHashes shardHashes;
  ConfigParams configParams;
  McStateExtraInfo info;
  CurrencyCollection globalBalance;

  private String getMagic() {
    return Long.toHexString(magic);
  }

  public Cell toCell() {
    return CellBuilder.beginCell()
        .storeUint(0xcc26, 16)
        .storeCell(shardHashes.toCell())
        .storeCell(configParams.toCell())
        .storeRef(info.toCell())
        .storeCell(globalBalance.toCell())
        .endCell();
  }

  public static McStateExtra deserialize(CellSlice cs) {
    if (isNull(cs)) {
      return null;
    }

    if (cs.isExotic()) {
      return null;
    }
    long magic = cs.loadUint(16).longValue();
    assert (magic == 0xcc26L)
        : "McStateExtra: magic not equal to 0xcc26, found 0x" + Long.toHexString(magic);

    McStateExtra mcStateExtra = McStateExtra.builder().magic(0xcc26L).build();
    mcStateExtra.setShardHashes(ShardHashes.deserialize(cs));

    mcStateExtra.setConfigParams(ConfigParams.deserialize(cs));
    mcStateExtra.setInfo(McStateExtraInfo.deserialize(CellSlice.beginParse(cs.loadRef())));
    mcStateExtra.setGlobalBalance(CurrencyCollection.deserialize(cs));

    return mcStateExtra;
  }
}
