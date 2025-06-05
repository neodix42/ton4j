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
  public static final int BLOCK_TRANSACTIONS_ANSWER = 0;

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
    boolean incomplete = byteBuffer.get() == 1;

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
