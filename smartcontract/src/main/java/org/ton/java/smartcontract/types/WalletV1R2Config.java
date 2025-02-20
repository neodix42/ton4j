package org.ton.java.smartcontract.types;

import lombok.Builder;
import lombok.Data;
import org.ton.java.address.Address;
import org.ton.java.cell.Cell;
import org.ton.java.tlb.*;
import org.ton.java.tonlib.types.ExtraCurrency;

import java.math.BigInteger;
import java.util.List;

@Builder
@Data
public class WalletV1R2Config implements WalletConfig {
  Boolean bounce;
  long seqno;
  int mode;
  Address destination;
  BigInteger amount;
  List<ExtraCurrency> extraCurrencies;
  Cell body;
  StateInit stateInit;
  String comment;
}
