package org.ton.ton4j.tl.queries;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import lombok.Builder;
import lombok.Getter;
import org.ton.ton4j.tl.types.BlockIdExt;
import org.ton.ton4j.tl.types.LiteServerQueryData;

@Builder
@Getter
public class BlockStateQuery implements LiteServerQueryData {
  public static final int BLOCK_STATE_QUERY = -1984567762; // 2ee6b589

  private BlockIdExt id;

  public String getQueryName() {
    return "liteServer.getState id:tonNode.blockIdExt = liteServer.BlockState";
  }

  public byte[] getQueryData() {
    int len = id.serialize().length + 4;
    ByteBuffer buffer = ByteBuffer.allocate(len);
    buffer.order(ByteOrder.LITTLE_ENDIAN);
    buffer.putInt(BLOCK_STATE_QUERY);
    buffer.put(id.serialize());
    return buffer.array();
  }
}
