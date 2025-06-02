package org.ton.ton4j.tl.types;

import java.io.Serializable;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import lombok.Builder;
import lombok.Data;
import org.ton.ton4j.utils.Utils;

@Data
@Builder
public class CurrentTime implements Serializable, LiteServerAnswer {
  private int now;

  public static final int constructorId =
      (int) Utils.getQueryCrc32IEEEE("liteServer.currentTime now:int = liteServer.CurrentTime");

  public static CurrentTime deserialize(ByteBuffer buffer) {
    buffer.order(ByteOrder.LITTLE_ENDIAN);
    return CurrentTime.builder().now(buffer.getInt()).build();
  }

  public static CurrentTime deserialize(byte[] data) {
    return deserialize(ByteBuffer.wrap(data));
  }
}
