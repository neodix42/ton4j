package org.ton.java.smartcontract.types;

import lombok.Builder;
import lombok.Data;
import org.ton.java.address.Address;
import org.ton.java.cell.Cell;
import org.ton.java.tlb.types.StateInit;

import java.math.BigInteger;

@Builder
@Data
public class WalletV3Config implements WalletConfig {
    long walletId;
    long seqno;
    int mode;
    long validUntil;
    boolean bounce;
    Address source;
    Address destination;
    BigInteger amount;
    Cell body;
    StateInit stateInit;
    String comment;
}
