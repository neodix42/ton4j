package org.ton.ton4j.tl.liteserver.queries;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import lombok.Builder;
import lombok.Getter;
import org.ton.ton4j.tl.liteserver.responses.BlockIdExt;
import org.ton.ton4j.tl.liteserver.responses.LiteServerQueryData;
import org.ton.ton4j.utils.Utils;

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
    return ByteBuffer.allocate(serialize().length + 4)
        .order(ByteOrder.LITTLE_ENDIAN)
        .putInt(SHARD_INFO_QUERY)
        .put(serialize())
        .array();
  }

  public byte[] serialize() {
    return ByteBuffer.allocate(BlockIdExt.getSize() + 4 + 8 + 4)
        .order(ByteOrder.LITTLE_ENDIAN)
        .put(id.serialize())
        .putInt(workchain)
        .putLong(shard)
        .putInt(exact ? Utils.TL_TRUE : Utils.TL_FALSE)
        .array();
  }
}
