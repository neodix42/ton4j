package org.ton.ton4j.tl.queries;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import org.ton.ton4j.utils.Utils;

/** liteServer.query data:bytes = Object id **df068c79** */
public class LiteServerQuery {
  int constructorId;
  byte[] data;

  public static byte[] serialize(String queryName) {
    ByteBuffer byteBuffer =
        ByteBuffer.allocate(4 + (1 + 4 + 3))
            .order(ByteOrder.LITTLE_ENDIAN)
            .putInt(
                (int) Utils.getQueryCrc32IEEEE("liteServer.query data:bytes = Object")) // 798c06df
            .put((byte) 4)
            .putInt((int) Utils.getQueryCrc32IEEEE(queryName));
    System.out.printf(Utils.bytesToHex(byteBuffer.array()));

    return byteBuffer.array();
  }
}
