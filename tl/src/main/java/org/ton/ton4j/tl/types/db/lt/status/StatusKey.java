package org.ton.ton4j.tl.types.db.lt.status;

import java.nio.ByteBuffer;
import lombok.Builder;
import lombok.Data;
import org.ton.ton4j.tl.types.db.lt.Key;

/**
 *
 *
 * <pre>
 * ton_api.tl
 * db.lt.status.key = db.lt.Key;
 * </pre>
 */
@Builder
@Data
public class StatusKey extends Key {

  public static StatusKey deserialize(ByteBuffer buffer) {
    return StatusKey.builder().build();
  }

  @Override
  public byte[] serialize() {
    // This key has no fields, so it's just an empty byte array
    return new byte[0];
  }
}
