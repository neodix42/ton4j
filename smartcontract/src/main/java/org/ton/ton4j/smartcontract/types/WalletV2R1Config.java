package org.ton.ton4j.smartcontract.types;

import java.math.BigInteger;
import java.util.List;
import lombok.Builder;
import lombok.Data;
import org.ton.ton4j.address.Address;
import org.ton.ton4j.cell.Cell;
import org.ton.ton4j.smartcontract.SendMode;
import org.ton.ton4j.tlb.*;
import org.ton.ton4j.tonlib.types.ExtraCurrency;

@Builder
@Data
public class WalletV2R1Config implements WalletConfig {
  Boolean bounce;
  long seqno;

  @Deprecated(since = "0.9.9", forRemoval = true)
  int mode;

  SendMode sendMode;
  long validUntil;
  Address destination1;
  Address destination2;
  Address destination3;
  Address destination4;
  BigInteger amount1;
  List<ExtraCurrency> extraCurrencies1;
  BigInteger amount2;
  List<ExtraCurrency> extraCurrencies2;
  BigInteger amount3;
  List<ExtraCurrency> extraCurrencies3;
  BigInteger amount4;
  List<ExtraCurrency> extraCurrencies4;
  Cell body;
  StateInit stateInit;
  String comment;
  BigInteger amount; // for internal msg
}
