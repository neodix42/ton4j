package org.ton.ton4j.toncenter.model;

import java.math.BigInteger;
import lombok.Builder;
import lombok.Data;
import org.ton.ton4j.address.Address;
import org.ton.ton4j.cell.Cell;

@Builder
@Data
public class JettonMinterData {
    BigInteger totalSupply;
    boolean isMutable;
    Address adminAddress;
    Cell jettonContentCell;
    String jettonContentUri;
    Cell jettonWalletCode;
}
