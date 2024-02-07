package org.ton.java.tlb.types;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;
import org.ton.java.cell.Cell;
import org.ton.java.cell.CellBuilder;
import org.ton.java.cell.CellSlice;

import java.math.BigInteger;

@ToString
@Builder
@Getter
/**
 * block_id_ext$_
 *   shard_id:ShardIdent
 *   seq_no:uint32
 *   root_hash:bits256
 *   file_hash:bits256 = BlockIdExt;
 */
public class BlockIdExtShardIdent {
    //    int workchain;
//    long shard;
    ShardIdent shardId;
    long seqno;
    public BigInteger rootHash;
    public BigInteger fileHash;

    public String getRootHash() {
        return rootHash.toString(16);
    }

    public String getFileHash() {
        return fileHash.toString(16);
    }

//    public String getShard() {
//        return Long.toHexString(shard);
//    }

    public Cell toCell() {
        return CellBuilder.beginCell()
                .storeCell(shardId.toCell())
//                .storeInt(workchain, 32)
//                .storeUint(shard, 64)
                .storeUint(seqno, 32)
                .storeUint(rootHash, 256)
                .storeUint(fileHash, 256)
                .endCell();
    }

    public static BlockIdExtShardIdent deserialize(CellSlice cs) {
        BlockIdExtShardIdent blockIdExtShardIdent = BlockIdExtShardIdent.builder()
                .shardId(ShardIdent.deserialize(cs))
                .seqno(cs.loadUint(32).longValue())
                .rootHash(cs.loadUint(256))
                .fileHash(cs.loadUint(256))
                .build();
        return blockIdExtShardIdent;
    }
}
