package org.ton.ton4j.tl.liteserver.responses;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import lombok.Builder;
import lombok.Data;
import org.ton.ton4j.utils.Utils;

/**
 * liteServer.transactionMetadata mode:# depth:int initiator:liteServer.accountId initiator_lt:long
 * = liteServer.TransactionMetadata;
 */
@Builder
@Data
public class TransactionMetadata {
  int mode;
  int depth;
  int workchain;
  public byte[] hash;
  long initiatorLt;

  public String getHash() {
    return Utils.bytesToHex(hash);
  }

  public byte[] serialize() {
    return ByteBuffer.allocate(4 + 4 + 4 + 32 + 8)
        .order(ByteOrder.LITTLE_ENDIAN)
        .putInt(mode)
        .putInt(depth)
        .putInt(workchain)
        .put(hash)
        .putLong(initiatorLt)
        .array();
  }

  public static TransactionMetadata deserialize(ByteBuffer byteBuffer) {
    byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
    return TransactionMetadata.builder()
        .mode(byteBuffer.getInt())
        .depth(byteBuffer.getInt())
        .workchain(byteBuffer.getInt())
        .hash(Utils.read(byteBuffer, 32))
        .initiatorLt(byteBuffer.getLong())
        .build();
  }
}
