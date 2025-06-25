package org.ton.ton4j.tl.types.db.lt.status;

import java.io.Serializable;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import lombok.Builder;
import lombok.Data;

/**
 *
 *
 * <pre>
 * ton_api.tl
 * db.lt.status.value total_shards:int = db.lt.status.Value;
 * </pre>
 */
@Builder
@Data
public class Value implements Serializable {

  int totalShards;

  public static Value deserialize(ByteBuffer buffer) {
    buffer.order(ByteOrder.LITTLE_ENDIAN);
    int totalShards = buffer.getInt();
    
    return Value.builder()
        .totalShards(totalShards)
        .build();
  }

  public byte[] serialize() {
    ByteBuffer buffer = ByteBuffer.allocate(4);
    buffer.order(ByteOrder.LITTLE_ENDIAN);
    buffer.putInt(totalShards);
    return buffer.array();
  }
}
