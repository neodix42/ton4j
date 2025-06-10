package org.ton.ton4j.tl.liteserver.queries;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import lombok.Builder;
import lombok.Data;
import org.ton.ton4j.tl.liteserver.responses.BlockIdExt;
import org.ton.ton4j.tl.liteserver.responses.LiteServerQueryData;

@Builder
@Data
public class BlockHeaderQuery implements LiteServerQueryData {
  public static final int BLOCK_HEADER_QUERY = 569116318;
  private BlockIdExt id;
  private int mode;

  public String getQueryName() {
    return "liteServer.getBlockHeader id:tonNode.blockIdExt mode:# = liteServer.BlockHeader";
  }

  public byte[] getQueryData() {
    return ByteBuffer.allocate(serialize().length + 4)
        .order(ByteOrder.LITTLE_ENDIAN)
        .putInt(BLOCK_HEADER_QUERY)
        .put(serialize())
        .array();
  }

  public byte[] serialize() {
    return ByteBuffer.allocate(BlockIdExt.getSize() + 4).put(id.serialize()).putInt(mode).array();
  }
}
