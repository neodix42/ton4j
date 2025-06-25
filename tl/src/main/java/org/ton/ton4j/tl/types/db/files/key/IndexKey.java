package org.ton.ton4j.tl.types.db.files.key;

import java.nio.ByteBuffer;
import lombok.Builder;
import lombok.Data;
import org.ton.ton4j.tl.types.db.files.Key;

/**
 *
 *
 * <pre>
 * ton_api.tl
 * db.files.index.key = db.files.Key;
 * </pre>
 */
@Builder
@Data
public class IndexKey extends Key {

  public static IndexKey deserialize(ByteBuffer buffer) {
    return IndexKey.builder().build();
  }

  @Override
  public byte[] serialize() {
    // This key has no fields, so it's just an empty byte array
    return new byte[0];
  }
}
