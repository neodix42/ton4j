package org.ton.ton4j.tl.types.db.block;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import lombok.Builder;
import lombok.Data;
import org.ton.ton4j.utils.Utils;

/**
 * tonNode.blockIdExt workchain:int shard:long seqno:int root_hash:int256 file_hash:int256 =
 * tonNode.BlockIdExt;
 */
@Builder
@Data
public class BlockIdExt {
  public static final int SERIALIZED_SIZE =
      4 + 8 + 4 + 32 + 32; //  workchain + shard + seqno + rootHash + fileHash = 80
  private int workchain;
  private long shard;
  private int seqno;
  public byte[] rootHash;
  public byte[] fileHash;

  public String getRootHash() {
    return Utils.bytesToHex(rootHash);
  }

  public String getFileHash() {
    return Utils.bytesToHex(fileHash);
  }

  /**
   * Deserializes a BlockIdExt from a ByteBuffer.
   *
   * @param buffer The ByteBuffer containing the serialized BlockIdExt
   * @return The deserialized BlockIdExt
   */
  public static BlockIdExt deserialize(ByteBuffer buffer) {
    buffer.order(ByteOrder.LITTLE_ENDIAN);

    BlockIdExt blockIdExt = BlockIdExt.builder().build();
    blockIdExt.workchain = buffer.getInt();
    blockIdExt.shard = buffer.getLong();
    blockIdExt.seqno = buffer.getInt();

    blockIdExt.rootHash = new byte[32];
    buffer.get(blockIdExt.rootHash);

    blockIdExt.fileHash = new byte[32];
    buffer.get(blockIdExt.fileHash);

    return blockIdExt;
  }

  /** Serializes this BlockIdExt to a ByteBuffer. */
  public byte[] serialize() {
    ByteBuffer buffer = ByteBuffer.allocate(SERIALIZED_SIZE);
    buffer.order(ByteOrder.LITTLE_ENDIAN);
    buffer.putInt(workchain);
    buffer.putLong(shard);
    buffer.putInt(seqno);
    buffer.put(rootHash);
    buffer.put(fileHash);
    return buffer.array();
  }

  public String toFilename() {
    return String.format(
        "(%d,%x,%d):%s:%s",
        workchain, shard, seqno, getRootHash().toUpperCase(), getFileHash().toUpperCase());
  }
}
