package org.ton.java.smartcontract.types;

import lombok.Builder;
import lombok.Data;
import org.ton.java.cell.Cell;
import org.ton.java.tonlib.types.ExtraCurrency;

import java.math.BigInteger;
import java.util.List;

@Builder
@Data
public class WalletV5Config implements WalletConfig {
  int op;
  long walletId;
  long seqno;
  long validUntil;
  boolean bounce;
  Cell body;
  boolean signatureAllowed;
  BigInteger amount;
  List<ExtraCurrency> extraCurrencies;
  long queryId;
}
