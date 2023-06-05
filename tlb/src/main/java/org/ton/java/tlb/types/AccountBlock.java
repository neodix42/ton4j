package org.ton.java.tlb.types;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.ton.java.cell.Cell;
import org.ton.java.cell.TonHashMapE;

@Builder
@Getter
@Setter
@ToString
public class AccountBlock {
    long magic;             // `tlb:"#5"`
    int[] addr;            // `tlb:"bits 256"`
    TonHashMapE transactions;// `tlb:"dict 64"`
    Cell stateUpdate;       // `tlb:"^"`
}
