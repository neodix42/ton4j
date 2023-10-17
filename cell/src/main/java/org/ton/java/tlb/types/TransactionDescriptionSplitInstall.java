package org.ton.java.tlb.types;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Builder
@Getter
@Setter
@ToString
public class TransactionDescriptionSplitInstall {
    int magic; //        `tlb:"$0101"`
    SplitMergeInfo splitInfo; // `tlb:"."`
    Transaction prepareTransaction; //   `tlb:"^"`
    boolean installed; //           `tlb:"bool"`

    private String getMagic() {
        return Long.toBinaryString(magic);
    }
}
