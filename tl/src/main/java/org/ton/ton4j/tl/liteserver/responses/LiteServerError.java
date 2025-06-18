package org.ton.ton4j.tl.liteserver.responses;

import java.io.Serializable;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import lombok.Builder;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;

/** liteServer.error code:int message:string = liteServer.Error; */
@Builder
@Data
public class LiteServerError implements Serializable, LiteServerAnswer {
  public static final int LITE_SERVER_ERROR_ANSWER = -1146494648;

  int code;
  String message;

  public static final int constructorId = LITE_SERVER_ERROR_ANSWER;

  public byte[] serialize() {
    ByteBuffer buffer = ByteBuffer.allocate(4 + message.length());
    buffer.putInt(code);
    buffer.put(message.getBytes());
    return buffer.array();
  }

  public static LiteServerError deserialize(ByteBuffer byteBuffer) {
    byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
    int code = byteBuffer.getInt();
    String message = new String(byteBuffer.array(), byteBuffer.position(), byteBuffer.remaining());
    if (StringUtils.isNotEmpty(message)) {
      message = message.substring(1, message.length() - 1).trim();
    }
    return LiteServerError.builder().code(code).message(message).build();
  }

  public static LiteServerError deserialize(byte[] byteArray) {
    return deserialize(ByteBuffer.wrap(byteArray));
  }
}
