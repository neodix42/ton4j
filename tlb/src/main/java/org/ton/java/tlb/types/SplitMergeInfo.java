package org.ton.java.tlb.types;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Builder
@Getter
@Setter
@ToString
public class SplitMergeInfo {
    int curShardPfxLen; //  `tlb:"## 6"`
    int accSplitDepth; //  `tlb:"## 6"`
    byte[] thisAddr; //     []byte `tlb:"bits 256"`
    byte[] siblingAddr; //    []byte `tlb:"bits 256"`
}
