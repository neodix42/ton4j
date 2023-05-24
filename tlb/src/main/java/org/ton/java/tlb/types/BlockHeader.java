package org.ton.java.tlb.types;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Builder
@Getter
@Setter
public class BlockHeader { // BlockIDExt from block.tlb
    BlockInfoPart blockInfoPart;
    GlobalVersion genSoftware;
    ExtBlkRef masterRef;
    BlkPrevInfo prevRef;
    BlkPrevInfo prevVertRef;
}
