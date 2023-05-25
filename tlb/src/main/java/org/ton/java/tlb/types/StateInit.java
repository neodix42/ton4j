package org.ton.java.tlb.types;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.ton.java.cell.Cell;

import java.math.BigInteger;
import java.util.HashMap;

@Builder
@Getter
@Setter
@ToString
public class StateInit {
    BigInteger depth;  // `tlb:"maybe ## 5"`
    TickTock tickTock; // `tlb:"maybe ."`
    Cell code; // `tlb:"maybe ^"`
    Cell data; // `tlb:"maybe ^"`
    HashMap lib; // `tlb:"dict 256"`
}
