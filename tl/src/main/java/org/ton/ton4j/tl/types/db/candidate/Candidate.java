package org.ton.ton4j.tl.types.db.candidate;

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
 * db.candidate source:PublicKey id:tonNode.blockIdExt data:bytes collated_data:bytes = db.Candidate;
 * </pre>
 */
@Builder
@Data
public class Candidate implements Serializable {

  public byte[] source; // PublicKey
  BlockIdExt id;
  public byte[] data;
  public byte[] collatedData;

  // Convenience getter
  public String getSource() {
    return Utils.bytesToHex(source);
  }

  public static Candidate deserialize(ByteBuffer buffer) {
    buffer.order(ByteOrder.LITTLE_ENDIAN);
    byte[] source = Utils.read(buffer, 32); // Assuming PublicKey is 32 bytes
    BlockIdExt id = BlockIdExt.deserialize(buffer);
    byte[] data = Utils.fromBytes(buffer);
    byte[] collatedData = Utils.fromBytes(buffer);

    return Candidate.builder().source(source).id(id).data(data).collatedData(collatedData).build();
  }

  public byte[] serialize() {
    ByteBuffer buffer =
        ByteBuffer.allocate(
            32
                + BlockIdExt.getSize()
                + Utils.toBytes(data).length
                + Utils.toBytes(collatedData).length);
    buffer.order(ByteOrder.LITTLE_ENDIAN);
    buffer.put(source);
    buffer.put(id.serialize());
    buffer.put(Utils.toBytes(data));
    buffer.put(Utils.toBytes(collatedData));
    return buffer.array();
  }
}
