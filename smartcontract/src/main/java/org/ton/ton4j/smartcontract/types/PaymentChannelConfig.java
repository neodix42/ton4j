package org.ton.ton4j.smartcontract.types;

import java.math.BigInteger;
import lombok.Builder;
import lombok.Data;
import org.ton.ton4j.address.Address;
import org.ton.ton4j.cell.Cell;
import org.ton.ton4j.smartcontract.SendMode;

@Builder
@Data
public class PaymentChannelConfig implements WalletConfig {
  long seqno;

  @Deprecated(since = "0.9.9", forRemoval = true)
  int mode;

  SendMode sendMode;
  long createdAt;
  Address destination;
  BigInteger amount;
  Cell body;
  int operation; // 0 - simple send; 1 - deploy and install plugin; 2 - install plugin; 3 - remove
  // plugin
  NewPlugin newPlugin;
  DeployedPlugin deployedPlugin;
}
