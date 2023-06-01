package org.ton.java.tlb.types;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Builder
@Getter
@Setter
@ToString
public class TransactionDescriptionMergeInstall {
    int magic;                   //         `tlb:"$0111"`
    SplitMergeInfo splitInfo;    //         `tlb:"."`
    Transaction prepareTransaction; //      `tlb:"^"`
    StoragePhase storagePhase;  //          `tlb:"maybe ."`
    CreditPhase creditPhase;    //          `tlb:"maybe ."`
    ComputePhase computePhase;  //          `tlb:"."`
    ActionPhase actionPhase;    //          `tlb:"maybe ^"`
    boolean aborted;            //          `tlb:"bool"`
    boolean destroyed;          //          `tlb:"bool"`
}
