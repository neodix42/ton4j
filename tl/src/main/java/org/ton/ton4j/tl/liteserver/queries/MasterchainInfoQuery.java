package org.ton.ton4j.tl.liteserver.queries;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import lombok.Builder;
import lombok.Getter;
import org.ton.ton4j.tl.liteserver.responses.LiteServerQueryData;

/** liteServer.getMasterchainInfo = liteServer.MasterchainInfo */
@Builder
@Getter
public class MasterchainInfoQuery implements LiteServerQueryData {
  public static final int MASTERCHAIN_INFO_QUERY = -1984567762; // 2ee6b589

  public String getQueryName() {
    return "liteServer.getMasterchainInfo = liteServer.MasterchainInfo";
  }

  public byte[] getQueryData() {
    int len = serialize().length + 4;
    ByteBuffer buffer = ByteBuffer.allocate(len);
    buffer.order(ByteOrder.LITTLE_ENDIAN);
    buffer.putInt(MASTERCHAIN_INFO_QUERY);
    buffer.put(serialize());
    return buffer.array();
  }

  public byte[] serialize() {
    return new byte[0];
  }
}
