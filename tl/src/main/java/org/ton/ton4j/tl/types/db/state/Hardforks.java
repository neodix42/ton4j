package org.ton.ton4j.tl.types.db.state;

import java.io.Serializable;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;
import lombok.Builder;
import lombok.Data;
import org.ton.ton4j.tl.liteserver.responses.BlockIdExt;

/**
 *
 *
 * <pre>
 * ton_api.tl
 * db.state.hardforks blocks:(vector tonNode.blockIdExt) = db.state.Hardforks;
 * </pre>
 */
@Builder
@Data
public class Hardforks implements Serializable {

  List<BlockIdExt> blocks;

  public static Hardforks deserialize(ByteBuffer buffer) {
    buffer.order(ByteOrder.LITTLE_ENDIAN);
    
    // Read blocks vector
    int blocksCount = buffer.getInt();
    List<BlockIdExt> blocks = new ArrayList<>(blocksCount);
    for (int i = 0; i < blocksCount; i++) {
      blocks.add(BlockIdExt.deserialize(buffer));
    }
    
    return Hardforks.builder()
        .blocks(blocks)
        .build();
  }

  public byte[] serialize() {
    // Calculate buffer size
    int size = 4; // 4 bytes for vector size
    for (BlockIdExt block : blocks) {
      size += block.serialize().length;
    }
    
    ByteBuffer buffer = ByteBuffer.allocate(size);
    buffer.order(ByteOrder.LITTLE_ENDIAN);
    
    // Write blocks vector
    buffer.putInt(blocks.size());
    for (BlockIdExt block : blocks) {
      buffer.put(block.serialize());
    }
    
    return buffer.array();
  }
}
