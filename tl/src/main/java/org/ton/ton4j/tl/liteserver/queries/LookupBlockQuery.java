package org.ton.ton4j.tl.liteserver.queries;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import lombok.Builder;
import lombok.Getter;
import org.ton.ton4j.tl.liteserver.responses.BlockId;
import org.ton.ton4j.tl.liteserver.responses.LiteServerQueryData;

@Builder
@Getter
public class LookupBlockQuery implements LiteServerQueryData {

  public final int LOOKUP_BLOCK_QUERY = -87492834;

  private int mode;
  private BlockId id;
  private long lt;
  private int utime;

  public String getQueryName() {
    return "liteServer.lookupBlock mode:# id:tonNode.blockId lt:mode.1?long utime:mode.2?int = liteServer.BlockHeader";
  }

  public byte[] getQueryData() {
    int size = BlockId.getSize() + 4 + 4;
    if ((mode & 2) != 0) size += 8;
    if ((mode & 4) != 0) size += 4;

    ByteBuffer buffer = ByteBuffer.allocate(size);
    buffer.order(ByteOrder.LITTLE_ENDIAN);
    buffer.putInt(LOOKUP_BLOCK_QUERY);
    buffer.putInt(mode);
    buffer.put(id.serialize());

    if ((mode & 2) != 0) {
      buffer.putLong(lt);
    }

    if ((mode & 4) != 0) {
      buffer.putInt(utime);
    }

    return buffer.array();
  }
}
