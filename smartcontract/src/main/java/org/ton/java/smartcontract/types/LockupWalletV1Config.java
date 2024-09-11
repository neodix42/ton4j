package org.ton.java.smartcontract.types;

import lombok.Builder;
import lombok.Data;
import org.ton.java.address.Address;
import org.ton.java.cell.Cell;
import org.ton.java.tlb.types.StateInit;

import java.math.BigInteger;

@Builder
@Data
public class LockupWalletV1Config implements WalletConfig {
    long walletId;
    long seqno;
    int mode;
    boolean bounce;
    long validUntil;
    Address destination;
    StateInit stateInit;
    BigInteger amount;
    Cell body;
    String comment;
}
