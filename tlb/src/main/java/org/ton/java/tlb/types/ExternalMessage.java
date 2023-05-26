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
public class ExternalMessage {
    long magic;             // `tlb:"$10"`
    Address srcAddr;        // `tlb:"addr"`
    Address dstAddr;        // `tlb:"addr"`
    BigInteger ImportFee;   // `tlb:"."`
    StateInit stateInit;    // `tlb:"maybe either . ^"`
    Cell body;              // `tlb:"either . ^"`
}
