package org.ton.java.tlb.types;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.math.BigInteger;

@Builder
@Getter
@Setter
@ToString
/**
 * ext_blk_ref$_
 * end_lt:uint64
 * seq_no:uint32
 * root_hash:bits256
 * file_hash:bits256 = ExtBlkRef;
 */
public class ExtBlkRef {
    BigInteger endLt;
    int seqno;
    BigInteger rootHash;
    BigInteger fileHash;

    private String getRootHash() {
        return rootHash.toString(16);
    }

    private String getFileHash() {
        return fileHash.toString(16);
    }
}
