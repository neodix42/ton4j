package org.ton.ton4j.tl.liteserver.queries;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.List;
import lombok.Builder;
import lombok.Data;
import org.ton.ton4j.tl.liteserver.responses.LiteServerQueryData;

/** liteServer.getLibraries library_list:(vector int256) = liteServer.LibraryResult; */
@Builder
@Data
public class LibrariesQuery implements LiteServerQueryData {
  public static final int GET_LIBRARIES_QUERY = -786254238; // Placeholder CRC32

  private final List<byte[]> libraryList;

  public String getQueryName() {
    return "liteServer.getLibraries library_list:(vector int256) = liteServer.LibraryResult";
  }

  public static final int constructorId = GET_LIBRARIES_QUERY;

  public byte[] getQueryData() {
    // Calculate total size: vector length + each library hash (32 bytes)
    int size = 4; // Vector length (int)
    for (byte[] lib : libraryList) {
      size += 32;
    }

    ByteBuffer buffer =
        ByteBuffer.allocate(size).order(ByteOrder.LITTLE_ENDIAN).putInt(libraryList.size());

    for (byte[] lib : libraryList) {
      buffer.put(lib);
    }

    // Prepend TL ID
    return ByteBuffer.allocate(buffer.array().length + 4)
        .order(ByteOrder.LITTLE_ENDIAN)
        .putInt(GET_LIBRARIES_QUERY)
        .put(buffer.array())
        .array();
  }
}
