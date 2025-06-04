package org.ton.ton4j.tl.types;

import java.io.Serializable;
import java.nio.ByteBuffer;
import lombok.Builder;
import lombok.Data;

/** tonNode.blockId workchain:int shard:long seqno:int = tonNode.BlockId; */
@Builder
@Data
public class BlockId implements Serializable {
  int workchain;
  long shard;
  int seqno;

  public static int getSize() {
    return 4 + 8 + 4; // workchain (int) + shard (long) + seqno (int)
  }

  public byte[] serialize() {
    ByteBuffer buffer = ByteBuffer.allocate(getSize());
    buffer.putInt(workchain);
    buffer.putLong(shard);
    buffer.putInt(seqno);
    return buffer.array();
  }

  public static BlockId deserialize(ByteBuffer byteBuffer) {
    return BlockId.builder()
        .workchain(byteBuffer.getInt())
        .shard(byteBuffer.getLong())
        .seqno(byteBuffer.getInt())
        .build();
  }

  public static BlockId deserialize(byte[] byteArray) {
    return deserialize(ByteBuffer.wrap(byteArray));
  }
}
