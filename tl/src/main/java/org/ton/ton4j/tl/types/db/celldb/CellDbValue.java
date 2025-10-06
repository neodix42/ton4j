package org.ton.ton4j.tl.types.db.celldb;

import java.io.Serializable;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import lombok.Builder;
import lombok.Data;
import org.ton.ton4j.tl.liteserver.responses.BlockIdExt;
import org.ton.ton4j.utils.Utils;

/**
 *
 *
 * <pre>
 * ton_api.tl
 * db.celldb.value block_id:tonNode.blockIdExt prev:int256 next:int256 root_hash:int256 = db.celldb.Value;
 * </pre>
 */
@Builder
@Data
public class CellDbValue implements Serializable {
  int magic;
  BlockIdExt blockId;
  public byte[] prev; // int256
  public byte[] next; // int256
  public byte[] rootHash; // int256

  // Convenience getters
  public String getPrev() {
    return Utils.bytesToHex(prev);
  }

  public String getNext() {
    return Utils.bytesToHex(next);
  }

  public String getRootHash() {
    return Utils.bytesToHex(rootHash);
  }

  public static CellDbValue deserialize(ByteBuffer buffer) {
    buffer.order(ByteOrder.LITTLE_ENDIAN);
    int magic = buffer.getInt();
    BlockIdExt blockId = BlockIdExt.deserialize(buffer);

    byte[] prev = Utils.read(buffer, 32);
    byte[] next = Utils.read(buffer, 32);
    byte[] rootHash = Utils.read(buffer, 32);

    return CellDbValue.builder()
        .magic(magic)
        .blockId(blockId)
        .prev(prev)
        .next(next)
        .rootHash(rootHash)
        .build();
  }

  public byte[] serialize() {
    ByteBuffer buffer = ByteBuffer.allocate(4 + BlockIdExt.getSize() + 32 + 32 + 32);
    buffer.order(ByteOrder.LITTLE_ENDIAN);
    buffer.putInt(magic);
    buffer.put(blockId.serialize());
    buffer.put(prev);
    buffer.put(next);
    buffer.put(rootHash);
    return buffer.array();
  }

  public static CellDbValue deserialize(byte[] data) {
    return deserialize(ByteBuffer.wrap(data));
  }
}
