package org.ton.ton4j.tl.queries;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import org.ton.ton4j.utils.Utils;

/** adnl.message.query query_id:int256 query:bytes = adnl.Message, id **7af98bb4** */
public class AdnlMessageQuery {

  public static byte[] serialize(byte[] queryId, byte[] queryPayload) {
    ByteBuffer byteBuffer =
        ByteBuffer.allocate(4 + 32 + (1 + 12 + 3))
            .order(ByteOrder.LITTLE_ENDIAN)
            .putInt(
                (int)
                    Utils.getQueryCrc32IEEEE(
                        "adnl.message.query query_id:int256 query:bytes = adnl.Message")) // 798c06df
            .put(queryId)
            .put((byte) 12)
            .put(queryPayload);
    System.out.printf(Utils.bytesToHex(byteBuffer.array()));

    return byteBuffer.array();
  }
}
