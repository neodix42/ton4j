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
 * db.candidate.id source:PublicKey id:tonNode.blockIdExt collated_data_file_hash:int256 = db.candidate.Id;
 * </pre>
 */
@Builder
@Data
public class Id implements Serializable {

  public byte[] source;  // PublicKey
  BlockIdExt id;
  public byte[] collatedDataFileHash;  // int256

  // Convenience getters
  public String getSource() {
    return Utils.bytesToHex(source);
  }
  
  public String getCollatedDataFileHash() {
    return Utils.bytesToHex(collatedDataFileHash);
  }

  public static Id deserialize(ByteBuffer buffer) {
    buffer.order(ByteOrder.LITTLE_ENDIAN);
    byte[] source = Utils.read(buffer, 32);  // Assuming PublicKey is 32 bytes
    BlockIdExt id = BlockIdExt.deserialize(buffer);
    byte[] collatedDataFileHash = Utils.read(buffer, 32);
    
    return Id.builder()
        .source(source)
        .id(id)
        .collatedDataFileHash(collatedDataFileHash)
        .build();
  }

  public byte[] serialize() {
    ByteBuffer buffer = ByteBuffer.allocate(32 + BlockIdExt.getSize() + 32);
    buffer.order(ByteOrder.LITTLE_ENDIAN);
    buffer.put(source);
    buffer.put(id.serialize());
    buffer.put(collatedDataFileHash);
    return buffer.array();
  }
}
