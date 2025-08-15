package org.ton.ton4j.tl.liteserver.responses;

import java.io.Serializable;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import lombok.Builder;
import lombok.Data;

/** tonNode.blockId workchain:int shard:long seqno:int = tonNode.BlockId; */
@Builder
@Data
public class BlockId implements Serializable {
  int workchain;
  public long shard;
  int seqno;

  public long getSeqno() {
    return seqno & 0xFFFFFFFFL;
  }

  public String getShard() {
    return Long.toUnsignedString(shard, 16);
  }

  //  public String getShard() {
  //    return Long.toHexString(shard);
  //  }

  public static int getSize() {
    return 4 + 8 + 4; // workchain (int) + shard (long) + seqno (int)
  }

  public byte[] serialize() {
    ByteBuffer buffer = ByteBuffer.allocate(getSize());
    buffer.order(ByteOrder.LITTLE_ENDIAN);
    buffer.putInt(workchain);
    buffer.putLong(shard);
    buffer.putInt(seqno);
    return buffer.array();
  }

  public static BlockId deserialize(ByteBuffer byteBuffer) {
    byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
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
