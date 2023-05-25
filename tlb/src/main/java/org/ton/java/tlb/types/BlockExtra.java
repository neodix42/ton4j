package org.ton.java.tlb.types;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.ton.java.cell.Cell;

@Builder
@Getter
@Setter
@ToString
public class BlockExtra {
    long magic; //        `tlb:"#4a33f6fd"`
    Cell inMsgDesc; // `tlb:"^"`
    Cell outMsgDesc; // `tlb:"^"`
    Cell shardAccountBlocks; //`tlb:"^"`
    byte[] randSeed; // tlb:"bits 256"`
    byte[] createdBy;// tlb:"bits 256"`
    McBlockExtra custom; // `tlb:"maybe ^"`
}
