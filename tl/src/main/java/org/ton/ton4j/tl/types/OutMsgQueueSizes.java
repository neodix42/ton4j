package org.ton.ton4j.tl.types;

import java.io.Serializable;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;
import lombok.Builder;
import lombok.Data;

/**
 * liteServer.outMsgQueueSizes shards:(vector liteServer.outMsgQueueSize)
 * ext_msg_queue_size_limit:int = liteServer.OutMsgQueueSizes;
 */
@Builder
@Data
public class OutMsgQueueSizes implements Serializable, LiteServerAnswer {
  public static final int CONSTRUCTOR_ID = -128955901;

  private final List<OutMsgQueueSize> shards;
  private final int extMsgQueueSizeLimit;

  public static final int constructorId = CONSTRUCTOR_ID;

  public static OutMsgQueueSizes deserialize(ByteBuffer buffer) {
    buffer.order(ByteOrder.LITTLE_ENDIAN);

    int vectorLength = buffer.getInt();
    List<OutMsgQueueSize> shards = new ArrayList<>(vectorLength);
    for (int i = 0; i < vectorLength; i++) {
      shards.add(OutMsgQueueSize.deserialize(buffer));
    }

    int extMsgQueueSizeLimit = buffer.getInt();

    return OutMsgQueueSizes.builder()
        .shards(shards)
        .extMsgQueueSizeLimit(extMsgQueueSizeLimit)
        .build();
  }

  public byte[] serialize() {
    int size = 4; // Vector length
    for (OutMsgQueueSize shard : shards) {
      size += shard.serialize().length;
    }
    size += 4; // extMsgQueueSizeLimit

    ByteBuffer buffer =
        ByteBuffer.allocate(size).order(ByteOrder.LITTLE_ENDIAN).putInt(shards.size());

    for (OutMsgQueueSize shard : shards) {
      buffer.put(shard.serialize());
    }

    buffer.putInt(extMsgQueueSizeLimit);

    return buffer.array();
  }

  public static OutMsgQueueSizes deserialize(byte[] byteArray) {
    return deserialize(ByteBuffer.wrap(byteArray));
  }
}
