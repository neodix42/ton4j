package org.ton.java.smartcontract.types;

import java.math.BigInteger;
import java.util.List;
import lombok.Builder;
import lombok.Data;
import org.ton.java.address.Address;
import org.ton.java.cell.Cell;
import org.ton.java.tlb.*;
import org.ton.java.tonlib.types.ExtraCurrency;

@Builder
@Data
public class WalletV2R1Config implements WalletConfig {
  Boolean bounce;
  long seqno;
  int mode;
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
