package org.ton.ton4j.tl.types.db.block;

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
 * db.block.packedInfo id:tonNode.blockIdExt unixtime:int offset:long = db.block.Info;
 * </pre>
 */
@Builder
@Data
public class PackedInfo implements Serializable {

  BlockIdExt id;
  int unixtime;
  long offset;

  public static PackedInfo deserialize(ByteBuffer buffer) {
    buffer.order(ByteOrder.LITTLE_ENDIAN);
    BlockIdExt id = BlockIdExt.deserialize(buffer);
    int unixtime = buffer.getInt();
    long offset = buffer.getLong();
    
    return PackedInfo.builder()
        .id(id)
        .unixtime(unixtime)
        .offset(offset)
        .build();
  }

  public byte[] serialize() {
    ByteBuffer buffer = ByteBuffer.allocate(BlockIdExt.getSize() + 4 + 8);
    buffer.order(ByteOrder.LITTLE_ENDIAN);
    buffer.put(id.serialize());
    buffer.putInt(unixtime);
    buffer.putLong(offset);
    return buffer.array();
  }
}
