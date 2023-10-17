package org.ton.java.tlb.types;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;
import org.ton.java.utils.Utils;

@ToString
@Builder
@Getter
public class BlockIdExt {
    //    int fieldId;
    long workchain; // int32  `tl:"int"`
    long shard; // int64  `tl:"long"`
    long seqno; // uint32 `tl:"int"`
    int[] rootHash; // []byte `tl:"int256"`
    int[] fileHash; // []byte `tl:"int256"`

    String getFileHash() {
        return Utils.bytesToHex(fileHash);
    }

    String getRootHash() {
        return Utils.bytesToHex(rootHash);
    }

    public String getShard() {
        return Long.toHexString(shard);
    }
}
