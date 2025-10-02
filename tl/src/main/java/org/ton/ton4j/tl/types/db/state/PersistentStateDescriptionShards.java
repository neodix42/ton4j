package org.ton.ton4j.tl.types.db.state;

import java.io.Serializable;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;
import lombok.Builder;
import lombok.Data;
import org.ton.ton4j.tl.liteserver.responses.BlockIdExt;

/**
 *
 *
 * <pre>
 * ton_api.tl
 * db.state.persistentStateDescriptionShards shard_blocks:(vector tonNode.blockIdExt) = db.state.PersistentStateDescriptionShards;
 * </pre>
 */
@Builder
@Data
public class PersistentStateDescriptionShards implements Serializable {
  int magic;
  List<BlockIdExt> shardBlocks;

  public static PersistentStateDescriptionShards deserialize(ByteBuffer buffer) {
    buffer.order(ByteOrder.LITTLE_ENDIAN);

    // Read shard_blocks vector
    int magic = buffer.getInt();
    int shardBlocksCount = buffer.getInt();
    List<BlockIdExt> shardBlocks = new ArrayList<>(shardBlocksCount);
    for (int i = 0; i < shardBlocksCount; i++) {
      shardBlocks.add(BlockIdExt.deserialize(buffer));
    }

    return PersistentStateDescriptionShards.builder().magic(magic).shardBlocks(shardBlocks).build();
  }

  public byte[] serialize() {
    // Calculate buffer size
    int size = 8; // 4 bytes for vector size
    for (BlockIdExt block : shardBlocks) {
      size += block.serialize().length;
    }

    ByteBuffer buffer = ByteBuffer.allocate(size);
    buffer.order(ByteOrder.LITTLE_ENDIAN);
    buffer.putInt(-433023920);
    // Write shard_blocks vector
    buffer.putInt(shardBlocks.size());
    for (BlockIdExt block : shardBlocks) {
      buffer.put(block.serialize());
    }

    return buffer.array();
  }
}
