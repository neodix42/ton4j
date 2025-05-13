package org.ton.ton4j.smartcontract.types;

import java.math.BigInteger;
import java.util.List;
import lombok.Builder;
import lombok.Data;
import org.ton.ton4j.address.Address;
import org.ton.ton4j.cell.Cell;
import org.ton.ton4j.tlb.*;
import org.ton.ton4j.tonlib.types.ExtraCurrency;

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
