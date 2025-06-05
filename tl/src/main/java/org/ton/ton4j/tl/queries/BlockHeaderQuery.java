package org.ton.ton4j.tl.queries;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import lombok.Builder;
import lombok.Getter;
import org.ton.ton4j.tl.types.BlockIdExt;
import org.ton.ton4j.tl.types.LiteServerQueryData;

@Builder
@Getter
public class BlockHeaderQuery implements LiteServerQueryData {
  public static final int BLOCK_HEADER_QUERY = 569116318;
  private BlockIdExt id;
  private int mode;

  public String getQueryName() {
    return "liteServer.getBlockHeader id:tonNode.blockIdExt mode:# = liteServer.BlockHeader";
  }

  public byte[] getQueryData() {
    int len = serialize().length + 4;
    ByteBuffer buffer = ByteBuffer.allocate(len);
    buffer.order(ByteOrder.LITTLE_ENDIAN);
    buffer.putInt(BLOCK_HEADER_QUERY);
    buffer.put(serialize());
    return buffer.array();
  }

  public byte[] serialize() {
    ByteBuffer buffer = ByteBuffer.allocate(BlockIdExt.getSize() + 4);
    buffer.put(id.serialize());
    buffer.putInt(mode);
    return buffer.array();
  }
}
