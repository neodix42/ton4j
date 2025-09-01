package org.ton.ton4j.tl.types.db.files;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import org.ton.ton4j.tl.types.db.files.key.IndexKey;
import org.ton.ton4j.tl.types.db.files.key.PackageKey;

public interface GlobalIndexKey {
  static GlobalIndexKey deserialize(ByteBuffer byteBuffer) {
    byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
    int magic = byteBuffer.getInt();
    if (magic == 0x7dc40502) {
      return IndexKey.builder().build();
    } else if (magic == 0xa504033e) {
      PackageKey packageKey = PackageKey.builder().build();
      packageKey.setPackageId(byteBuffer.getInt());
      packageKey.setKey(byteBuffer.getInt() == 0);
      packageKey.setTemp(byteBuffer.getInt() == 0);
      return packageKey;
    } else {
      throw new IllegalArgumentException("Unsupported magic number: " + magic);
    }
  }

  static GlobalIndexKey deserialize(byte[] data) {
    return deserialize(ByteBuffer.wrap(data));
  }
}
