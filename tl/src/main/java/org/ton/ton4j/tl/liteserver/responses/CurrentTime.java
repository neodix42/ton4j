package org.ton.ton4j.tl.liteserver.responses;

import java.io.Serializable;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CurrentTime implements Serializable, LiteServerAnswer {
  public static final int CURRENT_TIME_ANSWER = -380436467;

  private int now;

  public static final int constructorId = CURRENT_TIME_ANSWER;

  public static CurrentTime deserialize(ByteBuffer buffer) {
    buffer.order(ByteOrder.LITTLE_ENDIAN);
    return CurrentTime.builder().now(buffer.getInt()).build();
  }

  public static CurrentTime deserialize(byte[] data) {
    return deserialize(ByteBuffer.wrap(data));
  }
}
