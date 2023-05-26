package org.ton.java.tlb.types;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.ton.java.cell.Cell;
import org.ton.java.cell.TonHashMap;

@Builder
@Getter
@Setter
@ToString
public class AccountBlock {
    long magic;             // `tlb:"#5"`
    byte[] addr;            // `tlb:"bits 256"`
    TonHashMap transactions;// `tlb:"dict 64"`
    Cell stateUpdate;       // `tlb:"^"`
}
