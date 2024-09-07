package org.ton.java.tlb.types;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.ton.java.cell.Cell;
import org.ton.java.cell.CellBuilder;
import org.ton.java.cell.CellSlice;

import java.math.BigInteger;

/**
 * <pre>
 * processed_upto$_ last_msg_lt:uint64 last_msg_hash:bits256 = ProcessedUpto;
 * </pre>
 */
@Builder
@Getter
@Setter
@ToString


public class ProcessedUpto {
    BigInteger lastMsgLt;
    BigInteger lastMsgHash;

    public Cell toCell() {
        return CellBuilder.beginCell()
                .storeUint(lastMsgLt, 64)
                .storeUint(lastMsgHash, 64)
                .endCell();
    }

    public static ProcessedUpto deserialize(CellSlice cs) {
        return ProcessedUpto.builder()
                .lastMsgLt(cs.loadUint(64))
                .lastMsgHash(cs.loadUint(64))
                .build();
    }
}
