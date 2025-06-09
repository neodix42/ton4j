package org.ton.ton4j.tl.liteserver.responses;

import java.io.Serializable;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import lombok.Builder;
import lombok.Data;

/** liteServer.outMsgQueueSize id:tonNode.blockIdExt size:int = liteServer.OutMsgQueueSize; */
@Builder
@Data
public class OutMsgQueueSize implements Serializable {

  private final BlockIdExt id;
  private final int size;

  public static OutMsgQueueSize deserialize(ByteBuffer buffer) {
    buffer.order(ByteOrder.LITTLE_ENDIAN);
    return OutMsgQueueSize.builder()
        .id(BlockIdExt.deserialize(buffer))
        .size(buffer.getInt())
        .build();
  }

  public byte[] serialize() {
    ByteBuffer buffer =
        ByteBuffer.allocate(BlockIdExt.getSize() + 4)
            .order(ByteOrder.LITTLE_ENDIAN)
            .put(id.serialize())
            .putInt(size);

    return buffer.array();
  }

  public static OutMsgQueueSize deserialize(byte[] byteArray) {
    return deserialize(ByteBuffer.wrap(byteArray));
  }
}
