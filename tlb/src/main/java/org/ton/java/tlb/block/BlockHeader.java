package org.ton.java.tlb.block;

public class BlockHeader {
    BlockInfoPart blockInfoPart;
    GlobalVersion getSoftware;
    ExtBlkRef masterRef;
    BlkPrevInfo prevRef;
    BlkPrevInfo prevVertRef;
}
