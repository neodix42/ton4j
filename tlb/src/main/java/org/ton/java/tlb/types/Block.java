package org.ton.java.tlb.types;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.ton.java.cell.Cell;

@Builder
@Getter
@Setter
public class Block {
    int magic; // `tlb:"#11ef55aa"`
    int globalId; // `tlb:"## 32"`
    BlockHeader blockInfo; // `tlb:"^"`
    Cell valueFlow; // `tlb:"^"`
    StateUpdate stateUpdate; // `tlb:"^"`
    McBlockExtra extra; // `tlb:"^"`
}
