package org.ton.ton4j.tl.types.db.files.pkg;

import java.io.Serializable;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;
import lombok.Builder;
import lombok.Data;
import org.ton.ton4j.tl.types.db.files.GlobalIndexValue;
import org.ton.ton4j.utils.Utils;

/**
 *
 *
 * <pre>
 * ton_api.tl
 * db.files.package.value package_id:int key:Bool temp:Bool firstblocks:(vector db.files.package.firstBlock) deleted:Bool
 *                  = db.files.package.Value;
 * </pre>
 */
@Builder
@Data
public class PackageValue implements Serializable, GlobalIndexValue {

  int packageId;
  boolean key;
  boolean temp;
  List<FirstBlock> firstblocks;
  boolean deleted;

  public static PackageValue deserialize(ByteBuffer buffer) {
    buffer.order(ByteOrder.LITTLE_ENDIAN);
    int packageId = buffer.getInt();
    boolean key = buffer.getInt() == Utils.TL_TRUE;
    boolean temp = buffer.getInt() == Utils.TL_TRUE;

    // Read firstblocks vector
    int firstblocksCount = buffer.getInt();
    List<FirstBlock> firstblocks = new ArrayList<>(firstblocksCount);
    for (int i = 0; i < firstblocksCount; i++) {
      firstblocks.add(FirstBlock.deserialize(buffer));
    }

    boolean deleted = buffer.get() != 0;

    return PackageValue.builder()
        .packageId(packageId)
        .key(key)
        .temp(temp)
        .firstblocks(firstblocks)
        .deleted(deleted)
        .build();
  }

  public byte[] serialize() {
    // Calculate buffer size
    int size = 4 + 1 + 1 + 4 + (firstblocks.size() * (4 + 8 + 4 + 4 + 8)) + 1;

    ByteBuffer buffer = ByteBuffer.allocate(size);
    buffer.order(ByteOrder.LITTLE_ENDIAN);

    buffer.putInt(packageId);
    buffer.putInt(key ? Utils.TL_TRUE : Utils.TL_FALSE);
    buffer.putInt(temp ? Utils.TL_TRUE : Utils.TL_FALSE);

    // Write firstblocks vector
    buffer.putInt(firstblocks.size());
    for (FirstBlock block : firstblocks) {
      buffer.put(block.serialize());
    }

    buffer.put((byte) (deleted ? 1 : 0));

    return buffer.array();
  }

  public static PackageValue deserialize(byte[] data) {
    return deserialize(ByteBuffer.wrap(data));
  }
}
