package org.ton.java.tlb;

import lombok.Builder;
import lombok.Data;
import org.ton.java.cell.Cell;
import org.ton.java.cell.CellBuilder;
import org.ton.java.cell.CellSlice;

import java.math.BigInteger;

/**
 *
 *
 * <pre>
 * block_id_ext$_
 *   shard_id:ShardIdent
 *   seq_no:uint32
 *   root_hash:bits256
 *   file_hash:bits256 = BlockIdExt;
 *   </pre>
 */
@Builder
@Data
public class BlockIdExtShardIdent {
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
    return BlockIdExtShardIdent.builder()
        .shardId(ShardIdent.deserialize(cs))
        .seqno(cs.loadUint(32).longValue())
        .rootHash(cs.loadUint(256))
        .fileHash(cs.loadUint(256))
        .build();
  }
}
