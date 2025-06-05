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
  public long shard;
  int seqno;
  public byte[] rootHash;
  public byte[] fileHash;
  private byte[] rootHashBase64;
  private byte[] fileHashBase64;

  public String getRootHash() {
    return Utils.bytesToHex(rootHash);
  }

  public String getFileHash() {
    return Utils.bytesToHex(fileHash);
  }

  public String getRootHashBase64() {
    return Utils.bytesToBase64SafeUrl(rootHash);
  }

  public String getFileHashBase64() {
    return Utils.bytesToBase64SafeUrl(fileHash);
  }

  public String getShard() {
    return Long.toHexString(shard);
  }

  public BlockId getBlockId() {
    return BlockId.builder().workchain(workchain).shard(shard).seqno(seqno).build();
  }

  public byte[] serialize() {
    ByteBuffer byteBuffer =
        ByteBuffer.allocate(4 + 8 + 4 + 32 + 32)
            .order(ByteOrder.LITTLE_ENDIAN)
            .putInt(workchain)
            .putLong(shard)
            .putInt(seqno)
            .put(rootHash)
            .put(fileHash);

    return byteBuffer.array();
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
