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
 * db.state.shardClient block:tonNode.blockIdExt = db.state.ShardClient;
 * </pre>
 */
@Builder
@Data
public class ShardClient implements Serializable {
  long magic;
  BlockIdExt block;

  public static ShardClient deserialize(ByteBuffer buffer) {
    buffer.order(ByteOrder.LITTLE_ENDIAN);
    long magic = buffer.getInt();
    BlockIdExt block = BlockIdExt.deserialize(buffer);

    return ShardClient.builder().magic(magic).block(block).build();
  }

  public byte[] serialize() {
    ByteBuffer buffer = ByteBuffer.allocate(4 + ((block != null) ? BlockIdExt.getSize() : 0));
    buffer.order(ByteOrder.LITTLE_ENDIAN);
    buffer.putInt(-912576121);
    if (block != null) {
      buffer.put(block.serialize());
    }
    return buffer.array();
  }
}
