package org.ton.java.tlb.types;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Builder
@Getter
@Setter
@ToString
public class TransactionDescriptionSplitPrepare {
    int magic; //         `tlb:"$0100"`
    SplitMergeInfo splitInfo; // `tlb:"."`
    StoragePhase StoragePhase; //  `tlb:"maybe ."`
    ComputePhase computePhase; //   `tlb:"."`
    ActionPhase actionPhase; //   `tlb:"maybe ^"`
    boolean aborted; //           `tlb:"bool"`
    boolean destroyed; //           `tlb:"bool"`

    private String getMagic() {
        return Long.toBinaryString(magic);
    }
}
