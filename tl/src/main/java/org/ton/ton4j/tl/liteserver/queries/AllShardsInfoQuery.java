package org.ton.ton4j.tl.liteserver.queries;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import lombok.Builder;
import lombok.Data;
import org.ton.ton4j.tl.liteserver.responses.BlockIdExt;
import org.ton.ton4j.tl.liteserver.responses.LiteServerQueryData;

@Builder
@Data
public class AllShardsInfoQuery implements LiteServerQueryData {
  public static final int ALL_SHARDS_INFO_QUERY = 1960050027;

  private BlockIdExt id;

  public String getQueryName() {
    return "liteServer.getAllShardsInfo id:tonNode.blockIdExt = liteServer.AllShardsInfo";
  }

  public byte[] getQueryData() {
    int len = serialize().length + 4;
    ByteBuffer buffer = ByteBuffer.allocate(len);
    buffer.order(ByteOrder.LITTLE_ENDIAN);
    buffer.putInt(ALL_SHARDS_INFO_QUERY);
    buffer.put(serialize());
    return buffer.array();
  }

  public byte[] serialize() {
    ByteBuffer buffer = ByteBuffer.allocate(BlockIdExt.getSize());
    buffer.put(id.serialize());
    return buffer.array();
  }
}
