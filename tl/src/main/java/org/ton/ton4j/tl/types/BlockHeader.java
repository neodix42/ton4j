package org.ton.ton4j.tl.types;

import java.io.Serializable;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import lombok.Builder;
import lombok.Data;

/**
 * liteServer.lookupBlock mode:# id:tonNode.blockId lt:mode.1?long utime:mode.2?int =
 * liteServer.BlockHeader;
 */
@Builder
@Data
public class BlockHeader implements Serializable, LiteServerAnswer {
  public static final int BLOCK_HEADER_ANSWER = 1965916697;

  int mode;
  BlockId id;
  public long lt; // present if mode has bit 1 set
  int utime; // present if mode has bit 2 set

  public String getLt() {
    return BigInteger.valueOf(lt).toString(16);
  }

  public long getUtime() {
    return utime & 0xFFFFFFFFL;
  }

  public static final int constructorId = BLOCK_HEADER_ANSWER;

  public byte[] serialize() {
    // Calculate size: mode (4 bytes) + BlockId size + conditional fields
    int size = 4; // for mode
    size += id != null ? BlockId.getSize() : 0;
    if ((mode & 1) != 0) size += 8;
    if ((mode & 2) != 0) size += 4;

    ByteBuffer buffer = ByteBuffer.allocate(size);
    buffer.putInt(mode);
    if (id != null) {
      buffer.put(id.serialize());
    }

    if ((mode & 1) != 0) {
      buffer.putLong(lt);
    }
    if ((mode & 2) != 0) {
      buffer.putInt(utime);
    }

    return buffer.array();
  }

  public static BlockHeader deserialize(ByteBuffer byteBuffer) {
    int mode = byteBuffer.getInt();
    BlockId id = BlockId.deserialize(byteBuffer);

    long lt = 0;
    if ((mode & 1) != 0) {
      lt = byteBuffer.getLong();
    }

    int utime = 0;
    if ((mode & 2) != 0) {
      utime = byteBuffer.getInt();
    }

    return BlockHeader.builder().mode(mode).id(id).lt(lt).utime(utime).build();
  }

  public static BlockHeader deserialize(byte[] byteArray) {
    return deserialize(ByteBuffer.wrap(byteArray));
  }
}
