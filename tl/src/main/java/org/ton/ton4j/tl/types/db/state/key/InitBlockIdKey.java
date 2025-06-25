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
 * db.state.key.initBlockId = db.state.Key;
 * </pre>
 */
@Builder
@Data
public class InitBlockIdKey extends Key {

  public static InitBlockIdKey deserialize(ByteBuffer buffer) {
    return InitBlockIdKey.builder().build();
  }

  @Override
  public byte[] serialize() {
    // This key has no fields, so it's just an empty byte array
    return new byte[0];
  }
}
