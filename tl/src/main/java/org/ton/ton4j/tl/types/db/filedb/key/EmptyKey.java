package org.ton.ton4j.tl.types.db.filedb.key;

import java.nio.ByteBuffer;
import lombok.Builder;
import lombok.Data;
import org.ton.ton4j.tl.types.db.filedb.Key;

/**
 *
 *
 * <pre>
 * ton_api.tl
 * db.filedb.key.empty = db.filedb.Key;
 * </pre>
 */
@Builder
@Data
public class EmptyKey extends Key {

  public static EmptyKey deserialize(ByteBuffer buffer) {
    return EmptyKey.builder().build();
  }

  @Override
  public byte[] serialize() {
    // This key has no fields, so it's just an empty byte array
    return new byte[0];
  }
}
