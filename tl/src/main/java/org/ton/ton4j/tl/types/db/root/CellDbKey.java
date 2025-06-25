package org.ton.ton4j.tl.types.db.root;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import lombok.Builder;
import lombok.Data;

/**
 *
 *
 * <pre>
 * ton_api.tl
 * db.root.key.cellDb version:int = db.root.Key;
 * </pre>
 */
@Builder
@Data
public class CellDbKey extends Key {

  int version;

  public static CellDbKey deserialize(ByteBuffer buffer) {
    buffer.order(ByteOrder.LITTLE_ENDIAN);
    int version = buffer.getInt();
    
    return CellDbKey.builder()
        .version(version)
        .build();
  }

  @Override
  public byte[] serialize() {
    ByteBuffer buffer = ByteBuffer.allocate(4);
    buffer.order(ByteOrder.LITTLE_ENDIAN);
    buffer.putInt(version);
    return buffer.array();
  }
}
