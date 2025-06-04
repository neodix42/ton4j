package org.ton.ton4j.tl.types;

import java.io.Serializable;
import java.nio.ByteBuffer;
import lombok.Builder;
import lombok.Data;
import org.ton.ton4j.utils.Utils;

@Data
@Builder
public class BlockState implements Serializable, LiteServerAnswer {
  public static final int BLOCK_STATE_ANSWER = 659847997;

  private BlockIdExt id;
  private BlockIdExt root;
  private byte[] fileHash;
  private byte[] data;

  String getData() {
    return Utils.bytesToHex(data);
  }

  public static final int constructorId = BLOCK_STATE_ANSWER;

  public static BlockState deserialize(ByteBuffer buffer) {
    BlockIdExt id = BlockIdExt.deserialize(buffer);
    BlockIdExt root = BlockIdExt.deserialize(buffer);
    byte[] fileHash = new byte[32];
    buffer.get(fileHash);

    return BlockState.builder()
        .id(id)
        .root(root)
        .fileHash(fileHash)
        .data(Utils.fromBytes(buffer))
        .build();
  }

  public static BlockState deserialize(byte[] data) {
    return deserialize(ByteBuffer.wrap(data));
  }
}
