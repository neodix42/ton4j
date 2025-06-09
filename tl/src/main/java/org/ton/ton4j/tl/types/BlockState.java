package org.ton.ton4j.tl.types;

import java.io.Serializable;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import lombok.Builder;
import lombok.Data;
import org.ton.ton4j.utils.Utils;

/**
 * liteServer.blockState id:tonNode.blockIdExt root_hash:int256 file_hash:int256 data:bytes =
 * liteServer.BlockState;
 */
@Builder
@Data
public class BlockState implements Serializable, LiteServerAnswer {
  public static final int BLOCK_STATE_ANSWER = 659847997;

  private BlockIdExt id;
  private byte[] rootHash;
  private byte[] fileHash;
  private byte[] data;

  String getData() {
    return Utils.bytesToHex(data);
  }

  public static final int constructorId = BLOCK_STATE_ANSWER;

  public static BlockState deserialize(ByteBuffer byteBuffer) {
    byteBuffer.order(ByteOrder.LITTLE_ENDIAN);

    return BlockState.builder()
        .id(BlockIdExt.deserialize(byteBuffer))
        .rootHash(Utils.read(byteBuffer, 32))
        .fileHash(Utils.read(byteBuffer, 32))
        .data(Utils.fromBytes(byteBuffer))
        .build();
  }

  public static BlockState deserialize(byte[] data) {
    return deserialize(ByteBuffer.wrap(data));
  }
}
