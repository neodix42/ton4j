package org.ton.ton4j.tl.types.db.files;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import org.ton.ton4j.tl.types.db.files.index.IndexValue;
import org.ton.ton4j.tl.types.db.files.package_.PackageValue;

public interface GlobalIndexValue {
  static GlobalIndexValue deserialize(ByteBuffer byteBuffer) {
    byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
    int magic = byteBuffer.getInt();

    if (magic == 0xa2b1dafc) {
      return IndexValue.deserialize(byteBuffer);
    } else if (magic == 0xe44cd52b) {
      return PackageValue.deserialize(byteBuffer);
    } else {
      throw new IllegalArgumentException("Unsupported magic number: " + magic);
    }
  }

  static GlobalIndexValue deserialize(byte[] data) {
    return deserialize(ByteBuffer.wrap(data));
  }
}
