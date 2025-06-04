package org.ton.ton4j.tl.types;

import java.io.Serializable;
import java.nio.ByteBuffer;
import lombok.Builder;
import lombok.Data;
import org.ton.ton4j.utils.Utils;

/**
 * liteServer.blockTransactionsExt id:tonNode.blockIdExt req_count:# incomplete:Bool
 * transactions:bytes proof:bytes = liteServer.BlockTransactionsExt;
 */
@Builder
@Data
public class BlockTransactionsExt implements Serializable, LiteServerAnswer {
  BlockIdExt id;
  int req_count;
  boolean incomplete;
  byte[] transactions;
  byte[] proof;

  public static final int constructorId =
      (int)
          Utils.getQueryCrc32IEEEE(
              "liteServer.blockTransactionsExt id:tonNode.blockIdExt req_count:# incomplete:Bool transactions:bytes proof:bytes = liteServer.BlockTransactionsExt");

  public byte[] serialize() {
    int totalSize = BlockIdExt.getSize() + 4 + 1 + 4 + transactions.length + 4 + proof.length;

    ByteBuffer buffer = ByteBuffer.allocate(totalSize);
    buffer.put(id.serialize());
    buffer.putInt(req_count);
    buffer.put(incomplete ? (byte) 1 : (byte) 0);

    buffer.putInt(transactions.length);
    buffer.put(transactions);

    buffer.putInt(proof.length);
    buffer.put(proof);

    return buffer.array();
  }

  public static BlockTransactionsExt deserialize(ByteBuffer byteBuffer) {
    BlockIdExt id = BlockIdExt.deserialize(byteBuffer);
    int req_count = byteBuffer.getInt();
    boolean incomplete = byteBuffer.get() == 1;

    int transactionsLen = byteBuffer.getInt();
    byte[] transactions = new byte[transactionsLen];
    byteBuffer.get(transactions);

    int proofLen = byteBuffer.getInt();
    byte[] proof = new byte[proofLen];
    byteBuffer.get(proof);

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
