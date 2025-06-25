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
 * db.state.asyncSerializer block:tonNode.blockIdExt last:tonNode.blockIdExt last_ts:int = db.state.AsyncSerializer;
 * </pre>
 */
@Builder
@Data
public class AsyncSerializer implements Serializable {

  BlockIdExt block;
  BlockIdExt last;
  int lastTs;

  public static AsyncSerializer deserialize(ByteBuffer buffer) {
    buffer.order(ByteOrder.LITTLE_ENDIAN);
    BlockIdExt block = BlockIdExt.deserialize(buffer);
    BlockIdExt last = BlockIdExt.deserialize(buffer);
    int lastTs = buffer.getInt();
    
    return AsyncSerializer.builder()
        .block(block)
        .last(last)
        .lastTs(lastTs)
        .build();
  }

  public byte[] serialize() {
    ByteBuffer buffer = ByteBuffer.allocate(block.serialize().length + last.serialize().length + 4);
    buffer.order(ByteOrder.LITTLE_ENDIAN);
    buffer.put(block.serialize());
    buffer.put(last.serialize());
    buffer.putInt(lastTs);
    return buffer.array();
  }
}
