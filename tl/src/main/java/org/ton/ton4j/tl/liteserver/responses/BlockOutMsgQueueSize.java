package org.ton.ton4j.tl.liteserver.responses;

import java.io.Serializable;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import lombok.Builder;
import lombok.Data;
import org.ton.ton4j.utils.Utils;

/**
 * liteServer.blockOutMsgQueueSize mode:# id:tonNode.blockIdExt size:long proof:mode.0?bytes =
 * liteServer.BlockOutMsgQueueSize;
 */
@Builder
@Data
public class BlockOutMsgQueueSize implements Serializable, LiteServerAnswer {
  public static final int CONSTRUCTOR_ID = -1966227941;

  private final int mode;
  private final BlockIdExt id;
  private final long size;
  private final byte[] proof;

  public String getProof() {
    if (proof == null) {
      return "";
    }
    return Utils.bytesToHex(proof);
  }

  public static final int constructorId = CONSTRUCTOR_ID;

  public static BlockOutMsgQueueSize deserialize(ByteBuffer buffer) {
    buffer.order(ByteOrder.LITTLE_ENDIAN);
    int mode = buffer.getInt();
    BlockIdExt id = BlockIdExt.deserialize(buffer);
    long size = buffer.getLong();

    byte[] proof = null;
    if ((mode & 1) != 0) {
      proof = Utils.fromBytes(buffer);
    }

    return BlockOutMsgQueueSize.builder().mode(mode).id(id).size(size).proof(proof).build();
  }

  public byte[] serialize() {
    int totalSize = 4 + BlockIdExt.getSize() + 8;

    if ((mode & 1) != 0) {
      totalSize += Utils.toBytes(proof).length;
    }

    ByteBuffer buffer =
        ByteBuffer.allocate(totalSize)
            .order(ByteOrder.LITTLE_ENDIAN)
            .putInt(mode)
            .put(id.serialize())
            .putLong(size);

    if ((mode & 1) != 0) {
      buffer.put(Utils.toBytes(proof));
    }

    return buffer.array();
  }

  public static BlockOutMsgQueueSize deserialize(byte[] byteArray) {
    return deserialize(ByteBuffer.wrap(byteArray));
  }
}
