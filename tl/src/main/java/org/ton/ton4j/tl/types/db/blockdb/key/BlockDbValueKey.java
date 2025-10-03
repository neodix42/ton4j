package org.ton.ton4j.tl.types.db.blockdb.key;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import lombok.Builder;
import lombok.Data;
import org.ton.ton4j.tl.types.db.block.BlockIdExt;
import org.ton.ton4j.tl.types.db.filedb.Key;

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
public class BlockDbValueKey extends Key {
  int magic;
  BlockIdExt blockIdExt;

  public static BlockDbValueKey deserialize(ByteBuffer buffer) {
    buffer.order(ByteOrder.LITTLE_ENDIAN);

    return BlockDbValueKey.builder()
        .magic(buffer.getInt())
        .blockIdExt(BlockIdExt.deserialize(buffer))
        .build();
  }

  public byte[] serialize() {
    ByteBuffer buffer = ByteBuffer.allocate(4 + BlockIdExt.SERIALIZED_SIZE);
    buffer.order(ByteOrder.LITTLE_ENDIAN);
    buffer.putInt(2136461683);
    buffer.put(blockIdExt.serialize());
    return buffer.array();
  }

  public byte[] getKeyHash() {
    return serialize();
  }
}
