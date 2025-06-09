package org.ton.ton4j.tl.liteserver.queries;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import lombok.Builder;
import lombok.Data;
import org.ton.ton4j.tl.liteserver.responses.LiteServerQueryData;

/**
 * liteServer.getOutMsgQueueSizes mode:# wc:mode.0?int shard:mode.0?long =
 * liteServer.OutMsgQueueSizes;
 */
@Builder
@Data
public class OutMsgQueueSizesQuery implements LiteServerQueryData {
  public static final int GET_OUT_MSG_QUEUE_SIZES_QUERY = 2076286006; // Placeholder CRC32

  private final int mode;
  private final int wc;
  private final long shard;

  public String getQueryName() {
    return "liteServer.getOutMsgQueueSizes mode:# wc:mode.0?int shard:mode.0?long = liteServer.OutMsgQueueSizes";
  }

  public byte[] getQueryData() {
    int size = 4; // Mode size
    if ((mode & 1) != 0) {
      size += 4 + 8; // wc (int) + shard (long)
    }

    ByteBuffer buffer = ByteBuffer.allocate(size).order(ByteOrder.LITTLE_ENDIAN).putInt(mode);

    if ((mode & 1) != 0) {
      buffer.putInt(wc).putLong(shard);
    }

    return ByteBuffer.allocate(buffer.array().length + 4)
        .order(ByteOrder.LITTLE_ENDIAN)
        .putInt(GET_OUT_MSG_QUEUE_SIZES_QUERY)
        .put(buffer.array())
        .array();
  }
}
