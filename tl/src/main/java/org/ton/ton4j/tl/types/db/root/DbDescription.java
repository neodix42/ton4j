package org.ton.ton4j.tl.types.db.root;

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
 * db.root.dbDescription version:int first_masterchain_block_id:tonNode.blockIdExt flags:int = db.root.DbDescription;
 * </pre>
 */
@Builder
@Data
public class DbDescription implements Serializable {

  int version;
  BlockIdExt firstMasterchainBlockId;
  int flags;

  public static DbDescription deserialize(ByteBuffer buffer) {
    buffer.order(ByteOrder.LITTLE_ENDIAN);
    int version = buffer.getInt();
    BlockIdExt firstMasterchainBlockId = BlockIdExt.deserialize(buffer);
    int flags = buffer.getInt();
    
    return DbDescription.builder()
        .version(version)
        .firstMasterchainBlockId(firstMasterchainBlockId)
        .flags(flags)
        .build();
  }

  public byte[] serialize() {
    ByteBuffer buffer = ByteBuffer.allocate(4 + BlockIdExt.getSize() + 4);
    buffer.order(ByteOrder.LITTLE_ENDIAN);
    buffer.putInt(version);
    buffer.put(firstMasterchainBlockId.serialize());
    buffer.putInt(flags);
    return buffer.array();
  }
}
