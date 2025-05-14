package org.ton.ton4j.smartcontract.types;

import com.iwebpp.crypto.TweetNaclFast;
import java.math.BigInteger;
import lombok.Builder;
import lombok.Data;
import org.ton.ton4j.address.Address;
import org.ton.ton4j.cell.Cell;
import org.ton.ton4j.smartcontract.SendMode;
import org.ton.ton4j.smartcontract.wallet.v3.WalletV3R1;
import org.ton.ton4j.tlb.StateInit;

@Builder
@Data
public class DnsRootConfig implements WalletConfig {
  int wc;
  long seqno;

  @Deprecated(since = "0.9.9", forRemoval = true)
  int mode;

  SendMode sendMode;
  long createdAt;
  Address destination;
  BigInteger amount;
  Cell body;
  String comment;
  WalletV3R1 adminWallet;
  TweetNaclFast.Signature.KeyPair adminKeyPair;
  StateInit stateInit;
}
