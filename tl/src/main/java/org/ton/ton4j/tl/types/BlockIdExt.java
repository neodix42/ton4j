package org.ton.ton4j.tl.types;

import lombok.Builder;
import lombok.Data;
import org.ton.ton4j.cell.CellSlice;
import org.ton.ton4j.utils.Utils;

@Builder
@Data
/**
 * ton_api.tl tonNode.blockIdExt workchain:int shard:long seqno:int root_hash:int256
 * file_hash:int256 = tonNode.BlockIdExt;
 */
public class BlockIdExt {
  long workchain;
  long shard;
  long seqno;
  byte[] rootHash;
  byte[] fileHash;

  private String getRootHash() {
    return Utils.bytesToHex(rootHash);
  }

  private String getFileHash() {
    return Utils.bytesToHex(fileHash);
  }

  public String getShard() {
    return Long.toHexString(shard);
  }

  public static BlockIdExt deserialize(CellSlice cs) {
    BlockIdExt blockIdExt =
        BlockIdExt.builder()
            .workchain(Utils.bytesToInt(Utils.reverseByteArray(cs.loadBytes(32))))
            .shard(Long.reverseBytes(cs.loadUint(64).longValue()))
            .seqno(Utils.bytesToInt(Utils.reverseByteArray(cs.loadBytes(32))))
            .rootHash(Utils.reverseByteArray(cs.loadBytes(256)))
            .fileHash(Utils.reverseByteArray(cs.loadBytes(256)))
            .build();
    return blockIdExt;
  }
}
