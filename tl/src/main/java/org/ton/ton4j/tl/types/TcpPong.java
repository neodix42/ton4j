package org.ton.ton4j.tl.types;

import java.nio.ByteBuffer;
import lombok.Builder;
import lombok.Data;
import org.ton.ton4j.utils.Utils;

/** tcp.pong random_id:long = tcp.Pong, id **9a2b084d** */
@Builder
@Data
public class TcpPong {
  long randomId;
  public static final int constructorId =
      (int) Utils.getQueryCrc32IEEEE("tcp.pong random_id:long = tcp.Pong");

  public static TcpPong deserialize(byte[] payload) {
    return TcpPong.builder().randomId(Long.reverseBytes(ByteBuffer.wrap(payload).getLong())).build();
  }
}
