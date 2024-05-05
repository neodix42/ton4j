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
public class WalletV2R2Config implements WalletConfig {
    long seqno;
    byte mode;
    long createdAt;
    Address destination1;
    Address destination2;
    Address destination3;
    Address destination4;
    BigInteger amount1;
    BigInteger amount2;
    BigInteger amount3;
    BigInteger amount4;
    Cell body;
}
