package org.ton.java.tlb.types;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.ton.java.cell.Cell;
import org.ton.java.cell.CellBuilder;
import org.ton.java.cell.TonHashMapAugE;
import org.ton.java.cell.TonHashMapE;

@Builder
@Getter
@Setter
@ToString
/**
 * masterchain_block_extra#cca5
 *   key_block:(## 1)
 *   shard_hashes:ShardHashes
 *   shard_fees:ShardFees
 *   ^[ prev_blk_signatures:(HashmapE 16 CryptoSignaturePair)
 *      recover_create_msg:(Maybe ^InMsg)
 *      mint_msg:(Maybe ^InMsg) ]
 *   config:key_block?ConfigParams
 * = McBlockExtra;
 */
public class McBlockExtra {
    long magic;
    boolean keyBlock;
    TonHashMapE shardHashes; // _ (HashmapE 32 ^(BinTree ShardDescr)) = ShardHashes;
    TonHashMapAugE shardFees; // _ (HashmapAugE 96 ShardFeeCreated ShardFeeCreated) = ShardFees;
    Cell more;
    ConfigParams config;

    private String getMagic() {
        return Long.toHexString(magic);
    }

    public Cell toCell() {
        return CellBuilder.beginCell()
                .storeUint(0xcca5, 32)
                .storeBit(keyBlock)
                .storeDict(shardHashes.serialize(
                        k -> CellBuilder.beginCell().storeUint((Long) k, 32).bits,
                        v -> CellBuilder.beginCell().storeRef((Cell) v) // todo ShardDescr
                ))
                .storeDict(shardFees.serialize(
                        k -> CellBuilder.beginCell().storeUint((Long) k, 96).bits,
                        v -> CellBuilder.beginCell().storeRef((Cell) v), // todo ShardFeeCreated
                        e -> CellBuilder.beginCell().storeRef((Cell) e) // todo ShardFeeCreated
                ))
                .storeCell(more)
                .storeCell(keyBlock ? config.toCell() : CellBuilder.beginCell().endCell()).endCell();

    }
}
