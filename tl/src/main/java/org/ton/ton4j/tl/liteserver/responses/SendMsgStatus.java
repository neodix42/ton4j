package org.ton.ton4j.tl.liteserver.responses;

import java.io.Serializable;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SendMsgStatus implements Serializable, LiteServerAnswer {
  public static final int SEND_MSG_STATUS_ANSWER = 961602967;

  private int status;

  public static final int constructorId = SEND_MSG_STATUS_ANSWER;

  public static SendMsgStatus deserialize(ByteBuffer buffer) {
    buffer.order(ByteOrder.LITTLE_ENDIAN);
    return SendMsgStatus.builder().status(buffer.getInt()).build();
  }

  public static SendMsgStatus deserialize(byte[] data) {
    return deserialize(ByteBuffer.wrap(data));
  }
}
