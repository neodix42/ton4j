package org.ton.java.tlb.types;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.ton.java.cell.Cell;
import org.ton.java.cell.CellBuilder;
import org.ton.java.cell.CellSlice;

/**
 * <pre>
 * validator_info$_
 * validator_list_hash_short:uint32
 * catchain_seqno:uint32
 * nx_cc_updated:Bool
 * = ValidatorInfo;
 * </pre>
 */
@Builder
@Getter
@Setter
@ToString

public class ValidatorInfo {
    long validatorListHashShort;
    long catchainSeqno;
    boolean nXCcUpdated;

    public Cell toCell() {
        return CellBuilder.beginCell()
                .storeUint(validatorListHashShort, 32)
                .storeUint(catchainSeqno, 32)
                .storeBit(nXCcUpdated)
                .endCell();
    }

    public static ValidatorInfo deserialize(CellSlice cs) {
        return ValidatorInfo.builder()
                .validatorListHashShort(cs.loadUint(32).longValue())
                .catchainSeqno(cs.loadUint(32).longValue())
                .nXCcUpdated(cs.loadBit())
                .build();
    }
}
