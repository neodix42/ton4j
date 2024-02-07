package org.ton.java.tl.types;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.ton.java.cell.CellSlice;
import org.ton.java.utils.Utils;

@ToString
@Builder
@Getter
@Setter
/**
 * ton_api.tl
 * tonNode.blockIdExt
 *   workchain:int
 *   shard:long
 *   seqno:int
 *   root_hash:int256
 *   file_hash:int256 = tonNode.BlockIdExt;
 */
public class BlockIdExt {
    long workchain;
    long shard;
    long seqno;
    int[] rootHash;
    int[] fileHash;

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
        BlockIdExt blockIdExt = BlockIdExt.builder()
                .workchain(Utils.intsToInt(Utils.reverseIntArray(cs.loadBytes(32))))
                .shard(Long.reverseBytes(cs.loadUint(64).longValue()))
                .seqno(Utils.intsToInt(Utils.reverseIntArray(cs.loadBytes(32))))
                .rootHash(Utils.reverseIntArray(cs.loadBytes(256)))
                .fileHash(Utils.reverseIntArray(cs.loadBytes(256)))
                .build();
        return blockIdExt;
    }
}
