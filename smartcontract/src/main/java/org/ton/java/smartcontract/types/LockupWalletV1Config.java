package org.ton.java.smartcontract.types;

import java.math.BigInteger;
import java.util.List;
import lombok.Builder;
import lombok.Data;
import org.ton.java.address.Address;
import org.ton.java.cell.Cell;
import org.ton.java.tlb.StateInit;
import org.ton.java.tonlib.types.ExtraCurrency;

@Builder
@Data
public class LockupWalletV1Config implements WalletConfig {
  long walletId;
  long seqno;
  int mode;
  boolean bounce;
  long validUntil;
  Address destination;
  StateInit stateInit;
  BigInteger amount;
  List<ExtraCurrency> extraCurrencies;
  Cell body;
  String comment;
}
