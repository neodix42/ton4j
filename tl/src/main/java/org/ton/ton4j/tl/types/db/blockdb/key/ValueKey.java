package org.ton.ton4j.tl.types.db.blockdb.key;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import lombok.Builder;
import lombok.Data;
import org.ton.ton4j.tl.liteserver.responses.BlockIdExt;
import org.ton.ton4j.tl.types.db.blockdb.Key;

/**
 *
 *
 * <pre>
 * ton_api.tl
 * db.blockdb.key.value id:tonNode.blockIdExt = db.blockdb.Key;
 * </pre>
 */
@Builder
@Data
public class ValueKey extends Key {

  BlockIdExt id;

  public static ValueKey deserialize(ByteBuffer buffer) {
    buffer.order(ByteOrder.LITTLE_ENDIAN);
    BlockIdExt id = BlockIdExt.deserialize(buffer);
    
    return ValueKey.builder()
        .id(id)
        .build();
  }

  @Override
  public byte[] serialize() {
    return id.serialize();
  }
}
