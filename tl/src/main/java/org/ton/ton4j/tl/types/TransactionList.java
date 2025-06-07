package org.ton.ton4j.tl.types;

import java.io.Serializable;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;
import lombok.Builder;
import lombok.Data;
import org.ton.ton4j.cell.Cell;
import org.ton.ton4j.cell.CellBuilder;
import org.ton.ton4j.cell.CellSlice;
import org.ton.ton4j.tlb.Transaction;
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

  public String getTransactions() {
    return Utils.bytesToHex(transactions);
  }

  public List<Transaction> getTransactionsParsed() {
    List<Transaction> txs = new ArrayList<>();
    if ((transactions == null) || (transactions.length < 10)) {
      return txs;
    } else {
      List<Cell> cells = CellBuilder.beginCell().fromBocMultiRoot(transactions).endCells();
      for (Cell c : cells) {
        txs.add(Transaction.deserialize(CellSlice.beginParse(c)));
      }
      return txs;
    }
  }

  public static final int constructorId = TRANSACTION_LIST_ANSWER;

  public static TransactionList deserialize(ByteBuffer byteBuffer) {
    byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
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
