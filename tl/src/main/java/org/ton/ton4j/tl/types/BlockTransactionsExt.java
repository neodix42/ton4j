package org.ton.ton4j.tl.types;

import java.io.Serializable;
import java.nio.ByteBuffer;
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
 * liteServer.blockTransactionsExt id:tonNode.blockIdExt req_count:# incomplete:Bool
 * transactions:bytes proof:bytes = liteServer.BlockTransactionsExt;
 */
@Builder
@Data
public class BlockTransactionsExt implements Serializable, LiteServerAnswer {
  public static final int BLOCK_TRANSACTIONS_EXT_ANSWER = -74449692;

  BlockIdExt id;
  int req_count;
  boolean incomplete;
  public byte[] transactions;
  public byte[] proof;

  public String getTransactions() {
    return Utils.bytesToHex(transactions);
  }

  public String getProof() {
    return Utils.bytesToHex(proof);
  }

  public List<Transaction> getTransactionsParsed() {
    List<Transaction> txs = new ArrayList<>();
    List<Cell> cells = CellBuilder.beginCell().fromBocMultiRoot(transactions).endCells();
    for (Cell c : cells) {
      txs.add(Transaction.deserialize(CellSlice.beginParse(c)));
    }
    return txs;
  }

  public static final int constructorId = BLOCK_TRANSACTIONS_EXT_ANSWER;

  public byte[] serialize() {
    byte[] t1 = Utils.toBytes(transactions);
    byte[] t2 = Utils.toBytes(proof);

    ByteBuffer buffer =
        ByteBuffer.allocate(BlockIdExt.getSize() + 4 + 1 + t1.length + 4 + t2.length);
    buffer.put(id.serialize());
    buffer.putInt(req_count);
    buffer.put(incomplete ? (byte) 1 : (byte) 0);
    buffer.put(t1);
    buffer.put(t2);

    return buffer.array();
  }

  public static BlockTransactionsExt deserialize(ByteBuffer byteBuffer) {
    BlockIdExt id = BlockIdExt.deserialize(byteBuffer);
    int req_count = byteBuffer.getInt();
    boolean incomplete = byteBuffer.getInt() == 0; // why 4 bytes?

    byte[] transactions = Utils.fromBytes(byteBuffer);

    byte[] proof = Utils.fromBytes(byteBuffer);

    return BlockTransactionsExt.builder()
        .id(id)
        .req_count(req_count)
        .incomplete(incomplete)
        .transactions(transactions)
        .proof(proof)
        .build();
  }

  public static BlockTransactionsExt deserialize(byte[] byteArray) {
    return deserialize(ByteBuffer.wrap(byteArray));
  }
}
