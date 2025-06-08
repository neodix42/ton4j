package org.ton.ton4j.tl.types;

import java.io.Serializable;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;
import lombok.Builder;
import lombok.Data;
import org.ton.ton4j.utils.Utils;

@Builder
@Data
public class PartialBlockProof implements Serializable, LiteServerAnswer {
  public static final int CONSTRUCTOR_ID = -1898917183;

  private final boolean complete;
  private final BlockIdExt from;
  private final BlockIdExt to;
  private final List<BlockLink> steps;

  public static final int constructorId = CONSTRUCTOR_ID;

  public static PartialBlockProof deserialize(ByteBuffer buffer) {
    buffer.order(ByteOrder.LITTLE_ENDIAN);
    boolean complete = buffer.getInt() == Utils.TL_TRUE;
    BlockIdExt from = BlockIdExt.deserialize(buffer);
    BlockIdExt to = BlockIdExt.deserialize(buffer);

    int vectorLength = buffer.getInt();
    List<BlockLink> steps = new ArrayList<>(vectorLength);
    for (int i = 0; i < vectorLength; i++) {
      steps.add(BlockLink.deserialize(buffer));
    }

    return PartialBlockProof.builder().complete(complete).from(from).to(to).steps(steps).build();
  }

  public byte[] serialize() {
    int size = 1 + BlockIdExt.getSize() + BlockIdExt.getSize() + 4;
    for (BlockLink step : steps) {
      size += step.serialize().length;
    }

    ByteBuffer buffer =
        ByteBuffer.allocate(size)
            .order(ByteOrder.LITTLE_ENDIAN)
            .putInt((complete ? Utils.TL_TRUE : Utils.TL_FALSE))
            .put(from.serialize())
            .put(to.serialize())
            .putInt(steps.size());

    for (BlockLink step : steps) {
      buffer.put(step.serialize());
    }

    return buffer.array();
  }

  public static PartialBlockProof deserialize(byte[] byteBuffer) {
    return deserialize(ByteBuffer.wrap(byteBuffer));
  }
}
