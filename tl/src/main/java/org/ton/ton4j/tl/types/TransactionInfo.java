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
  BlockIdExt id;
  byte[] proof;
  byte[] transaction;

  public static final int constructorId =
      (int)
          Utils.getQueryCrc32IEEEE(
              "liteServer.transactionInfo id:tonNode.blockIdExt proof:bytes transaction:bytes = liteServer.TransactionInfo");

  public byte[] serialize() {
    ByteBuffer buffer =
        ByteBuffer.allocate(BlockIdExt.getSize() + 4 + proof.length + 4 + transaction.length);
    buffer.put(id.serialize());
    buffer.putInt(proof.length);
    buffer.put(proof);
    buffer.putInt(transaction.length);
    buffer.put(transaction);
    return buffer.array();
  }

  public static TransactionInfo deserialize(ByteBuffer byteBuffer) {
    BlockIdExt id = BlockIdExt.deserialize(byteBuffer);
    int proofLen = byteBuffer.getInt();
    byte[] proof = new byte[proofLen];
    byteBuffer.get(proof);
    int transactionLen = byteBuffer.getInt();
    byte[] transaction = new byte[transactionLen];
    byteBuffer.get(transaction);
    return TransactionInfo.builder().id(id).proof(proof).transaction(transaction).build();
  }

  public static TransactionInfo deserialize(byte[] byteArray) {
    return deserialize(ByteBuffer.wrap(byteArray));
  }
}
