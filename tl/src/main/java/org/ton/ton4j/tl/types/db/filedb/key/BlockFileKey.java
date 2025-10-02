package org.ton.ton4j.tl.types.db.filedb.key;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import lombok.Builder;
import lombok.Data;
import org.ton.ton4j.tl.types.db.block.BlockIdExt;
import org.ton.ton4j.tl.types.db.filedb.Key;
import org.ton.ton4j.utils.Utils;

/**
 *
 *
 * <pre>
 * ton_api.tl
 * db.filedb.key.blockFile block_id:tonNode.blockIdExt = db.filedb.Key;
 * </pre>
 */
@Builder
@Data
public class BlockFileKey extends Key {
  int magic;
  BlockIdExt blockIdExt;

  public static BlockFileKey deserialize(ByteBuffer buffer) {
    buffer.order(ByteOrder.LITTLE_ENDIAN);

    return BlockFileKey.builder()
        .magic(buffer.getInt())
        .blockIdExt(BlockIdExt.deserialize(buffer))
        .build();
  }

  public byte[] serialize() {
    ByteBuffer buffer = ByteBuffer.allocate(4 + 80);
    buffer.order(ByteOrder.LITTLE_ENDIAN);
    buffer.putInt(-1326783375);
    buffer.put(blockIdExt.serialize());
    return buffer.array();
  }

  public String getKeyHash() {
    return Utils.bytesToHex(Utils.sha256AsArray(serialize())).toUpperCase();
  }
}
