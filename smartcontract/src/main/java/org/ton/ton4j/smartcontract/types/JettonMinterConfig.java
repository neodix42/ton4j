package org.ton.ton4j.smartcontract.types;

import java.math.BigInteger;
import lombok.Builder;
import lombok.Data;
import org.ton.ton4j.address.Address;
import org.ton.ton4j.cell.Cell;
import org.ton.ton4j.smartcontract.SendMode;

@Builder
@Data
public class JettonMinterConfig implements WalletConfig {
  long seqno;

  @Deprecated(since = "0.9.9", forRemoval = true)
  int mode;

  SendMode sendMode;
  long createdAt;
  Address destination;
  BigInteger amount;
  Cell body;
  boolean withoutOp;
  NewPlugin newPlugin;

  BigInteger walletMsgValue;
  BigInteger mintMsgValue;
  BigInteger jettonToMintAmount;
}
