package org.ton.ton4j.tl.types.db.lt.desc;

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
 * db.lt.desc.key workchain:int shard:long = db.lt.Key;
 * </pre>
 */
@Builder
@Data
public class DescKey extends Key {

  int workchain;
  long shard;

  public static DescKey deserialize(ByteBuffer buffer) {
    buffer.order(ByteOrder.LITTLE_ENDIAN);
    int workchain = buffer.getInt();
    long shard = buffer.getLong();
    
    return DescKey.builder()
        .workchain(workchain)
        .shard(shard)
        .build();
  }

  @Override
  public byte[] serialize() {
    ByteBuffer buffer = ByteBuffer.allocate(4 + 8);
    buffer.order(ByteOrder.LITTLE_ENDIAN);
    buffer.putInt(workchain);
    buffer.putLong(shard);
    return buffer.array();
  }
}
