package org.ton.java.tlb.types;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.ton.java.address.Address;
import org.ton.java.cell.Cell;
import org.ton.java.cell.TonHashMap;

import java.math.BigInteger;

@Builder
@Getter
@Setter
@ToString
public class InternalMessage {
    long magic;         // `tlb:"$0"` int_msg_info$0
    boolean iHRDisabled;// `tlb:"bool"`
    boolean bounce;     // `tlb:"bool"`
    boolean bounced;    // `tlb:"bool"`
    Address srcAddr;    // `tlb:"addr"`
    Address dstAddr;    // `tlb:"addr"`
    BigInteger amount;  // `tlb:"."`
    TonHashMap extraCurrencies;// `tlb:"dict 32"`
    BigInteger iHRFee;  // `tlb:"."`
    BigInteger fwdFee;  // `tlb:"."`
    BigInteger createdLt;//`tlb:"## 64"`
    Long createdAt;     // `tlb:"## 32"`
    StateInit stateInit;// `tlb:"maybe either . ^"`
    Cell body;          // `tlb:"either . ^"`
}
