package org.ton.java.smartcontract.types;

import java.math.BigInteger;
import java.util.List;
import lombok.Builder;
import lombok.Data;
import org.ton.java.address.Address;
import org.ton.java.cell.Cell;
import org.ton.java.smartcontract.wallet.v4.SubscriptionInfo;
import org.ton.java.tlb.*;
import org.ton.java.tonlib.types.ExtraCurrency;

@Builder
@Data
public class WalletV4R2Config implements WalletConfig {
  long walletId;
  long seqno;
  int mode;
  long validUntil;
  long createdAt;
  boolean bounce;
  Address destination;
  BigInteger amount;
  List<ExtraCurrency> extraCurrencies;
  Cell body;
  StateInit stateInit;
  String comment;
  int
      operation; // 0 - simple send; 1 - deploy and install plugin; 2 - install plugin; 3 - remove
                 // plugin
  SubscriptionInfo subscriptionInfo;
  NewPlugin newPlugin;
  DeployedPlugin deployedPlugin;
}
