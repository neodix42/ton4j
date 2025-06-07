package org.ton.ton4j.tl.queries;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import lombok.Builder;
import lombok.Getter;
import org.ton.ton4j.tl.types.BlockIdExt;
import org.ton.ton4j.tl.types.LiteServerQueryData;
import org.ton.ton4j.tl.types.TransactionId3;

@Builder
@Getter
public class ListBlockTransactionsQuery implements LiteServerQueryData {
  public static final int LIST_BLOCK_TRANSACTIONS_QUERY = -1375942694;

  private BlockIdExt id;
  private int mode;
  private int count;
  private TransactionId3 afterTx;
  private boolean reverseOrder;
  private boolean wantProof;

  public String getQueryName() {
    return "liteServer.listBlockTransactions id:tonNode.blockIdExt mode:# count:# after:mode.7?liteServer.transactionId3 reverse_order:mode.6?true want_proof:mode.5?true = liteServer.BlockTransactions";
  }

  public byte[] getQueryData() {
    // Calculate size
    int size = BlockIdExt.getSize() + 4 + 4 + 4;
    if ((mode & 128) != 0) size += TransactionId3.getSize(); // after
    if ((mode & 64) != 0) size += 1; // reverse_order
    if ((mode & 32) != 0) size += 1; // want_proof

    ByteBuffer buffer = ByteBuffer.allocate(size);
    buffer.order(ByteOrder.LITTLE_ENDIAN);
    buffer.putInt(LIST_BLOCK_TRANSACTIONS_QUERY);
    buffer.put(id.serialize());
    buffer.putInt(mode);
    buffer.putInt(count);

    if ((mode & 128) != 0 && afterTx != null) {
      buffer.put(afterTx.serialize());
    }

    if ((mode & 64) != 0) {
      buffer.put((byte) (reverseOrder ? 1 : 0));
    }

    if ((mode & 32) != 0) {
      buffer.put((byte) (wantProof ? 1 : 0));
    }

    return buffer.array();
  }
}
