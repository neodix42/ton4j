package org.ton.ton4j.tl.types.db.state;

import java.io.Serializable;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import lombok.Builder;
import lombok.Data;
import org.ton.ton4j.tl.liteserver.responses.BlockIdExt;

/**
 *
 *
 * <pre>
 * ton_api.tl
 * db.state.initBlockId block:tonNode.blockIdExt = db.state.InitBlockId;
 * </pre>
 */
@Builder
@Data
public class InitBlockId implements Serializable {
  long magic;
  BlockIdExt block;

  public static InitBlockId deserialize(ByteBuffer buffer) {
    buffer.order(ByteOrder.LITTLE_ENDIAN);
    long magic = buffer.getInt();
    BlockIdExt block = BlockIdExt.deserialize(buffer);

    return InitBlockId.builder().magic(magic).block(block).build();
  }

  public byte[] serialize() {
    ByteBuffer buffer = ByteBuffer.allocate(4 + ((block != null) ? BlockIdExt.getSize() : 0));
    buffer.order(ByteOrder.LITTLE_ENDIAN);
    buffer.putInt(1971484899);
    if (block != null) {
      buffer.put(block.serialize());
    }
    return buffer.array();
  }
}
