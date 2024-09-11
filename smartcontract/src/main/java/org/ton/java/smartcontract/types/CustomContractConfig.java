package org.ton.java.smartcontract.types;

import lombok.Builder;
import lombok.Data;
import org.ton.java.address.Address;
import org.ton.java.cell.Cell;

import java.math.BigInteger;

@Builder
@Data
public class CustomContractConfig implements WalletConfig {
    long seqno;
    int mode;
    long validUntil;
    Address destination;
    BigInteger amount;
    Cell body;
    long extraField;
    String comment;
}
