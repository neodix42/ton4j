package org.ton.ton4j.tl.types.db.block;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import lombok.Builder;
import lombok.Data;
import org.ton.ton4j.utils.Utils;

/**
 * Extended block identifier for TON blockchain. Based on the TL schema definition for
 * tonNode.blockIdExt.
 */
@Builder
@Data
public class BlockIdExt {
  public static final int SERIALIZED_SIZE = 4 + 4 + 8 + 32; // workchain + shard + seqno + rootHash

  private int workchain;
  private long shard;
  private long seqno;
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
    blockIdExt.seqno = buffer.getLong();

    blockIdExt.rootHash = new byte[32];
    buffer.get(blockIdExt.rootHash);

    blockIdExt.fileHash = new byte[32];
    buffer.get(blockIdExt.fileHash);

    return blockIdExt;
  }

  /**
   * Serializes this BlockIdExt to a ByteBuffer.
   *
   * @param buffer The ByteBuffer to write to
   */
  public byte[] serialize(ByteBuffer buffer) {
    buffer.order(ByteOrder.LITTLE_ENDIAN);

    buffer.putInt(workchain);
    buffer.putLong(shard);
    buffer.putLong(seqno);
    buffer.put(rootHash);
    buffer.put(fileHash);
    return buffer.array();
  }

  @Override
  public String toString() {
    return "BlockIdExt{"
        + "workchain="
        + workchain
        + ", shard="
        + shard
        + ", seqno="
        + seqno
        + ", rootHash="
        + bytesToHex(rootHash)
        + ", fileHash="
        + bytesToHex(fileHash)
        + '}';
  }

  /**
   * Converts a byte array to a hexadecimal string.
   *
   * @param bytes The byte array to convert
   * @return The hexadecimal string
   */
  private static String bytesToHex(byte[] bytes) {
    StringBuilder hex = new StringBuilder(2 * bytes.length);
    for (byte b : bytes) {
      hex.append(Character.forDigit((b >> 4) & 0xF, 16)).append(Character.forDigit((b & 0xF), 16));
    }
    return hex.toString().toLowerCase();
  }
}
