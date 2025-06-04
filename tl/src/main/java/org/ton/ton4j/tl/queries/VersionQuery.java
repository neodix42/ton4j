package org.ton.ton4j.tl.queries;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import lombok.Builder;
import lombok.Getter;
import org.ton.ton4j.tl.types.LiteServerQueryData;
import org.ton.ton4j.utils.Utils;

@Builder
@Getter
public class VersionQuery implements LiteServerQueryData {
  public static final int VERSION_QUERY = 590058507;

  public String getQueryName() {
    return "liteServer.getVersion = liteServer.Version";
  }

  public byte[] getQueryData() {
    int bodyLenPad4 = Utils.pad4(serialize().length + 1);
    //    int queryLenPad8 = Utils.pad8(bodyLenPad4 + 1);

    ByteBuffer buffer = ByteBuffer.allocate(bodyLenPad4);
    buffer.order(ByteOrder.LITTLE_ENDIAN);
    //    buffer.put((byte) bodyLenPad4);
    buffer.putInt(VERSION_QUERY);
    buffer.put(serialize()); // important
    return buffer.array();
  }

  public byte[] serialize() {
    return new byte[0];
  }
}
