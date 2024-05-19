package org.ton.java.smartcontract.types;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.ton.java.address.Address;
import org.ton.java.cell.Cell;
import org.ton.java.tlb.types.StateInit;

import java.math.BigInteger;

@Builder
@Getter
@Setter
@ToString
public class WalletV1R1Config implements WalletConfig {
    Boolean bounce; // default true
    long seqno;
    int mode; // default 3
    Address destination;
    BigInteger amount;
    Cell body;
    StateInit intMsgStateInit;
    String comment; // default ""
}
