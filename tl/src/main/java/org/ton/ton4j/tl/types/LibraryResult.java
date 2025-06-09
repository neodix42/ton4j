package org.ton.ton4j.tl.types;

import java.io.Serializable;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;
import lombok.Builder;
import lombok.Data;

/** liteServer.libraryResult result:(vector liteServer.libraryEntry) = liteServer.LibraryResult; */
@Builder
@Data
public class LibraryResult implements Serializable, LiteServerAnswer {
  public static final int CONSTRUCTOR_ID = 293255531;

  private final List<LibraryEntry> result;

  public static final int constructorId = CONSTRUCTOR_ID;

  public static LibraryResult deserialize(ByteBuffer buffer) {
    buffer.order(ByteOrder.LITTLE_ENDIAN);
    int vectorLength = buffer.getInt();
    List<LibraryEntry> result = new ArrayList<>(vectorLength);

    for (int i = 0; i < vectorLength; i++) {
      result.add(LibraryEntry.deserialize(buffer));
    }

    return LibraryResult.builder().result(result).build();
  }

  public byte[] serialize() {
    // Calculate total size: vector length + each library entry size
    int size = 4; // Vector length (int)
    for (LibraryEntry entry : result) {
      size += entry.serialize().length;
    }

    ByteBuffer buffer =
        ByteBuffer.allocate(size).order(ByteOrder.LITTLE_ENDIAN).putInt(result.size());

    for (LibraryEntry entry : result) {
      buffer.put(entry.serialize());
    }

    return buffer.array();
  }

  public static LibraryResult deserialize(byte[] byteArray) {
    return deserialize(ByteBuffer.wrap(byteArray));
  }
}
