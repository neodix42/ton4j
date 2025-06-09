package org.ton.ton4j.tl.liteserver.queries;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import lombok.Builder;
import lombok.Data;
import org.ton.ton4j.tl.liteserver.responses.BlockIdExt;
import org.ton.ton4j.tl.liteserver.responses.LiteServerQueryData;

/**
 * liteServer.getBlockOutMsgQueueSize mode:# id:tonNode.blockIdExt want_proof:mode.0?true =
 * liteServer.BlockOutMsgQueueSize;
 */
@Builder
@Data
public class BlockOutMsgQueueSizeQuery implements LiteServerQueryData {
  public static final int GET_BLOCK_OUT_MSG_QUEUE_SIZE_QUERY = -1888716935; // Placeholder CRC32

  private final int mode;
  private final BlockIdExt id;
  private final boolean wantProof;

  public String getQueryName() {
    return "liteServer.getBlockOutMsgQueueSize mode:# id:tonNode.blockIdExt want_proof:mode.0?true = liteServer.BlockOutMsgQueueSize";
  }

  public byte[] getQueryData() {
    ByteBuffer buffer =
        ByteBuffer.allocate(4 + BlockIdExt.getSize())
            .order(ByteOrder.LITTLE_ENDIAN)
            .putInt(mode)
            .put(id.serialize());

    return ByteBuffer.allocate(buffer.array().length + 4)
        .order(ByteOrder.LITTLE_ENDIAN)
        .putInt(GET_BLOCK_OUT_MSG_QUEUE_SIZE_QUERY)
        .put(buffer.array())
        .array();
  }
}
