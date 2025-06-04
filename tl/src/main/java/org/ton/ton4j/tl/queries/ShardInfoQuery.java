package org.ton.ton4j.tl.queries;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import lombok.Builder;
import lombok.Getter;
import org.ton.ton4j.tl.types.BlockIdExt;
import org.ton.ton4j.tl.types.LiteServerQueryData;

@Builder
@Getter
public class ShardInfoQuery implements LiteServerQueryData {
  public static final int SHARD_INFO_QUERY = 1185084453;

  private BlockIdExt id;
  private int workchain;
  private long shard;
  private boolean exact;

  public String getQueryName() {
    return "liteServer.getShardInfo id:tonNode.blockIdExt workchain:int shard:long exact:Bool = liteServer.ShardInfo";
  }

  public byte[] getQueryData() {
    int len = serialize().length + 4;
    ByteBuffer buffer = ByteBuffer.allocate(len);
    buffer.order(ByteOrder.LITTLE_ENDIAN);
    buffer.putInt(SHARD_INFO_QUERY);
    buffer.put(serialize());
    return buffer.array();
  }

  public byte[] serialize() {
    ByteBuffer buffer = ByteBuffer.allocate(BlockIdExt.getSize() + 4 + 8 + 1);
    buffer.put(id.serialize());
    buffer.putInt(workchain);
    buffer.putLong(shard);
    buffer.put(exact ? (byte) 1 : (byte) 0);
    return buffer.array();
  }
}
