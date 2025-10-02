package org.ton.ton4j.tl.types.db.block;

import java.io.Serializable;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import lombok.Builder;
import lombok.Data;

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
  int magic;
  BlockIdExt id;
  int unixtime;
  long offset;

  public static PackedInfo deserialize(ByteBuffer buffer) {
    buffer.order(ByteOrder.LITTLE_ENDIAN);
    int magic = buffer.getInt();
    BlockIdExt id = BlockIdExt.deserialize(buffer);
    int unixtime = buffer.getInt();
    long offset = buffer.getLong();

    return PackedInfo.builder().magic(magic).id(id).unixtime(unixtime).offset(offset).build();
  }

  public byte[] serialize() {
    ByteBuffer buffer = ByteBuffer.allocate(4 + BlockIdExt.SERIALIZED_SIZE + 4 + 8);
    buffer.order(ByteOrder.LITTLE_ENDIAN);
    buffer.putInt(1186697618);
    buffer.put(id.serialize());
    buffer.putInt(unixtime);
    buffer.putLong(offset);
    return buffer.array();
  }
}
