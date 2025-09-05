package org.ton.ton4j.tl.types.db.block;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import lombok.Builder;
import lombok.Data;
import org.apache.commons.lang3.tuple.Pair;
import org.ton.ton4j.cell.Cell;
import org.ton.ton4j.tlb.Block;
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
        + Utils.bytesToHex(rootHash)
        + ", fileHash="
        + Utils.bytesToHex(fileHash)
        + '}';
  }

  public static BlockIdExt fromBlock(Pair<Cell, Block> cellBlock) {
    Block block = cellBlock.getRight();
    Cell cell = cellBlock.getLeft();
    return BlockIdExt.builder()
        .seqno(block.getBlockInfo().getSeqno())
        .shard(block.getBlockInfo().getShard().convertShardIdentToShard().longValue())
        .workchain(block.getBlockInfo().getShard().getWorkchain())
        .rootHash(block.toCell().getHash()) // bug in block serialization?
        .fileHash(cell.getHash())
        .build();
  }
}
