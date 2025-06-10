package org.ton.ton4j.tl.liteserver.responses;

import java.io.Serializable;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import lombok.Builder;
import lombok.Data;
import org.ton.ton4j.utils.Utils;

/**
 * liteServer.blockHeader id:tonNode.blockIdExt mode:# header_proof:bytes = liteServer.BlockHeader;;
 */
@Builder
@Data
public class BlockHeader implements Serializable, LiteServerAnswer {
  public static final int BLOCK_HEADER_ANSWER = 1965916697;

  BlockIdExt id;
  int mode;
  public byte[] headerProof;

  public String getHeaderProof() {
    if (headerProof == null) {
      return "";
    }
    return Utils.bytesToHex(headerProof);
  }

  public static final int constructorId = BLOCK_HEADER_ANSWER;

  public byte[] serialize() {
    byte[] t1 = Utils.toBytes(headerProof);
    return ByteBuffer.allocate(BlockIdExt.getSize() + 4 + t1.length)
        .put(id.serialize())
        .putInt(mode)
        .put(t1)
        .array();
  }

  public static BlockHeader deserialize(ByteBuffer byteBuffer) {
    byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
    return BlockHeader.builder()
        .id(BlockIdExt.deserialize(byteBuffer))
        .mode(byteBuffer.getInt())
        .headerProof(Utils.fromBytes(byteBuffer))
        .build();
  }

  public static BlockHeader deserialize(byte[] byteArray) {
    return deserialize(ByteBuffer.wrap(byteArray));
  }
}
