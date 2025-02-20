package org.ton.java.smartcontract.types;

import com.iwebpp.crypto.TweetNaclFast;
import java.math.BigInteger;
import lombok.Builder;
import lombok.Data;
import org.ton.java.address.Address;
import org.ton.java.cell.Cell;
import org.ton.java.smartcontract.wallet.v3.WalletV3R1;
import org.ton.java.tlb.StateInit;

@Builder
@Data
public class DnsRootConfig implements WalletConfig {
  int wc;
  long seqno;
  int mode;
  long createdAt;
  Address destination;
  BigInteger amount;
  Cell body;
  String comment;
  WalletV3R1 adminWallet;
  TweetNaclFast.Signature.KeyPair adminKeyPair;
  StateInit stateInit;
}
