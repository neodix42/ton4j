package org.ton.ton4j.tl.liteserver.queries;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import lombok.Builder;
import lombok.Data;
import org.ton.ton4j.address.Address;
import org.ton.ton4j.tl.liteserver.responses.BlockIdExt;
import org.ton.ton4j.tl.liteserver.responses.LiteServerQueryData;
import org.ton.ton4j.utils.Utils;

@Builder
@Data
public class RunSmcMethodQuery implements LiteServerQueryData {
  public static final int RUN_SMC_METHOD_QUERY = 0;

  private int mode;
  private BlockIdExt id;
  private Address account;
  private long methodId;
  public byte[] params;

  public String getParams() {
    if (params == null) {
      return "";
    }
    return Utils.bytesToHex(params);
  }

  public String getQueryName() {
    return "liteServer.runSmcMethod mode:# id:tonNode.blockIdExt account:liteServer.accountId method_id:long params:bytes = liteServer.RunMethodResult";
  }

  public byte[] getQueryData() {
    byte[] t1 = Utils.toBytes(params);
    ByteBuffer byteBuffer =
        ByteBuffer.allocate(BlockIdExt.getSize() + 4 + 4 + 4 + 32 + 8 + t1.length);
    byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
    byteBuffer.putInt((int) Utils.getQueryCrc32IEEEE(getQueryName()));

    byteBuffer.putInt(mode);
    byteBuffer.put(id.serialize());
    byteBuffer.putInt(account.wc);
    byteBuffer.put(account.hashPart);
    byteBuffer.putLong(methodId);
    byteBuffer.put(Utils.toBytes(params));
    return byteBuffer.array();
  }
}
