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
 * db.state.gcBlockId block:tonNode.blockIdExt = db.state.GcBlockId;
 * </pre>
 */
@Builder
@Data
public class GcBlockId implements Serializable {
  long magic;
  BlockIdExt block;

  public static GcBlockId deserialize(ByteBuffer buffer) {
    buffer.order(ByteOrder.LITTLE_ENDIAN);
    long magic = buffer.getInt();
    BlockIdExt block = BlockIdExt.deserialize(buffer);

    return GcBlockId.builder().magic(magic).block(block).build();
  }

  public byte[] serialize() {
    ByteBuffer buffer = ByteBuffer.allocate(4 + ((block != null) ? BlockIdExt.getSize() : 0));
    buffer.order(ByteOrder.LITTLE_ENDIAN);
    buffer.putInt(-1015417890);
    if (block != null) {
      buffer.put(block.serialize());
    }
    return buffer.array();
  }
}
