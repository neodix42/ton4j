package org.ton.ton4j.tl.types.db.root;

import java.nio.ByteBuffer;
import lombok.Builder;
import lombok.Data;

/**
 *
 *
 * <pre>
 * ton_api.tl
 * db.root.key.config = db.root.Key;
 * </pre>
 */
@Builder
@Data
public class ConfigKey extends Key {

  public static ConfigKey deserialize(ByteBuffer buffer) {
    return ConfigKey.builder().build();
  }

  @Override
  public byte[] serialize() {
    // This key has no fields, so it's just an empty byte array
    return new byte[0];
  }
}
