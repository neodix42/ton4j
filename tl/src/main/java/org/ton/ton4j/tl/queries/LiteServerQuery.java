package org.ton.ton4j.tl.queries;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import org.ton.ton4j.tl.types.LiteServerQueryData;
import org.ton.ton4j.utils.Utils;

/** liteServer.query data:bytes = Object id **df068c79** */
public class LiteServerQuery {
  public static final int LITE_SERVER_QUERY = 2039219935; // 0xdf068c79;

  public static byte[] serialize(String queryName) {
    ByteBuffer byteBuffer =
        ByteBuffer.allocate(4 + (1 + 4 + 3))
            .order(ByteOrder.LITTLE_ENDIAN)
            .putInt(LITE_SERVER_QUERY)
            .put((byte) 4)
            .putInt((int) Utils.getQueryCrc32IEEEE(queryName));

    return byteBuffer.array();
  }

  public static byte[] pack(LiteServerQueryData data) {
    byte[] temp = Utils.toBytes(data.getQueryData());
    ByteBuffer byteBuffer =
        ByteBuffer.allocate(4 + temp.length)
            .order(ByteOrder.LITTLE_ENDIAN)
            .putInt(LITE_SERVER_QUERY)
            .put(temp);

    return byteBuffer.array();
  }
}
