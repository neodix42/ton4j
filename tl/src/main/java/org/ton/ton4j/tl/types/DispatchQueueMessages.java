package org.ton.ton4j.tl.types;

import java.io.Serializable;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;
import lombok.Builder;
import lombok.Data;
import org.ton.ton4j.cell.Cell;
import org.ton.ton4j.cell.CellBuilder;
import org.ton.ton4j.utils.Utils;

/**
 * liteServer.dispatchQueueMessages mode:# id:tonNode.blockIdExt messages:(vector
 * liteServer.dispatchQueueMessage) complete:Bool proof:mode.0?bytes messages_boc:mode.2?bytes =
 * liteServer.DispatchQueueMessages;
 */
@Builder
@Data
public class DispatchQueueMessages implements Serializable, LiteServerAnswer {
  public static final int CONSTRUCTOR_ID = 1262516529; // Should match query ID

  private final int mode;
  private final BlockIdExt id;
  private final List<DispatchQueueMessage> messages;
  private final boolean complete;
  public final byte[] proof;
  private final byte[] messagesBoc;

  public String getProof() {
    if (proof == null) {
      return "";
    }
    return Utils.bytesToHex(proof);
  }

  public String getMessagesBoc() {
    if (messagesBoc == null) {
      return "";
    }
    return Utils.bytesToHex(messagesBoc);
  }

  public List<Cell> getMessages() {
    if ((messagesBoc == null) || (messages.isEmpty())) {
      return null;
    }
    return CellBuilder.beginCell().fromBocMultiRoot(messagesBoc).endCells();
  }

  public static final int constructorId = CONSTRUCTOR_ID;

  public static DispatchQueueMessages deserialize(ByteBuffer buffer) {
    buffer.order(ByteOrder.LITTLE_ENDIAN);
    int mode = buffer.getInt();
    BlockIdExt id = BlockIdExt.deserialize(buffer);

    int vectorLength = buffer.getInt();
    List<DispatchQueueMessage> messages = new ArrayList<>(vectorLength);
    for (int i = 0; i < vectorLength; i++) {
      messages.add(DispatchQueueMessage.deserialize(buffer));
    }

    boolean complete = buffer.getInt() == Utils.TL_TRUE;
    byte[] proof = null;
    if ((mode & 1) != 0) {
      proof = Utils.fromBytes(buffer);
    }
    byte[] messagesBoc = null;
    if ((mode & 4) != 0) {
      messagesBoc = Utils.fromBytes(buffer);
    }

    return DispatchQueueMessages.builder()
        .mode(mode)
        .id(id)
        .messages(messages)
        .complete(complete)
        .proof(proof)
        .messagesBoc(messagesBoc)
        .build();
  }

  //
  //  public byte[] serialize() {
  //    int size = 4 + BlockIdExt.getSize() + 4;
  //    for (DispatchQueueMessage message : messages) {
  //      size += message.serialize().length;
  //    }
  //    size += 1; // For complete boolean
  //    if ((mode & 1) != 0) size += Utils.toBytes(proof).length;
  //    if ((mode & 4) != 0) size += Utils.toBytes(messagesBoc).length;
  //
  //    ByteBuffer buffer =
  //        ByteBuffer.allocate(size)
  //            .order(ByteOrder.LITTLE_ENDIAN)
  //            .putInt(mode)
  //            .put(id.serialize())
  //            .putInt(messages.size());
  //
  //    for (DispatchQueueMessage message : messages) {
  //      buffer.put(message.serialize());
  //    }
  //
  //    buffer.put((byte) (complete ? 1 : 0));
  //
  //    if ((mode & 1) != 0) {
  //      buffer.put(Utils.toBytes(proof));
  //    }
  //    if ((mode & 4) != 0) {
  //      buffer.put(Utils.toBytes(messagesBoc));
  //    }
  //
  //    return buffer.array();
  //  }

  public static DispatchQueueMessages deserialize(byte[] byteArray) {
    return deserialize(ByteBuffer.wrap(byteArray));
  }
}
