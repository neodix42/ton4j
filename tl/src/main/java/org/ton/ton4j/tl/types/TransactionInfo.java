package org.ton.ton4j.tl.types;

import java.io.Serializable;
import java.nio.ByteBuffer;
import lombok.Builder;
import lombok.Data;
import org.ton.ton4j.utils.Utils;

/**
 * liteServer.transactionInfo id:tonNode.blockIdExt proof:bytes transaction:bytes =
 * liteServer.TransactionInfo;
 */
@Builder
@Data
public class TransactionInfo implements Serializable, LiteServerAnswer {
  public final int TRANSACTION_INFO_ANSWER = 0;

  BlockIdExt id;
  byte[] proof;
  byte[] transaction;

  public static final int constructorId =
      (int)
          Utils.getQueryCrc32IEEEE(
              "liteServer.transactionInfo id:tonNode.blockIdExt proof:bytes transaction:bytes = liteServer.TransactionInfo");

  public byte[] serialize() {
    byte[] t1 = Utils.toBytes(proof);
    byte[] t2 = Utils.toBytes(transaction);
    ByteBuffer buffer = ByteBuffer.allocate(BlockIdExt.getSize() + 4 + t1.length + 4 + t2.length);
    buffer.put(id.serialize());
    buffer.put(t1);
    buffer.put(t2);
    return buffer.array();
  }

  public static TransactionInfo deserialize(ByteBuffer byteBuffer) {

    return TransactionInfo.builder()
        .id(BlockIdExt.deserialize(byteBuffer))
        .proof(Utils.fromBytes(byteBuffer))
        .transaction(Utils.fromBytes(byteBuffer))
        .build();
  }

  public static TransactionInfo deserialize(byte[] byteArray) {
    return deserialize(ByteBuffer.wrap(byteArray));
  }
}
