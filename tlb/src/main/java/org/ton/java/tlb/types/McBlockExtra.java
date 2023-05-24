package org.ton.java.tlb.types;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.ton.java.cell.TonHashMap;

@Builder
@Getter
@Setter
public class McBlockExtra {
    long magic; // `tlb:"#cca5"`
    boolean keyBlock; // `tlb:"## 1"`
    TonHashMap shardHashes; // `tlb:"dict 32"`
    TonHashMap shardFees; // `tlb:"dict 96"`
}
