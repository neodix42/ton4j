package org.ton.java.smartcontract.types;

import lombok.Builder;
import lombok.Data;
import org.ton.java.address.Address;
import org.ton.java.cell.Cell;
import org.ton.java.tlb.types.StateInit;

import java.math.BigInteger;

@Builder
@Data
public class WalletV2R2Config implements WalletConfig {
    Boolean bounce;
    long seqno;
    int mode;
    long validUntil;
    Address destination1;
    Address destination2;
    Address destination3;
    Address destination4;
    BigInteger amount1;
    BigInteger amount2;
    BigInteger amount3;
    BigInteger amount4;
    Cell body;
    StateInit stateInit;
    String comment;
}
