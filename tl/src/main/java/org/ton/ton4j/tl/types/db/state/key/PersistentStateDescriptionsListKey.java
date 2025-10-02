package org.ton.ton4j.tl.types.db.state.key;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import lombok.Builder;
import lombok.Data;
import org.ton.ton4j.tl.types.db.state.Key;

/**
 *
 *
 * <pre>
 * ton_api.tl
 * db.state.key.persistentStateDescriptionsList = db.state.Key;
 * </pre>
 */
@Builder
@Data
public class PersistentStateDescriptionsListKey extends Key {
  long magic;

  public static PersistentStateDescriptionsListKey deserialize(ByteBuffer buffer) {
    buffer.order(ByteOrder.LITTLE_ENDIAN);
    int magic = buffer.getInt();

    return PersistentStateDescriptionsListKey.builder().magic(magic).build();
  }

  @Override
  public byte[] serialize() {
    ByteBuffer buffer = ByteBuffer.allocate(4);
    buffer.order(ByteOrder.LITTLE_ENDIAN);
    buffer.putInt(1511487946);
    return buffer.array();
  }
}
