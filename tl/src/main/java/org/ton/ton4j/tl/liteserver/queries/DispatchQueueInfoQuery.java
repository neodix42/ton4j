package org.ton.ton4j.tl.liteserver.queries;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import lombok.Builder;
import lombok.Getter;
import org.ton.ton4j.address.Address;
import org.ton.ton4j.tl.liteserver.responses.BlockIdExt;
import org.ton.ton4j.tl.liteserver.responses.LiteServerQueryData;

@Builder
@Getter
public class DispatchQueueInfoQuery implements LiteServerQueryData {
  public static final int DISPATCH_QUEUE_INFO_QUERY = 31878131;

  private final int mode;
  private final BlockIdExt id;
  private final Address afterAddr;
  private final int maxAccounts;
  private final Boolean wantProof;

  public String getQueryName() {
    return "liteServer.getDispatchQueueInfo mode:# id:tonNode.blockIdExt after_addr:mode.1?int256 max_accounts:int want_proof:mode.0?true = liteServer.DispatchQueueInfo";
  }

  public byte[] getQueryData() {
    int size = 4 + BlockIdExt.getSize() + 4; // Mode + BlockIdExt + maxAccounts
    boolean includeAfterAddr = (mode & 2) != 0;
    //    boolean includeWantProof = (mode & 1) != 0;

    if (includeAfterAddr) {
      size += 32; // int256 size
    }
    //    if (includeWantProof) {
    //      size += 4; // Boolean flag size
    //    }

    ByteBuffer buffer =
        ByteBuffer.allocate(size).order(ByteOrder.LITTLE_ENDIAN).putInt(mode).put(id.serialize());

    if (includeAfterAddr) {
      buffer.put(afterAddr.hashPart);
    }

    buffer.putInt(maxAccounts);

    //    if (includeWantProof) {
    //      buffer.putInt(wantProof ? Utils.TL_TRUE : Utils.TL_FALSE);
    //    }

    return ByteBuffer.allocate(buffer.array().length + 4)
        .order(ByteOrder.LITTLE_ENDIAN)
        .putInt(DISPATCH_QUEUE_INFO_QUERY)
        .put(buffer.array())
        .array();
  }
}
