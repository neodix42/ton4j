package org.ton.ton4j.smartcontract.types;

import lombok.Builder;
import lombok.Data;
import org.ton.ton4j.address.Address;
import org.ton.ton4j.cell.Cell;

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
