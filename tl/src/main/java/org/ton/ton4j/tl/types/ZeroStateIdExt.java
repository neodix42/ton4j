package org.ton.ton4j.tl.types;

import java.nio.ByteBuffer;
import lombok.Builder;
import lombok.Data;
import org.ton.ton4j.utils.Utils;

@Builder
@Data
/**
 * tonNode.zeroStateIdExt workchain:int root_hash:int256 file_hash:int256 = tonNode.ZeroStateIdExt;
 */
public class ZeroStateIdExt {
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
        .putInt(workchain)
        .put(rootHash)
        .put(fileHash)
        .array();
  }

  public static ZeroStateIdExt deserialize(ByteBuffer bf) {
    // bf.order(ByteOrder.LITTLE_ENDIAN);
    ZeroStateIdExt blockIdExt =
        ZeroStateIdExt.builder()
            .workchain(bf.getInt())
            .rootHash(Utils.read(bf, 32))
            .fileHash(Utils.read(bf, 32))
            .build();
    return blockIdExt;
  }

  public static int getSize() {
    return (32 + 256 + 256) / 8;
  }
}
