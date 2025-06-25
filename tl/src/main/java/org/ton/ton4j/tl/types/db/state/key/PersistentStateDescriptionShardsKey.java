package org.ton.ton4j.tl.types.db.state.key;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import lombok.Builder;
import lombok.Data;
import org.ton.ton4j.tl.types.db.state.Key;

/**
 *
 *
 * <pre>
 * ton_api.tl
 * db.state.key.persistentStateDescriptionShards masterchain_seqno:int = db.state.Key;
 * </pre>
 */
@Builder
@Data
public class PersistentStateDescriptionShardsKey extends Key {

  int masterchainSeqno;

  public static PersistentStateDescriptionShardsKey deserialize(ByteBuffer buffer) {
    buffer.order(ByteOrder.LITTLE_ENDIAN);
    int masterchainSeqno = buffer.getInt();
    
    return PersistentStateDescriptionShardsKey.builder()
        .masterchainSeqno(masterchainSeqno)
        .build();
  }

  @Override
  public byte[] serialize() {
    ByteBuffer buffer = ByteBuffer.allocate(4);
    buffer.order(ByteOrder.LITTLE_ENDIAN);
    buffer.putInt(masterchainSeqno);
    return buffer.array();
  }
}
