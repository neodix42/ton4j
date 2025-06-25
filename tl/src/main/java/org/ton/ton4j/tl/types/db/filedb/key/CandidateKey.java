package org.ton.ton4j.tl.types.db.filedb.key;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import lombok.Builder;
import lombok.Data;
import org.ton.ton4j.tl.types.db.candidate.Id;
import org.ton.ton4j.tl.types.db.filedb.Key;

/**
 *
 *
 * <pre>
 * ton_api.tl
 * db.filedb.key.candidate id:db.candidate.id = db.filedb.Key;
 * </pre>
 */
@Builder
@Data
public class CandidateKey extends Key {

  Id id;

  public static CandidateKey deserialize(ByteBuffer buffer) {
    buffer.order(ByteOrder.LITTLE_ENDIAN);
    Id id = Id.deserialize(buffer);
    
    return CandidateKey.builder()
        .id(id)
        .build();
  }

  @Override
  public byte[] serialize() {
    return id.serialize();
  }
}
