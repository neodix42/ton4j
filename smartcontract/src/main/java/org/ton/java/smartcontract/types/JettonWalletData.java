package org.ton.java.smartcontract.types;

import lombok.Builder;
import lombok.Data;
import org.ton.java.address.Address;
import org.ton.java.cell.Cell;

import java.math.BigInteger;

@Builder
@Data
public class JettonWalletData {
    BigInteger balance;
    Address ownerAddress;
    Address jettonMinterAddress;
    Cell jettonWalletCode;
}
