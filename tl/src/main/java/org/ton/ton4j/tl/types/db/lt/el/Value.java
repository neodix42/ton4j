package org.ton.ton4j.tl.types.db.lt.el;

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
 * db.lt.el.value id:tonNode.blockIdExt lt:long ts:int = db.lt.el.Value;
 * </pre>
 */
@Builder
@Data
public class Value implements Serializable {

  BlockIdExt id;
  long lt;
  int ts;

  public static Value deserialize(ByteBuffer buffer) {
    buffer.order(ByteOrder.LITTLE_ENDIAN);
    BlockIdExt id = BlockIdExt.deserialize(buffer);
    long lt = buffer.getLong();
    int ts = buffer.getInt();
    
    return Value.builder()
        .id(id)
        .lt(lt)
        .ts(ts)
        .build();
  }

  public byte[] serialize() {
    ByteBuffer buffer = ByteBuffer.allocate(id.serialize().length + 8 + 4);
    buffer.order(ByteOrder.LITTLE_ENDIAN);
    buffer.put(id.serialize());
    buffer.putLong(lt);
    buffer.putInt(ts);
    return buffer.array();
  }
}
