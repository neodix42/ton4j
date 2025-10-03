package org.ton.ton4j.tl.types.db.block;

import java.io.Serializable;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import lombok.Builder;
import lombok.Data;

/**
 *
 *
 * <pre>
 * ton_api.tl
 * db.block.archivedInfo id:tonNode.blockIdExt flags:# next:flags.0?tonNode.blockIdExt = db.block.Info;
 * </pre>
 */
@Builder
@Data
public class ArchivedInfo implements Serializable {

  BlockIdExt id;
  public BigInteger flags;
  BlockIdExt next;

  public static ArchivedInfo deserialize(ByteBuffer buffer) {
    buffer.order(ByteOrder.LITTLE_ENDIAN);
    buffer.get();

    BlockIdExt id = BlockIdExt.deserialize(buffer);
    int flagsInt = buffer.getInt();
    BigInteger flags = BigInteger.valueOf(flagsInt);

    ArchivedInfoBuilder builder = ArchivedInfo.builder().id(id).flags(flags);

    // Optional fields based on flags
    if (flags.testBit(0)) {
      builder.next(BlockIdExt.deserialize(buffer));
    }

    return builder.build();
  }

  public byte[] serialize() {
    // Calculate buffer size based on which optional fields are present
    int size = BlockIdExt.SERIALIZED_SIZE + 4; // id + flags

    if (flags.testBit(0) && next != null) {
      size += BlockIdExt.SERIALIZED_SIZE;
    }

    ByteBuffer buffer = ByteBuffer.allocate(size);
    buffer.order(ByteOrder.LITTLE_ENDIAN);

    buffer.put(id.serialize());
    buffer.putInt(flags.intValue());

    if (flags.testBit(0) && next != null) {
      buffer.put(next.serialize());
    }

    return buffer.array();
  }
}
