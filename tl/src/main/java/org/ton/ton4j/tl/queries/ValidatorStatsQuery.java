package org.ton.ton4j.tl.queries;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import lombok.Builder;
import lombok.Data;
import org.ton.ton4j.tl.types.BlockIdExt;
import org.ton.ton4j.tl.types.LiteServerQueryData;
import org.ton.ton4j.utils.Utils;

@Builder
@Data
public class ValidatorStatsQuery implements LiteServerQueryData {
  public static final int GET_VALIDATOR_STATS_QUERY = 152721596;

  private final int mode;
  private final BlockIdExt id;
  private final int limit;
  public final byte[] startAfter;
  private final int modifiedAfter;

  public String getStartAfter() {
    return Utils.bytesToHex(startAfter);
  }

  public String getQueryName() {
    return "liteServer.getValidatorStats#091a58bc mode:# id:Node.blockIdExt limit:int start_after:mode.0?int256 modified_after:mode.2?int = liteServer.ValidatorStats";
  }

  public byte[] getQueryData() {
    // Calculate total size
    int size = 4 + BlockIdExt.getSize() + 4;
    if ((mode & 1) != 0) size += 32;
    if ((mode & 4) != 0) size += 4;

    ByteBuffer buffer =
        ByteBuffer.allocate(size)
            .order(ByteOrder.LITTLE_ENDIAN)
            .putInt(mode)
            .put(id.serialize())
            .putInt(limit);

    // Add optional fields based on mode flags
    if ((mode & 1) != 0) {
      buffer.put(startAfter);
    }
    if ((mode & 4) != 0) {
      buffer.putInt(modifiedAfter);
    }

    return ByteBuffer.allocate(buffer.array().length + 4)
        .order(ByteOrder.LITTLE_ENDIAN)
        .putInt(GET_VALIDATOR_STATS_QUERY)
        .put(buffer.array())
        .array();
  }
}
