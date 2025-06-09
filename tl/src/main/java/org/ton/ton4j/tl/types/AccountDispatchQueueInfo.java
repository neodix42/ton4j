package org.ton.ton4j.tl.types;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import lombok.Builder;
import lombok.Getter;
import org.ton.ton4j.utils.Utils;

/**
 * liteServer.accountDispatchQueueInfo addr:int256 size:long min_lt:long max_lt:long =
 * liteServer.AccountDispatchQueueInfo;
 */
@Builder
@Getter
public class AccountDispatchQueueInfo {
  private final byte[] addr;
  private final long size;
  private final long minLt;
  private final long maxLt;

  public static AccountDispatchQueueInfo deserialize(ByteBuffer buffer) {
    buffer.order(ByteOrder.LITTLE_ENDIAN);
    return AccountDispatchQueueInfo.builder()
        .addr(Utils.read(buffer, 32))
        .size(buffer.getLong())
        .minLt(buffer.getLong())
        .maxLt(buffer.getLong())
        .build();
  }

  public byte[] serialize() {

    return ByteBuffer.allocate(32 + 8 + 8 + 8)
        .order(ByteOrder.LITTLE_ENDIAN)
        .put(addr)
        .putLong(size)
        .putLong(minLt)
        .putLong(maxLt)
        .array();
  }
}
