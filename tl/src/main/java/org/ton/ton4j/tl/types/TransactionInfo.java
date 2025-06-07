package org.ton.ton4j.tl.types;

import java.io.Serializable;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import lombok.Builder;
import lombok.Data;
import org.ton.ton4j.cell.Cell;
import org.ton.ton4j.cell.CellSlice;
import org.ton.ton4j.tlb.Transaction;
import org.ton.ton4j.utils.Utils;

/**
 * liteServer.transactionInfo id:tonNode.blockIdExt proof:bytes transaction:bytes =
 * liteServer.TransactionInfo;
 */
@Builder
@Data
public class TransactionInfo implements Serializable, LiteServerAnswer {
  public static final int TRANSACTION_INFO_ANSWER = 249490759;

  BlockIdExt id;
  public byte[] proof;
  public byte[] transaction;

  public String getTransaction() {
    return Utils.bytesToHex(transaction);
  }

  public String getProof() {
    return Utils.bytesToHex(proof);
  }

  public Transaction getTransactionParsed() {
    if ((transaction == null) || (transaction.length < 10)) {
      return null;
    } else {
      return Transaction.deserialize(CellSlice.beginParse(Cell.fromBoc(transaction)));
    }
  }

  public static final int constructorId = TRANSACTION_INFO_ANSWER;

  public byte[] serialize() {
    byte[] t1 = Utils.toBytes(proof);
    byte[] t2 = Utils.toBytes(transaction);
    ByteBuffer byteBuffer =
        ByteBuffer.allocate(BlockIdExt.getSize() + 4 + t1.length + 4 + t2.length);
    byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
    byteBuffer.put(id.serialize());
    byteBuffer.put(t1);
    byteBuffer.put(t2);
    return byteBuffer.array();
  }

  public static TransactionInfo deserialize(ByteBuffer byteBuffer) {
    byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
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
