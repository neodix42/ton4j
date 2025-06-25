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
 * db.blockdb.value next:tonNode.blockIdExt data:bytes = db.blockdb.Value;
 * </pre>
 */
@Builder
@Data
public class Value implements Serializable {

  BlockIdExt next;
  public byte[] data;

  public String getData() {
    return Utils.bytesToHex(data);
  }

  public static Value deserialize(ByteBuffer buffer) {
    buffer.order(ByteOrder.LITTLE_ENDIAN);
    BlockIdExt next = BlockIdExt.deserialize(buffer);
    byte[] data = Utils.fromBytes(buffer);

    return Value.builder().next(next).data(data).build();
  }

  public byte[] serialize() {
    ByteBuffer buffer = ByteBuffer.allocate(BlockIdExt.getSize() + Utils.toBytes(data).length);
    buffer.order(ByteOrder.LITTLE_ENDIAN);
    buffer.put(next.serialize());
    buffer.put(Utils.toBytes(data));
    return buffer.array();
  }
}
