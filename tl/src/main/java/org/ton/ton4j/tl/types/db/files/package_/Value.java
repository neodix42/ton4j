package org.ton.ton4j.tl.types.db.files.package_;

import java.io.Serializable;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;
import lombok.Builder;
import lombok.Data;

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
public class Value implements Serializable {

  int packageId;
  boolean key;
  boolean temp;
  List<FirstBlock> firstblocks;
  boolean deleted;

  public static Value deserialize(ByteBuffer buffer) {
    buffer.order(ByteOrder.LITTLE_ENDIAN);
    int packageId = buffer.getInt();
    boolean key = buffer.get() != 0;
    boolean temp = buffer.get() != 0;
    
    // Read firstblocks vector
    int firstblocksCount = buffer.getInt();
    List<FirstBlock> firstblocks = new ArrayList<>(firstblocksCount);
    for (int i = 0; i < firstblocksCount; i++) {
      firstblocks.add(FirstBlock.deserialize(buffer));
    }
    
    boolean deleted = buffer.get() != 0;
    
    return Value.builder()
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
    buffer.put((byte) (key ? 1 : 0));
    buffer.put((byte) (temp ? 1 : 0));
    
    // Write firstblocks vector
    buffer.putInt(firstblocks.size());
    for (FirstBlock block : firstblocks) {
      buffer.put(block.serialize());
    }
    
    buffer.put((byte) (deleted ? 1 : 0));
    
    return buffer.array();
  }
}
