package org.ton.java.tlb.types;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.ton.java.cell.Cell;
import org.ton.java.cell.CellBuilder;
import org.ton.java.cell.CellSlice;
import org.ton.java.cell.CellType;

import java.math.BigInteger;

@Builder
@Getter
@Setter
@ToString
/**
 * !merkle_update#02 {X:Type} old_hash:bits256 new_hash:bits256 old:^X new:^X  = MERKLE_UPDATE X;
 *  update_hashes#72 {X:Type} old_hash:bits256 new_hash:bits256                = HASH_UPDATE X;
 * !merkle_proof#03 {X:Type} virtual_hash:bits256 depth:uint16 virtual_root:^X = MERKLE_PROOF X;
 */
public class MerkleUpdate {
    //    ShardState oldOne;
    BigInteger oldHash;
    BigInteger newHash;
    ShardState oldShardState;
    ShardState newShardState;

    public Cell toCell() {
        return CellBuilder.beginCell()
                .storeUint(oldHash, 256)
                .storeUint(newHash, 256)
                .storeRef(oldShardState.toCell())
                .storeRef(newShardState.toCell())
                .endCell();
    }

    public static MerkleUpdate deserialize(CellSlice cs) {

        if (cs.type != CellType.MERKLE_UPDATE) {
            return null;
        }

        long magic = cs.loadUint(8).intValue();
//        assert (magic == 0x02) : "MerkleUpdate: magic not equal to 0x02, found 0x" + Long.toHexString(magic);

        return MerkleUpdate.builder()
                .oldHash(cs.loadUint(256))
                .newHash(cs.loadUint(256))
                .oldShardState(ShardState.deserialize(CellSlice.beginParse(cs.loadRef())))
                .newShardState(ShardState.deserialize(CellSlice.beginParse(cs.loadRef())))
                .build();
    }

    private String getOldHash() {
        return oldHash.toString(16);
    }

    private String getNewHash() {
        return newHash.toString(16);
    }
}
