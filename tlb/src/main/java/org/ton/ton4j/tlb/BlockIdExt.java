package org.ton.ton4j.tlb;

import java.io.Serializable;
import java.nio.ByteBuffer;
import lombok.Builder;
import lombok.Data;
import org.ton.ton4j.cell.Cell;
import org.ton.ton4j.cell.CellBuilder;
import org.ton.ton4j.cell.CellSlice;
import org.ton.ton4j.utils.Utils;

/**
 *
 *
 * <pre>
 * block_id_ext$_
 * shard_id:ShardIdent
 * seq_no:uint32
 * root_hash:bits256
 * file_hash:bits256 = BlockIdExt;
 * </pre>
 */
@Builder
@Data
public class BlockIdExt implements Serializable {
  int workchain;
  public long shard;
  //    ShardIdent shardId;
  long seqno;
  public byte[] rootHash;
  public byte[] fileHash;

  private String getRootHash() {
    return Utils.bytesToHex(rootHash);
  }

  private String getFileHash() {
    return Utils.bytesToHex(fileHash);
  }

  public String getShard() {
    return Long.toUnsignedString(shard, 16);
  }

  public Cell toCell() {
    return CellBuilder.beginCell()
        //                .storeCell(shardId.toCell())
        .storeInt(workchain, 32)
        .storeUint(shard, 64)
        .storeUint(seqno, 32)
        .storeBytes(rootHash)
        .storeBytes(fileHash)
        .endCell();
  }

  public static BlockIdExt deserialize(CellSlice cs) {
    return BlockIdExt.builder()
        .workchain(cs.loadInt(32).intValue())
        .shard(cs.loadUint(64).longValue())
        //                        .shardId((ShardIdent) cs.loadTlb(ShardIdent.class)) // todo weird
        // - this does not work
        .seqno(cs.loadUint(32).longValue())
        .rootHash(cs.loadBytes(32))
        .fileHash(cs.loadBytes(32))
        .build();
  }

  public static BlockIdExt deserialize(byte[] cs) {
    ByteBuffer bb = ByteBuffer.wrap(cs);
    BlockIdExt blockIdExt =
        BlockIdExt.builder().workchain(bb.getInt()).shard(bb.getLong()).seqno(bb.getInt()).build();
    byte[] temp = new byte[32];
    bb.get(temp);
    blockIdExt.setRootHash(temp);
    temp = new byte[32];
    bb.get(temp);
    blockIdExt.setFileHash(temp);

    return blockIdExt;
  }
}
