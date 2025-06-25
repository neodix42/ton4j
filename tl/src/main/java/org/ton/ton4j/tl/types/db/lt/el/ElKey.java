package org.ton.ton4j.tl.types.db.lt.el;

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
 * db.lt.el.key workchain:int shard:long idx:int = db.lt.Key;
 * </pre>
 */
@Builder
@Data
public class ElKey extends Key {

  int workchain;
  long shard;
  int idx;

  public static ElKey deserialize(ByteBuffer buffer) {
    buffer.order(ByteOrder.LITTLE_ENDIAN);
    int workchain = buffer.getInt();
    long shard = buffer.getLong();
    int idx = buffer.getInt();
    
    return ElKey.builder()
        .workchain(workchain)
        .shard(shard)
        .idx(idx)
        .build();
  }

  @Override
  public byte[] serialize() {
    ByteBuffer buffer = ByteBuffer.allocate(4 + 8 + 4);
    buffer.order(ByteOrder.LITTLE_ENDIAN);
    buffer.putInt(workchain);
    buffer.putLong(shard);
    buffer.putInt(idx);
    return buffer.array();
  }
}
