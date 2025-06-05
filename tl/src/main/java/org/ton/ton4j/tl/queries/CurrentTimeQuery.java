package org.ton.ton4j.tl.queries;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import lombok.Builder;
import lombok.Getter;
import org.ton.ton4j.tl.types.LiteServerQueryData;

@Builder
@Getter
public class CurrentTimeQuery implements LiteServerQueryData {
  public static final int CURRENT_TIME_QUERY = 380459572;

  public String getQueryName() {
    return "liteServer.getTime = liteServer.CurrentTime";
  }

  public byte[] getQueryData() {
    return ByteBuffer.allocate(serialize().length + 4)
        .order(ByteOrder.LITTLE_ENDIAN)
        .putInt(CURRENT_TIME_QUERY)
        .put(serialize())
        .array();
  }

  public byte[] serialize() {
    return new byte[0];
  }
}
