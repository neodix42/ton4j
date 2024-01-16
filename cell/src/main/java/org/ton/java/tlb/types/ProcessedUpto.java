package org.ton.java.tlb.types;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.ton.java.cell.Cell;
import org.ton.java.cell.CellBuilder;

import java.math.BigInteger;

@Builder
@Getter
@Setter
@ToString
/**
 processed_upto$_ last_msg_lt:uint64 last_msg_hash:bits256 = ProcessedUpto;
 */

public class ProcessedUpto {
    BigInteger lastMsgLt;
    BigInteger lastMsgHash;

    public Cell toCell() {
        return CellBuilder.beginCell()
                .storeUint(lastMsgLt, 64)
                .storeUint(lastMsgHash, 64)
                .endCell();
    }
}
