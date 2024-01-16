package org.ton.java.tl.types;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;
import org.ton.java.utils.Utils;

@ToString
@Builder
@Getter
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
}
