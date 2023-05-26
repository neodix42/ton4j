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
    byte[] root_hash; // []byte `tl:"int256"`
    byte[] file_hash; // []byte `tl:"int256"`

    String getfile_hash() {
        return Utils.bytesToHex(file_hash);
    }

    String getroot_hash() {
        return Utils.bytesToHex(root_hash);
    }

    public String getShard() {
        return Long.toHexString(shard);
    }
}
