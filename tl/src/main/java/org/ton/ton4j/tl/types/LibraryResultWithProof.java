package org.ton.ton4j.tl.types;

import java.io.Serializable;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;
import lombok.Builder;
import lombok.Data;
import org.ton.ton4j.utils.Utils;

/**
 * liteServer.libraryResultWithProof id:tonNode.blockIdExt mode:# result:(vector
 * liteServer.libraryEntry) state_proof:bytes data_proof:bytes = liteServer.LibraryResultWithProof;
 */
@Builder
@Data
public class LibraryResultWithProof implements Serializable, LiteServerAnswer {
  public static final int CONSTRUCTOR_ID = 279521215;

  private final BlockIdExt id;
  private final int mode;
  private final List<LibraryEntry> libraries;
  public final byte[] stateProof;
  public final byte[] dataProof;

  public String getStateProof() {
    if (stateProof == null) {
      return "";
    }
    return Utils.bytesToHex(stateProof);
  }

  public String getDataProof() {
    if (dataProof == null) {
      return "";
    }
    return Utils.bytesToHex(dataProof);
  }

  public static final int constructorId = CONSTRUCTOR_ID;

  public static LibraryResultWithProof deserialize(ByteBuffer buffer) {
    buffer.order(ByteOrder.LITTLE_ENDIAN);
    BlockIdExt id = BlockIdExt.deserialize(buffer);
    int mode = buffer.getInt();

    int vectorLength = buffer.getInt();
    List<LibraryEntry> result = new ArrayList<>(vectorLength);
    for (int i = 0; i < vectorLength; i++) {
      result.add(LibraryEntry.deserialize(buffer));
    }

    byte[] stateProof = Utils.fromBytes(buffer);
    byte[] dataProof = Utils.fromBytes(buffer);

    return LibraryResultWithProof.builder()
        .id(id)
        .mode(mode)
        .libraries(result)
        .stateProof(stateProof)
        .dataProof(dataProof)
        .build();
  }

  public byte[] serialize() {
    int size = BlockIdExt.getSize() + 4; // BlockIdExt size + mode

    // Vector size
    size += 4;
    for (LibraryEntry entry : libraries) {
      size += entry.serialize().length;
    }

    // Proofs
    size += Utils.toBytes(stateProof).length;
    size += Utils.toBytes(dataProof).length;

    ByteBuffer buffer =
        ByteBuffer.allocate(size)
            .order(ByteOrder.LITTLE_ENDIAN)
            .put(id.serialize())
            .putInt(mode)
            .putInt(libraries.size());

    for (LibraryEntry entry : libraries) {
      buffer.put(entry.serialize());
    }

    buffer.put(Utils.toBytes(stateProof));
    buffer.put(Utils.toBytes(dataProof));

    return buffer.array();
  }

  public static LibraryResultWithProof deserialize(byte[] byteArray) {
    return deserialize(ByteBuffer.wrap(byteArray));
  }
}
