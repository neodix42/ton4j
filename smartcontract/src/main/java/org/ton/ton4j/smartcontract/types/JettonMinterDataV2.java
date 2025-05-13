package org.ton.ton4j.smartcontract.types;

import lombok.Builder;
import lombok.Data;
import org.ton.ton4j.address.Address;
import org.ton.ton4j.cell.Cell;

import java.math.BigInteger;

@Builder
@Data
public class JettonMinterDataV2 {
    BigInteger totalSupply;
    Address adminAddress;
    Address nextAdminAddress;
    Cell jettonContentCell;
    String jettonContentUri;
    Cell jettonWalletCode;
}
