package org.ton.ton4j.tl.types.db.root;

import java.io.Serializable;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import lombok.Builder;
import lombok.Data;

/**
 *
 *
 * <pre>
 * ton_api.tl
 * db.root.config celldb_version:int blockdb_version:int = db.root.Config;
 * </pre>
 */
@Builder
@Data
public class Config implements Serializable {

  int celldbVersion;
  int blockdbVersion;

  public static Config deserialize(ByteBuffer buffer) {
    buffer.order(ByteOrder.LITTLE_ENDIAN);
    int celldbVersion = buffer.getInt();
    int blockdbVersion = buffer.getInt();
    
    return Config.builder()
        .celldbVersion(celldbVersion)
        .blockdbVersion(blockdbVersion)
        .build();
  }

  public byte[] serialize() {
    ByteBuffer buffer = ByteBuffer.allocate(8);
    buffer.order(ByteOrder.LITTLE_ENDIAN);
    buffer.putInt(celldbVersion);
    buffer.putInt(blockdbVersion);
    return buffer.array();
  }
}
