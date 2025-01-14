package org.ton.java.smartcontract.types;

import java.math.BigInteger;
import java.util.List;
import lombok.Builder;
import lombok.Data;
import org.ton.java.address.Address;
import org.ton.java.cell.Cell;
import org.ton.java.tlb.types.StateInit;
import org.ton.java.tonlib.types.ExtraCurrency;

@Builder
@Data
public class WalletV3Config implements WalletConfig {
  long walletId;
  long seqno;
  int mode;
  long validUntil;
  boolean bounce;
  Address source;
  Address destination;
  BigInteger amount;
  List<ExtraCurrency> extraCurrencies;
  Cell body;
  StateInit stateInit;
  String comment;
}
