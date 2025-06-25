package org.ton.ton4j.tl.types.db.state.key;

import java.nio.ByteBuffer;
import lombok.Builder;
import lombok.Data;
import org.ton.ton4j.tl.types.db.state.Key;

/**
 *
 *
 * <pre>
 * ton_api.tl
 * db.state.key.gcBlockId = db.state.Key;
 * </pre>
 */
@Builder
@Data
public class GcBlockIdKey extends Key {

  public static GcBlockIdKey deserialize(ByteBuffer buffer) {
    return GcBlockIdKey.builder().build();
  }

  @Override
  public byte[] serialize() {
    // This key has no fields, so it's just an empty byte array
    return new byte[0];
  }
}
