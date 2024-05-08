package org.ton.java.smartcontract.types;

import com.iwebpp.crypto.TweetNaclFast;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.ton.java.address.Address;
import org.ton.java.cell.Cell;
import org.ton.java.smartcontract.wallet.v3.WalletV3ContractR1;

import java.math.BigInteger;

@Builder
@Getter
@Setter
@ToString
public class DnsRootConfig implements WalletConfig {
    int wc;
    long seqno;
    int mode;
    long createdAt;
    Address destination;
    BigInteger amount;
    Cell body;
    String comment;
    WalletV3ContractR1 adminWallet;
    TweetNaclFast.Signature.KeyPair adminKeyPair;
    org.ton.java.tlb.types.StateInit stateInit;

}
