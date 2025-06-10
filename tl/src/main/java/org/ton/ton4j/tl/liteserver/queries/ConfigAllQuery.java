package org.ton.ton4j.tl.liteserver.queries;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import lombok.Builder;
import lombok.Data;
import org.ton.ton4j.tl.liteserver.responses.BlockIdExt;
import org.ton.ton4j.tl.liteserver.responses.LiteServerQueryData;

@Builder
@Data
public class ConfigAllQuery implements LiteServerQueryData {
  public static final int CONFIG_ALL_QUERY = -1860491593; // 0xb7261b91
  BlockIdExt id;
  int mode;

  public String getQueryName() {
    return "liteServer.getConfigAll mode:# id:tonNode.blockIdExt = liteServer.ConfigInfo";
  }

  public byte[] getQueryData() {
    int len = serialize().length + 4;
    ByteBuffer buffer = ByteBuffer.allocate(len);
    buffer.order(ByteOrder.LITTLE_ENDIAN);
    buffer.putInt(CONFIG_ALL_QUERY);
    buffer.put(serialize());
    return buffer.array();
  }

  public byte[] serialize() {
    ByteBuffer buffer = ByteBuffer.allocate(BlockIdExt.getSize() + 4);
    buffer.putInt(mode);
    buffer.put(id.serialize());
    return buffer.array();
  }
}
