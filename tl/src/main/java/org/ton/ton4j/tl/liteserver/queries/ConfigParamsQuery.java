package org.ton.ton4j.tl.liteserver.queries;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import lombok.Builder;
import lombok.Data;
import org.ton.ton4j.tl.liteserver.responses.BlockIdExt;
import org.ton.ton4j.tl.liteserver.responses.LiteServerQueryData;

@Builder
@Data
public class ConfigParamsQuery implements LiteServerQueryData {
  public static final int GET_CONFIG_PARAMS_QUERY = 705764377; // CRC32 of query schema

  private final int mode;
  private final BlockIdExt id;
  private final int[] paramList;

  public String getQueryName() {
    return "liteServer.getConfigParams mode:# id:tonNode.blockIdExt param_list:(vector int) = liteServer.ConfigInfo";
  }

  public byte[] getQueryData() {
    // Calculate total size: mode(int) + BlockIdExt size + paramList length(int) + each param(int)
    int size = 4 + BlockIdExt.getSize() + 4 + (paramList.length * 4);
    ByteBuffer buffer =
        ByteBuffer.allocate(size).order(ByteOrder.LITTLE_ENDIAN).putInt(mode).put(id.serialize());

    // Write vector length and parameters
    buffer.putInt(paramList.length);
    for (int param : paramList) {
      buffer.putInt(param);
    }

    return ByteBuffer.allocate(buffer.array().length + 4)
        .order(ByteOrder.LITTLE_ENDIAN)
        .putInt(GET_CONFIG_PARAMS_QUERY)
        .put(buffer.array())
        .array();
  }
}
