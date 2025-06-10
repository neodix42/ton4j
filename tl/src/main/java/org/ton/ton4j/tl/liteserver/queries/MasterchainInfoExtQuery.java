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
    ByteBuffer buffer = ByteBuffer.allocate(4 + 4);
    buffer.order(ByteOrder.LITTLE_ENDIAN);
    buffer.putInt(MASTERCHAIN_INFO_EXT_QUERY);
    buffer.putInt(mode);
    return buffer.array();
  }
}
