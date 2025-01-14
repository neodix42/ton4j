package org.ton.java.smartcontract.types;

import lombok.Builder;
import lombok.Data;
import org.ton.java.address.Address;
import org.ton.java.cell.Cell;
import org.ton.java.tlb.types.StateInit;
import org.ton.java.tonlib.types.ExtraCurrency;

import java.math.BigInteger;
import java.util.List;

@Builder
@Data
public class WalletV1R1Config implements WalletConfig {
  Boolean bounce; // default true
  long seqno;
  int mode; // default 3
  Address destination;
  BigInteger amount;
  List<ExtraCurrency> extraCurrencies;
  Cell body;
  StateInit stateInit;
  String comment; // default ""
}
