package org.ton.java.tlb.block;

import org.ton.java.cell.Cell;

public class BlockExtra {
    int Magic; //        `tlb:"#4a33f6fd"`
    Cell inMsgDesc;
    Cell outMsgDesc;
    Cell shardAccountBlocks;
    byte[] randSeed; // tlb:"bits 256"`
    byte[] createdBy;// tlb:"bits 256"`
    McBlockExtra custom; // `tlb:"maybe ^"`
}
