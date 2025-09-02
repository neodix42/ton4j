package org.ton.ton4j.exporter.types;

import lombok.Builder;
import lombok.Data;
import org.ton.ton4j.tlb.Block;
import org.ton.ton4j.utils.Utils;

/** Data class representing an exported block object that can be used in streams */
@Data
@Builder
public class ExportedBlock {
  private final String archiveKey;
  private final String blockKey;
  private final byte[] rawData;
  private final Block deserializedBlock;
  private final boolean isDeserialized;

  /**
   * Get the block as a deserialized Block object
   *
   * @return Block object if deserialized, null otherwise
   */
  public Block getBlock() {
    return deserializedBlock;
  }

  /**
   * Get the raw block data as hex string
   *
   * @return hex string representation of the block
   */
  public byte[] getRawDataBytes() {
    return rawData;
  }

  public String getRawData() {
    return Utils.bytesToHex(rawData);
  }

  /**
   * Get workchain from the block
   *
   * @return workchain number
   */
  public int getWorkchain() {
    if (deserializedBlock != null) {
      return deserializedBlock.getBlockInfo().getShard().getWorkchain();
    }
    return -1; // Unknown if not deserialized
  }

  /**
   * Get shard from the block
   *
   * @return shard as hex string
   */
  public String getShard() {
    if (deserializedBlock != null) {
      return deserializedBlock.getBlockInfo().getShard().convertShardIdentToShard().toString(16);
    }
    return null; // Unknown if not deserialized
  }

  /**
   * Get sequence number from the block
   *
   * @return sequence number
   */
  public long getSeqno() {
    if (deserializedBlock != null) {
      return deserializedBlock.getBlockInfo().getSeqno();
    }
    return -1; // Unknown if not deserialized
  }
}
