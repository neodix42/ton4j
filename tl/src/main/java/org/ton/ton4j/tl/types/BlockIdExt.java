package org.ton.ton4j.tl.types;

import java.io.Serializable;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
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
public class BlockIdExt implements Serializable, LiteServerAnswer {
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
    return ByteBuffer.allocate(4 + 8 + 4 + 32 + 32)
        .order(ByteOrder.LITTLE_ENDIAN)
        .putInt(workchain)
        .putLong(shard)
        .putInt((int) seqno)
        .put(rootHash)
        .put(fileHash)
        .array();
  }

  public static BlockIdExt deserialize(ByteBuffer bf) {
    bf.order(ByteOrder.LITTLE_ENDIAN);
    return BlockIdExt.builder()
        .workchain(bf.getInt())
        .shard(bf.getLong())
        .seqno(bf.getInt())
        .rootHash(Utils.read(bf, 32))
        .fileHash(Utils.read(bf, 32))
        .build();
  }

  public static int getSize() {
    return 4 + 8 + 4 + 32 + 32;
  }
}
