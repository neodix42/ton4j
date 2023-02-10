package org.ton.java.tlb.block;

import org.ton.java.cell.Cell;

public class Block {
    int magic; // `tlb:"#11ef55aa"`
    int globalId;
    BlockHeader blockInfo;
    Cell valueFlow;
    StateUpdate stateUpdate;
    McBlockExtra extra;
}
