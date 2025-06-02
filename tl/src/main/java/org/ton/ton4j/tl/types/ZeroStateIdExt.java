package org.ton.ton4j.tl.types;

import java.io.Serializable;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import lombok.Builder;
import lombok.Data;
import org.ton.ton4j.utils.Utils;

@Builder
@Data
/**
 * tonNode.zeroStateIdExt workchain:int root_hash:int256 file_hash:int256 = tonNode.ZeroStateIdExt;
 */
public class ZeroStateIdExt implements Serializable, LiteServerAnswer {
  int workchain;
  byte[] rootHash;
  byte[] fileHash;

  private String getRootHash() {
    return Utils.bytesToHex(rootHash);
  }

  private String getFileHash() {
    return Utils.bytesToHex(fileHash);
  }

  public byte[] serialize() {
    return ByteBuffer.allocate((32 + 256 + 256) / 8)
        .order(ByteOrder.LITTLE_ENDIAN)
        .putInt(workchain)
        .put(rootHash)
        .put(fileHash)
        .array();
  }

  public static ZeroStateIdExt deserialize(ByteBuffer bf) {
    bf.order(ByteOrder.LITTLE_ENDIAN);
    return ZeroStateIdExt.builder()
        .workchain(bf.getInt())
        .rootHash(Utils.read(bf, 32))
        .fileHash(Utils.read(bf, 32))
        .build();
  }

  public static int getSize() {
    return (32 + 256 + 256) / 8;
  }
}
