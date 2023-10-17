package org.ton.java.tlb.types;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Builder
@Getter
@Setter
@ToString
public class TransactionDescriptionTickTock {
    int magic; //       `tlb:"$001"`
    boolean isTock; //         `tlb:"bool"`
    StoragePhase storagePhase; // `tlb:"."`
    ComputePhase computePhase; // `tlb:"."`
    ActionPhase actionPhase; // `tlb:"maybe ^"`
    boolean aborted; //         `tlb:"bool"`
    boolean destroyed; //         `tlb:"bool"`

    private String getMagic() {
        return Long.toBinaryString(magic);
    }
}
