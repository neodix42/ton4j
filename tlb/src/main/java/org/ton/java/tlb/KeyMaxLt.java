package org.ton.java.tlb;

import java.math.BigInteger;
import lombok.Builder;
import lombok.Data;
import org.ton.java.cell.Cell;
import org.ton.java.cell.CellBuilder;
import org.ton.java.cell.CellSlice;

/**
 * _ key:Bool max_end_lt:uint64 = KeyMaxLt;
 */
@Builder
@Data

public class KeyMaxLt {
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

    public Cell toCell() {
        return CellBuilder.beginCell()
                .storeUint(endLt, 64)
                .storeUint(seqno, 32)
                .storeUint(rootHash, 256)
                .storeUint(fileHash, 256)
                .endCell();
    }

    public static KeyMaxLt deserialize(CellSlice cs) {
        return KeyMaxLt.builder()
                .endLt(cs.loadUint(64))
                .seqno(cs.loadUint(32).intValue())
                .rootHash(cs.loadUint(256))
                .fileHash(cs.loadUint(256))
                .build();
    }
}
