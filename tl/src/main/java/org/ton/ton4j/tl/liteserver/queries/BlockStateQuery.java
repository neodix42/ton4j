package org.ton.ton4j.tl.liteserver.queries;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import lombok.Builder;
import lombok.Data;
import org.ton.ton4j.tl.liteserver.responses.BlockIdExt;
import org.ton.ton4j.tl.liteserver.responses.LiteServerQueryData;

@Builder
@Data
public class BlockStateQuery implements LiteServerQueryData {
  public static final int BLOCK_STATE_QUERY = -1167184202;

  private BlockIdExt id;

  public String getQueryName() {
    return "liteServer.getState id:tonNode.blockIdExt = liteServer.BlockState";
  }

  public byte[] getQueryData() {

    return ByteBuffer.allocate(80 + 4)
        .order(ByteOrder.LITTLE_ENDIAN)
        .putInt(BLOCK_STATE_QUERY)
        .put(id.serialize())
        .array();
  }
}
