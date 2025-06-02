package org.ton.ton4j.tl.types;

import java.io.Serializable;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import lombok.Builder;
import lombok.Data;
import org.ton.ton4j.utils.Utils;

@Data
@Builder
public class Version implements Serializable, LiteServerAnswer {
  private int mode;
  private int version;
  private long capabilities;
  private int now;

  public static final int constructorId =
      (int)
          Utils.getQueryCrc32IEEEE(
              "liteServer.version mode:# version:int capabilities:long now:int = liteServer.Version");

  public static Version deserialize(ByteBuffer buffer) {
    buffer.order(ByteOrder.LITTLE_ENDIAN);
    return Version.builder()
        .mode(buffer.getInt())
        .version(buffer.getInt())
        .capabilities(buffer.getLong())
        .now(buffer.getInt())
        .build();
  }

  public static Version deserialize(byte[] data) {
    return deserialize(ByteBuffer.wrap(data));
  }
}
