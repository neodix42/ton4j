package org.ton.ton4j.tl.types;

import java.nio.ByteBuffer;
import lombok.Builder;
import lombok.Data;
import org.ton.ton4j.utils.Utils;

/**
 *
 *
 * <pre>
 * ton_api.tl
 * tonNode.blockIdExt
 *  workchain:int
 *  shard:long
 *  seqno:int
 *  root_hash:int256
 *  file_hash:int256 = tonNode.BlockIdExt;
 * </pre>
 */
@Builder
@Data
public class BlockIdExt {
  int workchain;
  long shard;
  long seqno;
  byte[] rootHash;
  byte[] fileHash;

  private String getRootHash() {
    return Utils.bytesToHex(rootHash);
  }

  private String getFileHash() {
    return Utils.bytesToHex(fileHash);
  }

  public String getShard() {
    return Long.toHexString(shard);
  }

  public byte[] serialize() {
    return ByteBuffer.allocate((32 + 64 + 32 + 256 + 256) / 8)
        .putInt(workchain) // 32
        .putLong(shard) // 64
        .putInt((int) (seqno)) // 32
        .put(rootHash)
        .put(fileHash)
        .array();
  }

  public static BlockIdExt deserialize(ByteBuffer bf) {
    BlockIdExt blockIdExt =
        BlockIdExt.builder()
            .workchain(bf.getInt())
            .shard(bf.getLong())
            .seqno(bf.getInt() & 0xFFFFFFFFL) // todo wrong
            .rootHash(Utils.read(bf, 32))
            .fileHash(Utils.read(bf, 32))
            .build();
    return blockIdExt;
  }

  public static int getSize() {
    return (32 + 64 + 32 + 256 + 256) / 8;
  }
}
