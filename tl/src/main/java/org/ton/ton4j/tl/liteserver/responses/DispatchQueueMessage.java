package org.ton.ton4j.tl.liteserver.responses;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import lombok.Builder;
import lombok.Getter;
import org.ton.ton4j.utils.Utils;

/**
 * liteServer.dispatchQueueMessage addr:int256 lt:long hash:int256
 * metadata:liteServer.transactionMetadata = liteServer.DispatchQueueMessage;
 */
@Builder
@Getter
public class DispatchQueueMessage {
  public final byte[] addr;
  private final long lt;
  public final byte[] hash;

  public String getAddr() {
    if (addr == null) {
      return "";
    }
    return Utils.bytesToHex(addr);
  }

  public String getHash() {
    if (hash == null) {
      return "";
    }
    return Utils.bytesToHex(hash);
  }

  private final TransactionMetadata metadata;

  public static DispatchQueueMessage deserialize(ByteBuffer buffer) {
    buffer.order(ByteOrder.LITTLE_ENDIAN);

    return DispatchQueueMessage.builder()
        .addr(Utils.read(buffer, 32))
        .lt(buffer.getLong())
        .hash(Utils.read(buffer, 32))
        .metadata(TransactionMetadata.deserialize(buffer))
        .build();
  }
}
