package org.ton.ton4j.tl.types;

import lombok.Builder;
import lombok.Data;
import org.ton.ton4j.cell.Cell;
import org.ton.ton4j.cell.CellBuilder;
import org.ton.ton4j.cell.CellSlice;
import org.ton.ton4j.utils.Utils;

@Builder
@Data
/**
 * tonNode.zeroStateIdExt workchain:int root_hash:int256 file_hash:int256 = tonNode.ZeroStateIdExt;
 */
public class ZeroStateIdExt {
  long workchain;
  byte[] rootHash;
  byte[] fileHash;

  private String getRootHash() {
    return Utils.bytesToHex(rootHash);
  }

  private String getFileHash() {
    return Utils.bytesToHex(fileHash);
  }

  public Cell toCell() {
    return CellBuilder.beginCell()
        .storeInt(workchain, 32)
        .storeBytes(rootHash, 256)
        .storeBytes(fileHash, 256)
        .endCell();
  }

  public static ZeroStateIdExt deserialize(CellSlice cs) {
    ZeroStateIdExt blockIdExt =
        ZeroStateIdExt.builder()
            .workchain(Utils.bytesToInt(Utils.reverseByteArray(cs.loadBytes(32))))
            .rootHash(Utils.reverseByteArray(cs.loadBytes(256)))
            .fileHash(Utils.reverseByteArray(cs.loadBytes(256)))
            .build();
    return blockIdExt;
  }
}
