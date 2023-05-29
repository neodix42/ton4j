package org.ton.java.tlb.types;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.ton.java.address.Address;
import org.ton.java.cell.Cell;

import java.math.BigInteger;

@Builder
@Getter
@Setter
@ToString
public class ExternalMessageOut {
    long magic;             // `tlb:"$11"` ext_out_msg_info$11
    Address srcAddr;        // `tlb:"addr"`
    Address dstAddr;        // `tlb:"addr"`
    BigInteger createdLt;   // `tlb:"## 64"`
    Long createdAt;         // `tlb:"## 32"`
    StateInit stateInit;    // `tlb:"maybe either . ^"`
    Cell body;              // `tlb:"either . ^"`
}
