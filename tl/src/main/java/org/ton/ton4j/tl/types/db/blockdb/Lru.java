package org.ton.ton4j.tl.types.db.blockdb;

import java.io.Serializable;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import lombok.Builder;
import lombok.Data;
import org.ton.ton4j.tl.liteserver.responses.BlockIdExt;
import org.ton.ton4j.utils.Utils;

/**
 *
 *
 * <pre>
 * ton_api.tl
 * db.blockdb.lru id:tonNode.blockIdExt prev:int256 next:int256 = db.blockdb.Lru;
 * </pre>
 */
@Builder
@Data
public class Lru implements Serializable {

  BlockIdExt id;
  public byte[] prev;  // int256
  public byte[] next;  // int256

  // Convenience getters
  public String getPrev() {
    return Utils.bytesToHex(prev);
  }
  
  public String getNext() {
    return Utils.bytesToHex(next);
  }

  public static Lru deserialize(ByteBuffer buffer) {
    buffer.order(ByteOrder.LITTLE_ENDIAN);
    BlockIdExt id = BlockIdExt.deserialize(buffer);
    byte[] prev = Utils.read(buffer, 32);
    byte[] next = Utils.read(buffer, 32);
    
    return Lru.builder()
        .id(id)
        .prev(prev)
        .next(next)
        .build();
  }

  public byte[] serialize() {
    ByteBuffer buffer = ByteBuffer.allocate(BlockIdExt.getSize() + 32 + 32);
    buffer.order(ByteOrder.LITTLE_ENDIAN);
    buffer.put(id.serialize());
    buffer.put(prev);
    buffer.put(next);
    return buffer.array();
  }
}
