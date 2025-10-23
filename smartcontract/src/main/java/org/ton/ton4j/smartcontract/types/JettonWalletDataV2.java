package org.ton.ton4j.smartcontract.types;

import java.math.BigInteger;
import lombok.Builder;
import lombok.Data;
import org.ton.ton4j.address.Address;
import org.ton.ton4j.cell.Cell;

@Builder
@Data
public class JettonWalletDataV2 {
  BigInteger balance;
  Address ownerAddress;
  Address jettonMinterAddress;
  Cell jettonWalletCode;
}
