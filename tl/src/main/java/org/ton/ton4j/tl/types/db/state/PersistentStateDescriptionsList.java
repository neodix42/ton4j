package org.ton.ton4j.tl.types.db.state;

import java.io.Serializable;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;
import lombok.Builder;
import lombok.Data;

/**
 *
 *
 * <pre>
 * ton_api.tl
 * db.state.persistentStateDescriptionsList list:(vector db.state.persistentStateDescriptionHeader) = db.state.PersistentStateDescriptionsList;
 * </pre>
 */
@Builder
@Data
public class PersistentStateDescriptionsList implements Serializable {
  int magic;
  List<PersistentStateDescriptionHeader> list;

  public static PersistentStateDescriptionsList deserialize(ByteBuffer buffer) {
    buffer.order(ByteOrder.LITTLE_ENDIAN);

    // Read list vector
    int magic = buffer.getInt();
    int listCount = buffer.getInt();
    List<PersistentStateDescriptionHeader> list = new ArrayList<>(listCount);
    for (int i = 0; i < listCount; i++) {
      list.add(PersistentStateDescriptionHeader.deserialize(buffer));
    }

    return PersistentStateDescriptionsList.builder().magic(magic).list(list).build();
  }

  public byte[] serialize() {
    // Calculate buffer size
    int size = 8; // 4 bytes for vector size
    for (PersistentStateDescriptionHeader header : list) {
      size += header.serialize().length;
    }

    ByteBuffer buffer = ByteBuffer.allocate(size);
    buffer.order(ByteOrder.LITTLE_ENDIAN);
    buffer.putInt(-1485326599);
    // Write list vector
    buffer.putInt(list.size());
    for (PersistentStateDescriptionHeader header : list) {
      buffer.put(header.serialize());
    }

    return buffer.array();
  }
}
