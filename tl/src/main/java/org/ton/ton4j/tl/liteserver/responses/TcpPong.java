package org.ton.ton4j.tl.liteserver.responses;

import java.io.Serializable;
import java.nio.ByteBuffer;
import lombok.Builder;
import lombok.Data;

/** tcp.pong random_id:long = tcp.Pong, id **9a2b084d** */
@Builder
@Data
public class TcpPong implements Serializable, LiteServerAnswer {
  public static final int TCP_PONG_ANSWER = -597034237;

  long randomId;
  public static final int constructorId = TCP_PONG_ANSWER;

  public static TcpPong deserialize(byte[] payload) {

    return TcpPong.builder()
        .randomId(Long.reverseBytes(ByteBuffer.wrap(payload).getLong()))
        .build();
  }
}
