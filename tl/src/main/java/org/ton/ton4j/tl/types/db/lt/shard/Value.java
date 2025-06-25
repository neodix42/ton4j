package org.ton.ton4j.tl.types.db.lt.shard;

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
 * db.lt.shard.value workchain:int shard:long = db.lt.shard.Value;
 * </pre>
 */
@Builder
@Data
public class Value implements Serializable {

  int workchain;
  long shard;

  public static Value deserialize(ByteBuffer buffer) {
    buffer.order(ByteOrder.LITTLE_ENDIAN);
    int workchain = buffer.getInt();
    long shard = buffer.getLong();
    
    return Value.builder()
        .workchain(workchain)
        .shard(shard)
        .build();
  }

  public byte[] serialize() {
    ByteBuffer buffer = ByteBuffer.allocate(4 + 8);
    buffer.order(ByteOrder.LITTLE_ENDIAN);
    buffer.putInt(workchain);
    buffer.putLong(shard);
    return buffer.array();
  }
}
