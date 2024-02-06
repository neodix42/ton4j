package org.ton.java.tlb.types;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.ton.java.cell.Cell;
import org.ton.java.cell.CellBuilder;
import org.ton.java.cell.CellSlice;

import java.math.BigInteger;

@Builder
@Getter
@Setter
@ToString
/**
 * _ key:Bool max_end_lt:uint64 = KeyMaxLt;
 */
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
