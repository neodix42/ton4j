package org.ton.ton4j.tl.types.db.celldb.key;

import java.io.Serializable;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import lombok.Builder;
import lombok.Data;
import org.ton.ton4j.utils.Utils;

/**
 *
 *
 * <pre>
 * ton_api.tl
 * db.celldb.key.value hash:int256 = db.celldb.key.Value;
 * </pre>
 */
@Builder
@Data
public class Value implements Serializable {

  public byte[] hash;  // int256

  // Convenience getter
  public String getHash() {
    return Utils.bytesToHex(hash);
  }

  public static Value deserialize(ByteBuffer buffer) {
    buffer.order(ByteOrder.LITTLE_ENDIAN);
    byte[] hash = Utils.read(buffer, 32);
    
    return Value.builder()
        .hash(hash)
        .build();
  }

  public byte[] serialize() {
    ByteBuffer buffer = ByteBuffer.allocate(32);
    buffer.order(ByteOrder.LITTLE_ENDIAN);
    buffer.put(hash);
    return buffer.array();
  }
}
