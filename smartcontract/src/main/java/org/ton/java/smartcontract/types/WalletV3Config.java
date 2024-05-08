package org.ton.java.smartcontract.types;

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
public class WalletV3Config implements WalletConfig {
    long subWalletId;
    long seqno;
    int mode;
    boolean bounce;
    long createdAt;
    Address destination;
    BigInteger amount;
    Cell body;
    org.ton.java.tlb.types.StateInit stateInit;
    byte[] secretKey;
    byte[] publicKey;
    String comment;
    long validUntil;
}
