package org.ton.ton4j.tl.types;

import java.io.Serializable;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import lombok.Builder;
import lombok.Data;
import org.ton.ton4j.cell.Cell;
import org.ton.ton4j.utils.Utils;

@Builder
@Data
public class LibraryEntry implements Serializable {
  public final byte[] hash;
  public final byte[] data;

  public String getHash() {
    if (hash == null) {
      return "";
    }
    return Utils.bytesToHex(hash);
  }

  public String getData() {
    if (data == null) {
      return "";
    }
    return Utils.bytesToHex(data);
  }

  public Cell getDataParsed() {
    if (data == null) {
      return null;
    }
    return Cell.fromBoc(data);
  }

  public static LibraryEntry deserialize(ByteBuffer buffer) {
    buffer.order(ByteOrder.LITTLE_ENDIAN);
    return LibraryEntry.builder()
        .hash(Utils.read(buffer, 32))
        .data(Utils.fromBytes(buffer))
        .build();
  }

  public byte[] serialize() {
    int len = Utils.toBytes(data).length;
    return ByteBuffer.allocate(hash.length + len)
        .order(ByteOrder.LITTLE_ENDIAN)
        .put(hash)
        .put(Utils.toBytes(data))
        .array();
  }
}
