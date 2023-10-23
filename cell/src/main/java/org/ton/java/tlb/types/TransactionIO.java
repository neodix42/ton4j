package org.ton.java.tlb.types;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.ton.java.cell.TonHashMapE;

@Builder
@Getter
@Setter
@ToString
/**
 * ^[ in_msg:(Maybe ^(Message Any)) out_msgs:(HashmapE 15 ^(Message Any)) ]
 */
public class TransactionIO {
    Message in; // `tlb:"maybe ^"`
    TonHashMapE out;  // `tlb:"maybe ^"`
}
