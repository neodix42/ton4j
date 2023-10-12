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
public class StateUpdate {
    //    ShardState oldOne;
    Cell oldOne;
    Cell newOne; // todo ShardState?
}
