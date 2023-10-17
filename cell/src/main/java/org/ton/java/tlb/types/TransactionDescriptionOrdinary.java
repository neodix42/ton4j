package org.ton.java.tlb.types;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Builder
@Getter
@Setter
@ToString
public class TransactionDescriptionOrdinary {
    int magic; //         `tlb:"$0000"`
    boolean creditFirst; //        `tlb:"bool"`
    StoragePhase storagePhase; //  `tlb:"maybe ."`
    CreditPhase creditPhase; //    `tlb:"maybe ."`
    ComputePhase computePhase; //  `tlb:"."`
    ActionPhase actionPhase; //    `tlb:"maybe ^"`
    boolean aborted;            // `tlb:"bool"`
    BouncePhase bouncePhase; //    `tlb:"maybe ."`
    boolean destroyed; //          `tlb:"bool"`

    private String getMagic() {
        return Long.toBinaryString(magic);
    }
}
