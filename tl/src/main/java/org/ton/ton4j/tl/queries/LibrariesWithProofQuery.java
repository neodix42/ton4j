package org.ton.ton4j.tl.queries;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.List;
import lombok.Builder;
import lombok.Data;
import org.ton.ton4j.tl.types.BlockIdExt;
import org.ton.ton4j.tl.types.LiteServerQueryData;

@Builder
@Data
public class LibrariesWithProofQuery implements LiteServerQueryData {
  public static final int GET_LIBRARIES_WITH_PROOF_QUERY = -646540355;

  private final BlockIdExt id;
  private final int mode;
  private final List<byte[]> libraryList;

  public String getQueryName() {
    return "liteServer.getLibrariesWithProof id:tonNode.blockIdExt mode:# library_list:(vector int256) = liteServer.LibraryResultWithProof";
  }

  public byte[] getQueryData() {
    int size = BlockIdExt.getSize() + 4 + 4 + (libraryList.size() * 32);

    ByteBuffer buffer =
        ByteBuffer.allocate(size)
            .order(ByteOrder.LITTLE_ENDIAN)
            .put(id.serialize())
            .putInt(mode)
            .putInt(libraryList.size());

    for (byte[] lib : libraryList) {
      buffer.put(lib);
    }

    return ByteBuffer.allocate(buffer.array().length + 4)
        .order(ByteOrder.LITTLE_ENDIAN)
        .putInt(GET_LIBRARIES_WITH_PROOF_QUERY)
        .put(buffer.array())
        .array();
  }
}
