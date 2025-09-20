package org.ton.ton4j.tlb;

import java.io.Serializable;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.ton.ton4j.cell.Cell;
import org.ton.ton4j.cell.CellBuilder;
import org.ton.ton4j.cell.CellSlice;

/**
 *
 *
 * <pre>
 * masterchain_block_extra#cca5
 *   key_block:(## 1)
 *   shard_hashes:ShardHashes // _ (HashmapE 32 ^(BinTree ShardDescr)) = ShardHashes;
 *   shard_fees:ShardFees //     _ (HashmapAugE 96 ShardFeeCreated ShardFeeCreated) = ShardFees;
 *   ^[
 *     prev_blk_signatures:(HashmapE 16 CryptoSignaturePair)
 *     recover_create_msg:(Maybe ^InMsg)
 *     mint_msg:(Maybe ^InMsg)
 *    ]
 *   config:key_block?ConfigParams
 * = McBlockExtra;
 * </pre>
 */
@Builder
@Data
@Slf4j
public class McBlockExtra implements Serializable {
  long magic;
  boolean keyBlock;
  ShardHashes shardHashes;
  ShardFees shardFees;
  McBlockExtraInfo info;
  ConfigParams config;

  private String getMagic() {
    return Long.toHexString(magic);
  }

  public Cell toCell() {
    CellBuilder cell =
        CellBuilder.beginCell()
            .storeUint(0xcca5, 32)
            .storeBit(keyBlock)
            .storeCell(shardHashes.toCell())
            .storeCell(shardFees.toCell())
            .storeRef(info.toCell());
    if (keyBlock) {
      cell.storeCell(config.toCell());
    }
    return cell.endCell();
  }

  public static McBlockExtra deserialize(CellSlice cs) {
    long magic = cs.loadUint(16).longValue();
    assert (magic == 0xcca5L)
        : "McBlockExtra: magic not equal to 0xcca5, found 0x" + Long.toHexString(magic);

    boolean keyBlock = cs.loadBit();
    McBlockExtra mcBlockExtra =
        McBlockExtra.builder()
            .magic(0xcca5L)
            .keyBlock(keyBlock)
            .shardHashes(ShardHashes.deserialize(cs))
            .shardFees(ShardFees.deserialize(cs))
            .build();
    mcBlockExtra.setInfo(McBlockExtraInfo.deserialize(CellSlice.beginParse(cs.loadRef())));
    mcBlockExtra.setConfig(keyBlock ? ConfigParams.deserialize(cs) : null);

    return mcBlockExtra;
  }
}
