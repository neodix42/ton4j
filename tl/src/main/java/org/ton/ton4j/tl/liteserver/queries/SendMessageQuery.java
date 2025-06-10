package org.ton.ton4j.tl.liteserver.queries;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import lombok.Builder;
import lombok.Data;
import org.ton.ton4j.tl.liteserver.responses.LiteServerQueryData;
import org.ton.ton4j.utils.Utils;

@Builder
@Data
public class SendMessageQuery implements LiteServerQueryData {
  public static final int SEND_MESSAGE_QUERY = 0;

  public byte[] body;

  public String getBody() {
    if (body == null) {
      return "";
    }
    return Utils.bytesToHex(body);
  }

  public String getQueryName() {
    return "liteServer.sendMessage body:bytes = liteServer.SendMsgStatus";
  }

  public byte[] getQueryData() {
    byte[] t1 = Utils.toBytes(body);
    ByteBuffer buffer = ByteBuffer.allocate(4 + t1.length);
    buffer.order(ByteOrder.LITTLE_ENDIAN);
    buffer.putInt((int) Utils.getQueryCrc32IEEEE(getQueryName()));
    buffer.put(t1);
    return buffer.array();
  }
}
