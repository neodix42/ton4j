package org.ton.ton4j.tl.queries;

import java.nio.ByteBuffer;
import lombok.Builder;
import lombok.Getter;
import org.ton.ton4j.tl.types.BlockId;
import org.ton.ton4j.tl.types.LiteServerQueryData;
import org.ton.ton4j.utils.Utils;

@Builder
@Getter
public class LookupBlockQuery implements LiteServerQueryData {

  public final int LOOKUP_BLOCK_QUERY = (int) Utils.getQueryCrc32IEEEE(getQueryName());

  private int mode;
  private BlockId id;
  private long lt;
  private int utime;

  public String getQueryName() {
    return "liteServer.lookupBlock mode:# id:tonNode.blockId lt:mode.1?long utime:mode.2?int = liteServer.BlockHeader";
  }

  public byte[] getQueryData() {
    int size = BlockId.getSize() + 4;
    if ((mode & 1) != 0) size += 8; // lt
    if ((mode & 2) != 0) size += 4; // utime

    ByteBuffer buffer = ByteBuffer.allocate(size);
    buffer.putInt(mode);
    buffer.put(id.serialize());

    if ((mode & 1) != 0) {
      buffer.putLong(lt);
    }

    if ((mode & 2) != 0) {
      buffer.putInt(utime);
    }

    return buffer.array();
  }
}
