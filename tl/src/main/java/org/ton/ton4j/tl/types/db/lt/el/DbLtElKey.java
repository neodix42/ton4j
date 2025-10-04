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
public class DbLtElKey extends Key {
  int magic;
  int workchain;
  long shard;
  int idx;

  public static DbLtElKey deserialize(ByteBuffer buffer) {
    buffer.order(ByteOrder.LITTLE_ENDIAN);
    int magic = buffer.getInt();
    int workchain = buffer.getInt();
    long shard = buffer.getLong();
    int idx = buffer.getInt();

    return DbLtElKey.builder().magic(magic).workchain(workchain).shard(shard).idx(idx).build();
  }

  @Override
  public byte[] serialize() {
    ByteBuffer buffer = ByteBuffer.allocate(4 + 4 + 8 + 4);
    buffer.order(ByteOrder.LITTLE_ENDIAN);
    buffer.putInt(-1523442974);
    buffer.putInt(workchain);
    buffer.putLong(shard);
    buffer.putInt(idx);
    return buffer.array();
  }
}
