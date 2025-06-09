package org.ton.ton4j.tl.liteserver.responses;

import java.io.Serializable;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;
import lombok.Builder;
import lombok.Data;
import org.ton.ton4j.utils.Utils;

/**
 * liteServer.dispatchQueueInfo mode:# id:tonNode.blockIdExt account_dispatch_queues:(vector
 * liteServer.accountDispatchQueueInfo) complete:Bool proof:mode.0?bytes =
 * liteServer.DispatchQueueInfo;
 */
@Builder
@Data
public class DispatchQueueInfo implements Serializable, LiteServerAnswer {
  public static final int CONSTRUCTOR_ID = 1561408208;

  private final int mode;
  private final BlockIdExt id;
  private final List<AccountDispatchQueueInfo> accountDispatchQueues;
  private final boolean complete;
  public final byte[] proof;

  public String getProof() {
    if (proof == null) {
      return "";
    }
    return Utils.bytesToHex(proof);
  }

  public static final int constructorId = CONSTRUCTOR_ID;

  public static DispatchQueueInfo deserialize(ByteBuffer buffer) {
    buffer.order(ByteOrder.LITTLE_ENDIAN);
    int mode = buffer.getInt();
    BlockIdExt id = BlockIdExt.deserialize(buffer);

    int vectorLength = buffer.getInt();
    List<AccountDispatchQueueInfo> queues = new ArrayList<>(vectorLength);
    for (int i = 0; i < vectorLength; i++) {
      queues.add(AccountDispatchQueueInfo.deserialize(buffer));
    }

    boolean complete = buffer.getInt() == Utils.TL_TRUE;
    byte[] proof = null;
    if ((mode & 1) != 0) {
      proof = Utils.fromBytes(buffer);
    }

    return DispatchQueueInfo.builder()
        .mode(mode)
        .id(id)
        .accountDispatchQueues(queues)
        .complete(complete)
        .proof(proof)
        .build();
  }

  public byte[] serialize() {
    int size = 4 + BlockIdExt.getSize() + 4;
    for (AccountDispatchQueueInfo queue : accountDispatchQueues) {
      size += queue.serialize().length;
    }
    size += 4;
    if ((mode & 1) != 0) {
      size += Utils.toBytes(proof).length;
    }

    ByteBuffer buffer =
        ByteBuffer.allocate(size)
            .order(ByteOrder.LITTLE_ENDIAN)
            .putInt(mode)
            .put(id.serialize())
            .putInt(accountDispatchQueues.size());

    for (AccountDispatchQueueInfo queue : accountDispatchQueues) {
      buffer.put(queue.serialize());
    }

    buffer.putInt(complete ? Utils.TL_TRUE : Utils.TL_FALSE);

    if ((mode & 1) != 0) {
      buffer.put(Utils.toBytes(proof));
    }

    return buffer.array();
  }

  public static DispatchQueueInfo deserialize(byte[] byteArray) {
    return deserialize(ByteBuffer.wrap(byteArray));
  }
}
