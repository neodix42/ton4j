package org.ton.java.tlb.types;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Builder
@Getter
@Setter
@ToString
public class BlkPrevInfo {
    ExtBlkRef Prev1;
    ExtBlkRef Prev2; // pointer  https://github.com/xssnick/tonutils-go/blob/46dbf5f820af066ab10c5639a508b4295e5aa0fb/tlb/block.go#L136
}
