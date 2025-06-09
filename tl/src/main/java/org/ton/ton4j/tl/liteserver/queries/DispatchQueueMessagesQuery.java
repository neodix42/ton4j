package org.ton.ton4j.tl.liteserver.queries;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import lombok.Builder;
import lombok.Data;
import org.ton.ton4j.address.Address;
import org.ton.ton4j.tl.liteserver.responses.BlockIdExt;
import org.ton.ton4j.tl.liteserver.responses.LiteServerQueryData;

/**
 * liteServer.getDispatchQueueMessages mode:# id:tonNode.blockIdExt addr:int256 after_lt:long
 * max_messages:int want_proof:mode.0?true one_account:mode.1?true messages_boc:mode.2?true =
 * liteServer.DispatchQueueMessages;
 */
@Builder
@Data
public class DispatchQueueMessagesQuery implements LiteServerQueryData {
  public static final int GET_DISPATCH_QUEUE_MESSAGES_QUERY = -1141021639;

  private final int mode;
  private final BlockIdExt id;
  private final Address addr;
  private final long afterLt;
  private final int maxMessages;
  private final boolean wantProof;
  private final boolean oneAccount;
  private final boolean messagesBoc;

  public String getQueryName() {
    return "liteServer.getDispatchQueueMessages mode:# id:tonNode.blockIdExt addr:int256 after_lt:long max_messages:int want_proof:mode.0?true one_account:mode.1?true messages_boc:mode极.2?true = liteServer.DispatchQueueMessages";
  }

  public byte[] getQueryData() {
    int size =
        4
            + BlockIdExt.getSize()
            + 32
            + 8
            + 4; // Mode + BlockIdExt + addr (int256) + afterLt (long) + maxMessages (int)

    //    if ((mode & 1) != 0) size += 1; // wantProof (boolean)
    //    if ((mode & 2) != 0) size += 1; // oneAccount (boolean)
    //    if ((mode & 4) != 0) size += 1; // messagesBoc (boolean)
    //
    ByteBuffer buffer =
        ByteBuffer.allocate(size).order(ByteOrder.LITTLE_ENDIAN).putInt(mode).put(id.serialize());

    buffer.put(addr.hashPart);

    buffer.putLong(afterLt).putInt(maxMessages);

    //    if ((mode & 1) != 0) buffer.put((byte) (wantProof ? 1 : 0));
    //    if ((mode & 2) != 0) buffer.put((byte) (oneAccount ? 1 : 0));
    //    if ((mode & 4) != 0) buffer.put((byte) (messagesBoc ? 1 : 0));

    return ByteBuffer.allocate(buffer.array().length + 4)
        .order(ByteOrder.LITTLE_ENDIAN)
        .putInt(GET_DISPATCH_QUEUE_MESSAGES_QUERY)
        .put(buffer.array())
        .array();
  }
}
