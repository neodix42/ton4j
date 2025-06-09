package org.ton.ton4j.tl.liteserver.queries;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import lombok.Builder;
import lombok.Getter;
import org.ton.ton4j.tl.liteserver.responses.LiteServerQueryData;

@Builder
@Getter
public class VersionQuery implements LiteServerQueryData {
  public static final int VERSION_QUERY = 590058507;

  public String getQueryName() {
    return "liteServer.getVersion = liteServer.Version";
  }

  public byte[] getQueryData() {
    return ByteBuffer.allocate(serialize().length + 4)
        .order(ByteOrder.LITTLE_ENDIAN)
        .putInt(VERSION_QUERY)
        .put(serialize())
        .array();
  }

  public byte[] serialize() {
    return new byte[0];
  }
}
