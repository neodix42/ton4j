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
  public static final int BLOCK_TRANSACTIONS_ANSWER = -1114854101;

  BlockIdExt id;
  int req_count;
  boolean incomplete;
  List<TransactionId> ids;
  public byte[] proof;

  public String getProof() {
    return Utils.bytesToHex(proof);
  }

  public static final int constructorId = BLOCK_TRANSACTIONS_ANSWER;

  public byte[] serialize() {
    byte[] t1 = Utils.toBytes(proof);

    ByteBuffer buffer =
        ByteBuffer.allocate(BlockIdExt.getSize() + 4 + 1 + ids.size() + 16 + t1.length);
    buffer.put(id.serialize());
    buffer.putInt(req_count);
    buffer.put(incomplete ? (byte) 1 : (byte) 0);
    buffer.putInt(ids.size());
    for (TransactionId id : ids) {
      buffer.put(id.serialize());
    }
    buffer.put(t1);

    return buffer.array();
  }

  public static BlockTransactions deserialize(ByteBuffer byteBuffer) {
    BlockIdExt id = BlockIdExt.deserialize(byteBuffer);
    int req_count = byteBuffer.getInt();
    boolean incomplete = byteBuffer.getInt() == 0; // why 4 bytes?

    int idsCount = byteBuffer.getInt();
    List<TransactionId> ids = new ArrayList<>(idsCount);
    for (int i = 0; i < idsCount; i++) {
      ids.add(TransactionId.deserialize(byteBuffer));
    }

    byte[] proof = Utils.fromBytes(byteBuffer);

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
