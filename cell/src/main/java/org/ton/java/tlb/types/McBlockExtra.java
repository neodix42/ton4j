package org.ton.java.tlb.types;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.ton.java.cell.*;

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
    //    ShardHashes shardHashes;
    TonHashMapE shardHashes;
    //    ShardFees shardFees;
    TonHashMapAugE shardFees;
    Cell more;
    ConfigParams config;

    private String getMagic() {
        return Long.toHexString(magic);
    }

    public Cell toCell() {
        return CellBuilder.beginCell()
                .storeUint(0xcca5, 32)
                .storeBit(keyBlock)
//                .storeCell(shardHashes.toCell())
//                .storeCell(shardFees.toCell())
                .storeDict(shardHashes.serialize(
                        k -> CellBuilder.beginCell().storeUint((Long) k, 32).bits,
                        v -> CellBuilder.beginCell().storeRef((Cell) v) // todo ShardDescr
                ))
                .storeDict(shardFees.serialize(
                        k -> CellBuilder.beginCell().storeUint((Long) k, 96).bits,
                        v -> CellBuilder.beginCell().storeRef((Cell) v), // todo ShardFeeCreated
                        e -> CellBuilder.beginCell().storeRef((Cell) e), // todo ShardFeeCreated
                        (fk, fv) -> CellBuilder.beginCell().storeUint(0, 1) // todo
                ))
                .storeRef(more)
                .storeCell(keyBlock ? config.toCell() : CellBuilder.beginCell().endCell())
                .endCell();
    }

    public static McBlockExtra deserialize(CellSlice cs) {
        long magic = cs.loadUint(16).longValue();
        assert (magic == 0xcca5L) : "McBlockExtra: magic not equal to 0xcca5, found 0x" + Long.toHexString(magic);

        boolean keyBlock = cs.loadBit();
        return McBlockExtra.builder()
                .magic(0xcca5L)
                .keyBlock(keyBlock)
//                .shardHashes(ShardHashes.deserialize(cs))
//                .shardFees(ShardFees.deserialize(cs))
                .shardHashes(cs.loadDictE(32, k -> k.readInt(32), v -> v))
                .shardFees(cs.loadDictAugE(92,
                        k -> k.readInt(92),
                        v -> v,
                        e -> e))
                .more(cs.loadRef())
                .config(keyBlock ? ConfigParams.deserialize(cs) : null)
                .build();
    }
}
