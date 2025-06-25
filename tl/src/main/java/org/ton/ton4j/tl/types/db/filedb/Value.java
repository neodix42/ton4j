package org.ton.ton4j.tl.types.db.filedb;

import java.io.Serializable;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import lombok.Builder;
import lombok.Data;
import org.ton.ton4j.utils.Utils;

/**
 *
 *
 * <pre>
 * ton_api.tl
 * db.filedb.value key:db.filedb.Key prev:int256 next:int256 file_hash:int256 = db.filedb.Value;
 * </pre>
 */
@Builder
@Data
public class Value implements Serializable {

  Key key;
  public byte[] prev; // int256
  public byte[] next; // int256
  public byte[] fileHash; // int256

  // Convenience getters
  public String getPrev() {
    return Utils.bytesToHex(prev);
  }

  public String getNext() {
    return Utils.bytesToHex(next);
  }

  public String getFileHash() {
    return Utils.bytesToHex(fileHash);
  }

  public static Value deserialize(ByteBuffer buffer) {
    buffer.order(ByteOrder.LITTLE_ENDIAN);
    Key key = null; // todo
    byte[] prev = Utils.read(buffer, 32);
    byte[] next = Utils.read(buffer, 32);
    byte[] fileHash = Utils.read(buffer, 32);

    return Value.builder().key(key).prev(prev).next(next).fileHash(fileHash).build();
  }

  public byte[] serialize() {
    ByteBuffer buffer = ByteBuffer.allocate(32 + 32 + 32 + key.serialize().length);
    buffer.order(ByteOrder.LITTLE_ENDIAN);
    buffer.put(key.serialize());
    buffer.put(prev);
    buffer.put(next);
    buffer.put(fileHash);
    return buffer.array();
  }
}
