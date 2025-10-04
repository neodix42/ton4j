package org.ton.ton4j.tl.types.db.lt.desc;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import lombok.Builder;
import lombok.Data;
import org.ton.ton4j.tl.types.db.lt.Key;
import org.ton.ton4j.utils.Utils;

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
public class DbLtDescKey extends Key {
  int magic;
  int workchain;
  long shard;

  public static DbLtDescKey deserialize(ByteBuffer buffer) {
    buffer.order(ByteOrder.LITTLE_ENDIAN);
    int magic = buffer.getInt();
    int workchain = buffer.getInt();
    long shard = buffer.getLong();

    return DbLtDescKey.builder().magic(magic).workchain(workchain).shard(shard).build();
  }

  @Override
  public byte[] serialize() {
    ByteBuffer buffer = ByteBuffer.allocate(4 + 4 + 8);
    buffer.order(ByteOrder.LITTLE_ENDIAN);
    buffer.putInt(-236722287);
    buffer.putInt(workchain);
    buffer.putLong(shard);
    return buffer.array();
  }

  public String getKeyHash() {
    return Utils.bytesToHex(Utils.sha256AsArray(serialize())).toUpperCase();
  }
}
