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
 * db.filedb.key.proof block_id:tonNode.blockIdExt = db.filedb.Key;
 * </pre>
 */
@Builder
@Data
public class ProofKey extends Key {

  BlockIdExt blockId;

  public static ProofKey deserialize(ByteBuffer buffer) {
    buffer.order(ByteOrder.LITTLE_ENDIAN);
    BlockIdExt blockId = BlockIdExt.deserialize(buffer);
    
    return ProofKey.builder()
        .blockId(blockId)
        .build();
  }

  @Override
  public byte[] serialize() {
    return blockId.serialize();
  }
}
