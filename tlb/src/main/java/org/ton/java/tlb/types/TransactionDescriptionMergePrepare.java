package org.ton.java.tlb.types;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Builder
@Getter
@Setter
@ToString
public class TransactionDescriptionMergePrepare {
    int magic; //         `tlb:"$0110"`
    SplitMergeInfo splitInfo; // `tlb:"."`
    StoragePhase storagePhase; //   `tlb:"."`
    boolean aborted; //           `tlb:"bool"`

    private String getMagic() {
        return Long.toBinaryString(magic);
    }
}
