package org.ton.ton4j.tl.types;

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
  private final byte[] addr;
  private final long lt;
  private final byte[] hash;
  private final TransactionMetadata metadata; // Placeholder for transactionMetadata

  public static DispatchQueueMessage deserialize(ByteBuffer buffer) {
    buffer.order(ByteOrder.LITTLE_ENDIAN);

    return DispatchQueueMessage.builder()
        .addr(Utils.read(buffer, 32))
        .lt(buffer.getLong())
        .hash(Utils.read(buffer, 32))
        .metadata(TransactionMetadata.deserialize(buffer))
        .build();
  }

  //  public byte[] serialize() {
  //    byte[] addrBytes = new byte[32];
  //    byte[] originalAddr = addr.toByteArray();
  //    int startAddr = 32 - originalAddr.length;
  //    System.arraycopy(originalAddr, 0, addrBytes, startAddr, originalAddr.length);
  //
  //    byte[] hashBytes = new byte[32];
  //    byte[] originalHash = hash.toByteArray();
  //    int startHash = 32 - originalHash.length;
  //    System.arraycopy(originalHash, 0, hashBytes, startHash, originalHash.length);
  //
  //    return ByteBuffer.allocate(32 + 8 + 32 + metadata.length)
  //        .order(ByteOrder.LITTLE_ENDIAN)
  //        .put(addrBytes)
  //        .putLong(lt)
  //        .put(hashBytes)
  //        .put(metadata)
  //        .array();
  //  }
}
