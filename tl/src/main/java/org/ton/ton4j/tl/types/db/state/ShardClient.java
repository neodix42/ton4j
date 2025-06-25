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

  BlockIdExt block;

  public static ShardClient deserialize(ByteBuffer buffer) {
    buffer.order(ByteOrder.LITTLE_ENDIAN);
    BlockIdExt block = BlockIdExt.deserialize(buffer);
    
    return ShardClient.builder()
        .block(block)
        .build();
  }

  public byte[] serialize() {
    return block.serialize();
  }
}
