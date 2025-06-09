package org.ton.ton4j.tl.liteserver.queries;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import org.ton.ton4j.utils.Utils;

/** adnl.message.query query_id:int256 query:bytes = adnl.Message, id **7af98bb4** */
public class AdnlMessageQuery {
  public static final int ADNL_MESSAGE_QUERY = -1265895046; // 0x7af98bb4

  public static byte[] serialize(byte[] queryId, byte[] queryPayload) {
    byte[] temp = Utils.toBytes(queryPayload);

    ByteBuffer byteBuffer =
        ByteBuffer.allocate(4 + 32 + temp.length)
            .order(ByteOrder.LITTLE_ENDIAN)
            .putInt(ADNL_MESSAGE_QUERY)
            .put(queryId)
            .put(temp);

    return byteBuffer.array();
  }
}
