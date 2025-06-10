package org.ton.ton4j.tl.liteserver.queries;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import lombok.Builder;
import lombok.Data;
import org.ton.ton4j.utils.Utils;

/** tcp.ping random_id:long = tcp.Pong, id **9a2b084d** */
@Builder
@Data
public class PingQuery {

  public static byte[] serialize(long randomId) {
    ByteBuffer byteBuffer =
        ByteBuffer.allocate(4 + 8)
            .order(ByteOrder.LITTLE_ENDIAN)
            .putInt((int) Utils.getQueryCrc32IEEEE("tcp.ping random_id:long = tcp.Pong"))
            .putLong(randomId);

    return byteBuffer.array();
  }
}
