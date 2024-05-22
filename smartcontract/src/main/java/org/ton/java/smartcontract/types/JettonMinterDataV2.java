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
public class JettonMinterDataV2 {
    BigInteger totalSupply;
    Address adminAddress;
    Address nextAdminAddress;
    Cell jettonContentCell;
    String jettonContentUri;
    Cell jettonWalletCode;
}
