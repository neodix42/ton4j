package org.ton.ton4j.tl.types;

import java.io.Serializable;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import lombok.Builder;
import lombok.Data;
import org.ton.ton4j.utils.Utils;

@Builder
@Data
public class ValidatorStats implements Serializable, LiteServerAnswer {
  public static final int constructorId = -1174956328;

  private final int mode;
  private final BlockIdExt id;
  private final int count;
  private final boolean complete;
  public final byte[] stateProof;
  public final byte[] dataProof;

  public String getStateProof() {
    return Utils.bytesToHex(stateProof);
  }

  public String getDataProof() {
    return Utils.bytesToHex(dataProof);
  }

  public static ValidatorStats deserialize(ByteBuffer buffer) {
    buffer.order(ByteOrder.LITTLE_ENDIAN);
    int mode = buffer.getInt();
    BlockIdExt id = BlockIdExt.deserialize(buffer);
    int count = buffer.getInt();
    boolean complete = buffer.getInt() == Utils.TL_TRUE; // bool
    byte[] stateProof = Utils.fromBytes(buffer);
    byte[] dataProof = Utils.fromBytes(buffer);

    return ValidatorStats.builder()
        .mode(mode)
        .id(id)
        .count(count)
        .complete(complete)
        .stateProof(stateProof)
        .dataProof(dataProof)
        .build();
  }

  public byte[] serialize() {
    byte[] stateProofBytes = Utils.toBytes(stateProof);
    byte[] dataProofBytes = Utils.toBytes(dataProof);

    return ByteBuffer.allocate(
            4 + BlockIdExt.getSize() + 4 + 4 + stateProofBytes.length + dataProofBytes.length)
        .order(ByteOrder.LITTLE_ENDIAN)
        .putInt(mode)
        .put(id.serialize())
        .putInt(count)
        .putInt((complete ? Utils.TL_TRUE : Utils.TL_FALSE))
        .put(stateProofBytes)
        .put(dataProofBytes)
        .array();
  }

  public static ValidatorStats deserialize(byte[] data) {
    return deserialize(ByteBuffer.wrap(data));
  }
}
