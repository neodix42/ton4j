package org.ton.java.tlb;

import lombok.Builder;
import lombok.Data;
import org.ton.java.cell.Cell;
import org.ton.java.cell.CellBuilder;
import org.ton.java.cell.CellSlice;
import org.ton.java.cell.TonHashMapAugE;

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
public class McBlockExtra {
  long magic;
  boolean keyBlock;
  ShardHashes shardHashes;
  //    ShardFees shardFees;
  TonHashMapAugE shardFees;
  McBlockExtraInfo info;
  ConfigParams config;

  private String getMagic() {
    return Long.toHexString(magic);
  }

  public Cell toCell() {
    return CellBuilder.beginCell()
        .storeUint(0xcca5, 32)
        .storeBit(keyBlock)
        .storeCell(shardHashes.toCell())
        //                .storeCell(shardFees.toCell())
        .storeDict(
            shardFees.serialize(
                k -> CellBuilder.beginCell().storeUint((Long) k, 96).endCell().getBits(),
                v -> CellBuilder.beginCell().storeCell((Cell) v), // todo ShardFeeCreated
                e -> CellBuilder.beginCell().storeCell((Cell) e), // todo ShardFeeCreated
                (fk, fv) -> CellBuilder.beginCell().storeUint(0, 1) // todo
                ))
        .storeRef(info.toCell())
        .storeCell(keyBlock ? config.toCell() : CellBuilder.beginCell().endCell())
        .endCell();
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
            //                .shardFees(ShardFees.deserialize(cs))
            .shardFees(cs.loadDictAugE(96, k -> k.readInt(96), v -> v, e -> e))
            .build();
    mcBlockExtra.setInfo(McBlockExtraInfo.deserialize(CellSlice.beginParse(cs.loadRef())));
    mcBlockExtra.setConfig(keyBlock ? ConfigParams.deserialize(cs) : null);
    return mcBlockExtra;
  }
}
