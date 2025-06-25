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

  BlockIdExt block;

  public static InitBlockId deserialize(ByteBuffer buffer) {
    buffer.order(ByteOrder.LITTLE_ENDIAN);
    BlockIdExt block = BlockIdExt.deserialize(buffer);
    
    return InitBlockId.builder()
        .block(block)
        .build();
  }

  public byte[] serialize() {
    return block.serialize();
  }
}
