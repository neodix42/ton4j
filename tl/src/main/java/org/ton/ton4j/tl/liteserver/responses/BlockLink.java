package org.ton.ton4j.tl.liteserver.responses;

import java.nio.ByteBuffer;

public abstract class BlockLink {
  public static BlockLink deserialize(ByteBuffer buffer) {
    int constructorId = buffer.getInt();
    buffer.position(buffer.position() - 4);

    if (constructorId == BlockLinkBack.CONSTRUCTOR_ID) {
      return BlockLinkBack.deserialize(buffer);
    } else if (constructorId == BlockLinkForward.CONSTRUCTOR_ID) {
      return BlockLinkForward.deserialize(buffer);
    } else {
      throw new IllegalArgumentException("Unknown BlockLink constructor: " + constructorId);
    }
  }

  public abstract byte[] serialize();
}
