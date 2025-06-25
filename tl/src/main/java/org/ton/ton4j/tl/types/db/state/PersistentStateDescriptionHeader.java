package org.ton.ton4j.tl.types.db.state;

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
 * db.state.persistentStateDescriptionHeader masterchain_id:tonNode.blockIdExt start_time:int end_time:int = db.state.PersistentStateDescriptionHeader;
 * </pre>
 */
@Builder
@Data
public class PersistentStateDescriptionHeader implements Serializable {

  BlockIdExt masterchainId;
  int startTime;
  int endTime;

  public static PersistentStateDescriptionHeader deserialize(ByteBuffer buffer) {
    buffer.order(ByteOrder.LITTLE_ENDIAN);
    BlockIdExt masterchainId = BlockIdExt.deserialize(buffer);
    int startTime = buffer.getInt();
    int endTime = buffer.getInt();
    
    return PersistentStateDescriptionHeader.builder()
        .masterchainId(masterchainId)
        .startTime(startTime)
        .endTime(endTime)
        .build();
  }

  public byte[] serialize() {
    ByteBuffer buffer = ByteBuffer.allocate(masterchainId.serialize().length + 4 + 4);
    buffer.order(ByteOrder.LITTLE_ENDIAN);
    buffer.put(masterchainId.serialize());
    buffer.putInt(startTime);
    buffer.putInt(endTime);
    return buffer.array();
  }
}
