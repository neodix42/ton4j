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
  public final int TRANSACTION_LIST_ANSWER = 0;

  List<BlockIdExt> ids;
  byte[] transactions;

  public static final int constructorId =
      (int)
          Utils.getQueryCrc32IEEEE(
              "liteServer.transactionList ids:(vector tonNode.blockIdExt) transactions:bytes = liteServer.TransactionList");

  public byte[] serialize() {
    // Calculate total size: 4 bytes for vector length + size of each BlockIdExt + transactions
    int totalSize = 4;
    for (BlockIdExt id : ids) {
      totalSize += BlockIdExt.getSize();
    }
    totalSize += 4 + transactions.length;

    ByteBuffer buffer = ByteBuffer.allocate(totalSize);

    // Write vector of ids
    buffer.putInt(ids.size());
    for (BlockIdExt id : ids) {
      buffer.put(id.serialize());
    }

    // Write transactions
    buffer.put(Utils.toBytes(transactions));

    return buffer.array();
  }

  public static TransactionList deserialize(ByteBuffer byteBuffer) {
    // Read vector of ids
    int idsCount = byteBuffer.getInt();
    List<BlockIdExt> ids = new ArrayList<>(idsCount);
    for (int i = 0; i < idsCount; i++) {
      ids.add(BlockIdExt.deserialize(byteBuffer));
    }

    // Read transactions
    byte[] transactions = Utils.fromBytes(byteBuffer);

    return TransactionList.builder().ids(ids).transactions(transactions).build();
  }

  public static TransactionList deserialize(byte[] byteArray) {
    return deserialize(ByteBuffer.wrap(byteArray));
  }
}
