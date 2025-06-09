package org.ton.ton4j.tl.liteserver.queries;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import lombok.Builder;
import lombok.Getter;
import org.ton.ton4j.tl.liteserver.responses.BlockIdExt;
import org.ton.ton4j.tl.liteserver.responses.LiteServerQueryData;

@Builder
@Getter
public class BlockQuery implements LiteServerQueryData {
  public static final int BLOCK_QUERY = 1668796173;
  BlockIdExt id;

  public String getQueryName() {
    return "liteServer.getBlock id:tonNode.blockIdExt = liteServer.BlockData";
  }

  public byte[] getQueryData() {
    return ByteBuffer.allocate(id.serialize().length + 4)
        .order(ByteOrder.LITTLE_ENDIAN)
        .putInt(BLOCK_QUERY)
        .put(id.serialize())
        .array();
  }
}
