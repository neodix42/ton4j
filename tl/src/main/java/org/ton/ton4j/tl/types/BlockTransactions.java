package org.ton.ton4j.tl.types;

import java.io.Serializable;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import lombok.Builder;
import lombok.Data;
import org.ton.ton4j.utils.Utils;

/**
 * liteServer.blockTransactions id:tonNode.blockIdExt req_count:# incomplete:Bool ids:(vector
 * liteServer.transactionId) proof:bytes = liteServer.BlockTransactions;
 */
@Builder
@Data
public class BlockTransactions implements Serializable, LiteServerAnswer {
  BlockIdExt id;
  int req_count;
  boolean incomplete;
  List<TransactionId3> ids;
  byte[] proof;

  public static final int constructorId =
      (int)
          Utils.getQueryCrc32IEEEE(
              "liteServer.blockTransactions id:tonNode.blockIdExt req_count:# incomplete:Bool ids:(vector liteServer.transactionId) proof:bytes = liteServer.BlockTransactions");

  public byte[] serialize() {
    // Calculate total size: BlockIdExt + 4 (req_count) + 1 (incomplete) + 4 (vector length) +
    // (ids.size() * TransactionId3.getSize()) + 4 (proof length) + proof.length
    int totalSize =
        BlockIdExt.getSize()
            + 4
            + 1
            + 4
            + (ids.size() * TransactionId3.getSize())
            + 4
            + proof.length;

    ByteBuffer buffer = ByteBuffer.allocate(totalSize);
    buffer.put(id.serialize());
    buffer.putInt(req_count);
    buffer.put(incomplete ? (byte) 1 : (byte) 0);

    // Write vector of transaction IDs
    buffer.putInt(ids.size());
    for (TransactionId3 id : ids) {
      buffer.put(id.serialize());
    }

    buffer.putInt(proof.length);
    buffer.put(proof);

    return buffer.array();
  }

  public static BlockTransactions deserialize(ByteBuffer byteBuffer) {
    BlockIdExt id = BlockIdExt.deserialize(byteBuffer);
    int req_count = byteBuffer.getInt();
    boolean incomplete = byteBuffer.get() == 1;

    int idsCount = byteBuffer.getInt();
    List<TransactionId3> ids = new ArrayList<>(idsCount);
    for (int i = 0; i < idsCount; i++) {
      ids.add(TransactionId3.deserialize(byteBuffer));
    }

    int proofLen = byteBuffer.getInt();
    byte[] proof = new byte[proofLen];
    byteBuffer.get(proof);

    return BlockTransactions.builder()
        .id(id)
        .req_count(req_count)
        .incomplete(incomplete)
        .ids(ids)
        .proof(proof)
        .build();
  }

  public static BlockTransactions deserialize(byte[] byteArray) {
    return deserialize(ByteBuffer.wrap(byteArray));
  }
}
