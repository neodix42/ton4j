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
 * db.blockdb.key.lru id:tonNode.blockIdExt = db.blockdb.Key;
 * </pre>
 */
@Builder
@Data
public class LruKey extends Key {

  BlockIdExt id;

  public static LruKey deserialize(ByteBuffer buffer) {
    buffer.order(ByteOrder.LITTLE_ENDIAN);
    BlockIdExt id = BlockIdExt.deserialize(buffer);
    
    return LruKey.builder()
        .id(id)
        .build();
  }

  @Override
  public byte[] serialize() {
    return id.serialize();
  }
}
