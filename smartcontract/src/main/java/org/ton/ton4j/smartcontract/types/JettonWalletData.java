package org.ton.ton4j.smartcontract.types;

import lombok.Builder;
import lombok.Data;
import org.ton.ton4j.address.Address;
import org.ton.ton4j.cell.Cell;

import java.math.BigInteger;

@Builder
@Data
public class JettonWalletData {
    BigInteger balance;
    Address ownerAddress;
    Address jettonMinterAddress;
    Cell jettonWalletCode;
}
