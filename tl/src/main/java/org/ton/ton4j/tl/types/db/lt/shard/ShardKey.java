package org.ton.ton4j.tl.types.db.lt.shard;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import lombok.Builder;
import lombok.Data;
import org.ton.ton4j.tl.types.db.lt.Key;

/**
 *
 *
 * <pre>
 * ton_api.tl
 * db.lt.shard.key idx:int = db.lt.Key;
 * </pre>
 */
@Builder
@Data
public class ShardKey extends Key {

  int idx;

  public static ShardKey deserialize(ByteBuffer buffer) {
    buffer.order(ByteOrder.LITTLE_ENDIAN);
    int idx = buffer.getInt();
    
    return ShardKey.builder()
        .idx(idx)
        .build();
  }

  @Override
  public byte[] serialize() {
    ByteBuffer buffer = ByteBuffer.allocate(4);
    buffer.order(ByteOrder.LITTLE_ENDIAN);
    buffer.putInt(idx);
    return buffer.array();
  }
}
