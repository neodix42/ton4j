package org.ton.ton4j.tl.types.db.state;

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
 * db.state.dbVersion version:int = db.state.DbVersion;
 * </pre>
 */
@Builder
@Data
public class DbVersion implements Serializable {

  int version;

  public static DbVersion deserialize(ByteBuffer buffer) {
    buffer.order(ByteOrder.LITTLE_ENDIAN);
    int version = buffer.getInt();
    
    return DbVersion.builder()
        .version(version)
        .build();
  }

  public byte[] serialize() {
    ByteBuffer buffer = ByteBuffer.allocate(4);
    buffer.order(ByteOrder.LITTLE_ENDIAN);
    buffer.putInt(version);
    return buffer.array();
  }
}
