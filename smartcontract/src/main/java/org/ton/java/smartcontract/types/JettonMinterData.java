package org.ton.java.smartcontract.types;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.ton.java.address.Address;
import org.ton.java.cell.Cell;

import java.math.BigInteger;

@Builder
@Getter
@Setter
public class JettonMinterData {
    BigInteger totalSupply;
    boolean isMutable;
    Address adminAddress;
    Cell jettonContentCell;
    String jettonContentUri;
    Cell jettonWalletCode;
}
