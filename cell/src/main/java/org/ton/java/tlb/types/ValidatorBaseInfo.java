package org.ton.java.tlb.types;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.ton.java.cell.Cell;
import org.ton.java.cell.CellBuilder;

@Builder
@Getter
@Setter
@ToString
/**
 validator_base_info$_
 validator_list_hash_short:uint32
 catchain_seqno:uint32
 = ValidatorBaseInfo;
 */
public class ValidatorBaseInfo {
    int magic;
    long validatorListHashShort;
    long catchainSeqno;

    public Cell toCell() {
        return CellBuilder.beginCell()
                .storeUint(validatorListHashShort, 32)
                .storeUint(catchainSeqno, 32)
                .endCell();
    }
}
