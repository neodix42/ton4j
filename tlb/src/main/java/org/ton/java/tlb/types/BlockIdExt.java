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
    int workchain; // 4 bytes, int32
    long shard; // 8 bytes, int64
    long seqno; // 4 bytes, uint32
    byte[] root_hash; // 32 bytes, int256
    byte[] file_hash; // 32 bytes, int256

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
