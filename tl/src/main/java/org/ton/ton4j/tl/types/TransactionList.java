package org.ton.ton4j.tl.types;

import java.io.Serializable;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import lombok.Builder;
import lombok.Data;
import org.ton.ton4j.utils.Utils;

/**
 * liteServer.transactionList ids:(vector tonNode.blockIdExt) transactions:bytes =
 * liteServer.TransactionList;
 */
@Builder
@Data
public class TransactionList implements Serializable, LiteServerAnswer {
  public static final int TRANSACTION_LIST_ANSWER = 1864812043;

  List<BlockIdExt> ids;
  byte[] transactions;

  public static final int constructorId = TRANSACTION_LIST_ANSWER;

  public static TransactionList deserialize(ByteBuffer byteBuffer) {

    int idsCount = byteBuffer.getInt();
    List<BlockIdExt> ids = new ArrayList<>(idsCount);
    for (int i = 0; i < idsCount; i++) {
      ids.add(BlockIdExt.deserialize(byteBuffer));
    }

    byte[] transactions = Utils.fromBytes(byteBuffer); // multi-root boc

    return TransactionList.builder().ids(ids).transactions(transactions).build();
  }

  public static TransactionList deserialize(byte[] byteArray) {
    return deserialize(ByteBuffer.wrap(byteArray));
  }
}
