package org.ton.ton4j.tl.types.db.files.key;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import lombok.Builder;
import lombok.Data;
import org.ton.ton4j.tl.types.db.files.Key;

/**
 *
 *
 * <pre>
 * ton_api.tl
 * db.files.package.key package_id:int key:Bool temp:Bool = db.files.Key;
 * </pre>
 */
@Builder
@Data
public class PackageKey extends Key {

  int packageId;
  boolean key;
  boolean temp;

  public static PackageKey deserialize(ByteBuffer buffer) {
    buffer.order(ByteOrder.LITTLE_ENDIAN);
    int packageId = buffer.getInt();
    boolean key = buffer.get() != 0;
    boolean temp = buffer.get() != 0;
    
    return PackageKey.builder()
        .packageId(packageId)
        .key(key)
        .temp(temp)
        .build();
  }

  @Override
  public byte[] serialize() {
    ByteBuffer buffer = ByteBuffer.allocate(4 + 1 + 1);
    buffer.order(ByteOrder.LITTLE_ENDIAN);
    buffer.putInt(packageId);
    buffer.put((byte) (key ? 1 : 0));
    buffer.put((byte) (temp ? 1 : 0));
    return buffer.array();
  }
}
