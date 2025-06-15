package org.ton.ton4j.tl.liteserver.queries;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import lombok.Builder;
import lombok.Data;
import org.ton.ton4j.tl.liteserver.responses.LiteServerQueryData;

@Builder
@Data
public class MasterchainInfoExtQuery implements LiteServerQueryData {
  public static final int MASTERCHAIN_INFO_EXT_QUERY = 1889956319;

  private int mode;

  public String getQueryName() {
    return "liteServer.getMasterchainInfoExt mode:# = liteServer.MasterchainInfoExt";
  }

  public byte[] getQueryData() {
    return ByteBuffer.allocate(4 + 4)
        .order(ByteOrder.LITTLE_ENDIAN)
        .putInt(MASTERCHAIN_INFO_EXT_QUERY)
        .putInt(mode)
        .array();
  }
}
