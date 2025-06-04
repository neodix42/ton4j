package org.ton.ton4j.tl.queries;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import org.ton.ton4j.tl.types.LiteServerQueryData;
import org.ton.ton4j.utils.Utils;

/** liteServer.query data:bytes = Object id **df068c79** */
public class LiteServerQuery {

  public static byte[] serialize(String queryName) {
    ByteBuffer byteBuffer =
        ByteBuffer.allocate(4 + (1 + 4 + 3))
            .order(ByteOrder.LITTLE_ENDIAN)
            .putInt(
                (int) Utils.getQueryCrc32IEEEE("liteServer.query data:bytes = Object")) // df068c79
            .put((byte) 4)
            .putInt((int) Utils.getQueryCrc32IEEEE(queryName));
    //    System.out.printf(Utils.bytesToHex(byteBuffer.array()));

    return byteBuffer.array();
  }

  //
  //  public static byte[] serialize(String queryName, byte[] data) {
  //    ByteBuffer byteBuffer =
  //        ByteBuffer.allocate(4 + (1 + 4 + 3) + data.length)
  //            .order(ByteOrder.LITTLE_ENDIAN)
  //            .putInt(
  //                (int) Utils.getQueryCrc32IEEEE("liteServer.query data:bytes = Object")) //
  // df068c79
  //            .put((byte) (4 + data.length))
  //            .putInt((int) Utils.getQueryCrc32IEEEE(queryName))
  //            .put(data);
  //
  //    return byteBuffer.array();
  //  }

  public static byte[] pack(LiteServerQueryData data) {
    int LITE_SERVER_QUERY = 2039219935; // liteServer.query data:bytes = Object, df068c79
    byte[] temp = Utils.toBytes(data.getQueryData());
    ByteBuffer byteBuffer =
        ByteBuffer.allocate(4 + temp.length)
            .order(ByteOrder.LITTLE_ENDIAN)
            .putInt(LITE_SERVER_QUERY)
            .put(temp);

    return byteBuffer.array();
  }
}
