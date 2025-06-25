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

  BlockIdExt block;

  public static GcBlockId deserialize(ByteBuffer buffer) {
    buffer.order(ByteOrder.LITTLE_ENDIAN);
    BlockIdExt block = BlockIdExt.deserialize(buffer);
    
    return GcBlockId.builder()
        .block(block)
        .build();
  }

  public byte[] serialize() {
    return block.serialize();
  }
}
