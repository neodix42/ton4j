package org.ton.ton4j.tl.types.db.filedb.key;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import lombok.Builder;
import lombok.Data;
import org.ton.ton4j.tl.liteserver.responses.BlockIdExt;
import org.ton.ton4j.tl.types.db.filedb.Key;

/**
 *
 *
 * <pre>
 * ton_api.tl
 * db.filedb.key.persistentStateFile block_id:tonNode.blockIdExt masterchain_block_id:tonNode.blockIdExt = db.filedb.Key;
 * </pre>
 */
@Builder
@Data
public class PersistentStateFileKey extends Key {

  BlockIdExt blockId;
  BlockIdExt masterchainBlockId;

  public static PersistentStateFileKey deserialize(ByteBuffer buffer) {
    buffer.order(ByteOrder.LITTLE_ENDIAN);
    BlockIdExt blockId = BlockIdExt.deserialize(buffer);
    BlockIdExt masterchainBlockId = BlockIdExt.deserialize(buffer);
    
    return PersistentStateFileKey.builder()
        .blockId(blockId)
        .masterchainBlockId(masterchainBlockId)
        .build();
  }

  @Override
  public byte[] serialize() {
    ByteBuffer buffer = ByteBuffer.allocate(BlockIdExt.getSize() * 2);
    buffer.order(ByteOrder.LITTLE_ENDIAN);
    buffer.put(blockId.serialize());
    buffer.put(masterchainBlockId.serialize());
    return buffer.array();
  }
}
