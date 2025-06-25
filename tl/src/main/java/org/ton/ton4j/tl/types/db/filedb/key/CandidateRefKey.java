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
 * db.filedb.key.candidateRef id:tonNode.blockIdExt = db.filedb.Key;
 * </pre>
 */
@Builder
@Data
public class CandidateRefKey extends Key {

  BlockIdExt id;

  public static CandidateRefKey deserialize(ByteBuffer buffer) {
    buffer.order(ByteOrder.LITTLE_ENDIAN);
    BlockIdExt id = BlockIdExt.deserialize(buffer);
    
    return CandidateRefKey.builder()
        .id(id)
        .build();
  }

  @Override
  public byte[] serialize() {
    return id.serialize();
  }
}
