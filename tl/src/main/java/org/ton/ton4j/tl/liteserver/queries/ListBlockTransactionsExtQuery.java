package org.ton.ton4j.tl.liteserver.queries;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import lombok.Builder;
import lombok.Data;
import org.ton.ton4j.tl.liteserver.responses.BlockIdExt;
import org.ton.ton4j.tl.liteserver.responses.LiteServerQueryData;
import org.ton.ton4j.tl.liteserver.responses.TransactionId3;

@Builder
@Data
public class ListBlockTransactionsExtQuery implements LiteServerQueryData {
  public static final int LIST_BLOCK_TRANSACTIONS_EXT_QUERY = 7986524;

  private BlockIdExt id;
  private int mode;
  private int count;
  private TransactionId3 after;
  private boolean reverseOrder;
  private boolean wantProof;

  public String getQueryName() {
    return "liteServer.listBlockTransactionsExt id:tonNode.blockIdExt mode:# count:# after:mode.7?liteServer.transactionId3 reverse_order:mode.6?true want_proof:mode.5?true = liteServer.BlockTransactionsExt";
  }

  public byte[] getQueryData() {

    int size = BlockIdExt.getSize() + 4 + 4 + 4;
    if ((mode & 128) != 0) {
      size += TransactionId3.getSize();
    }
    if (reverseOrder) {
      mode = mode | 64;
    }
    if (wantProof) {
      mode = mode | 32;
    }

    ByteBuffer buffer = ByteBuffer.allocate(size);
    buffer.order(ByteOrder.LITTLE_ENDIAN);
    buffer.putInt(LIST_BLOCK_TRANSACTIONS_EXT_QUERY);
    buffer.put(id.serialize());
    buffer.putInt(mode);
    buffer.putInt(count);

    if ((mode & 128) != 0) {
      buffer.put(after.serialize());
    }
    return buffer.array();
  }
}
