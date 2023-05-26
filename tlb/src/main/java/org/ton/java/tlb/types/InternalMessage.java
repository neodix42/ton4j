package org.ton.java.tlb.types;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.ton.java.address.Address;
import org.ton.java.cell.Cell;

import java.math.BigInteger;
import java.util.HashMap;

@Builder
@Getter
@Setter
@ToString
public class InternalMessage {
    long magic;
    ;//          `tlb:"$0"`
    boolean iHRDisabled;//            `tlb:"bool"`
    boolean bounce;//            `tlb:"bool"`
    boolean bounced;//             `tlb:"bool"`
    Address srcAddr;// `tlb:"addr"`
    Address dstAddr;// `tlb:"addr"`
    BigInteger amount;//            `tlb:"."`
    HashMap extraCurrencies;// `tlb:"dict 32"`
    BigInteger IHRFee;//            `tlb:"."`
    BigInteger FwdFee;//            `tlb:"."`
    BigInteger createdLT;//uint64           `tlb:"## 64"`
    Long createdAt;// uint32           `tlb:"## 32"`

    StateInit stateInit;//`tlb:"maybe either . ^"`
    Cell body;// `tlb:"either . ^"`
}
