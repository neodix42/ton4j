package org.ton.java.smartcontract.types;

import lombok.Builder;
import lombok.Data;
import org.ton.java.address.Address;
import org.ton.java.cell.Cell;
import org.ton.java.tlb.types.StateInit;

import java.math.BigInteger;

@Builder
@Data
public class WalletV1R3Config implements WalletConfig {
    Boolean bounce;
    long seqno;
    int mode;
    Address destination;
    BigInteger amount;
    Cell body;
    StateInit stateInit;
    String comment;
}
