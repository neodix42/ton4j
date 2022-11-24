package org.ton.java.smartcontract.wallet;

import com.iwebpp.crypto.TweetNaclFast;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.ton.java.address.Address;
import org.ton.java.cell.Cell;
import org.ton.java.smartcontract.payments.ClosingConfig;
import org.ton.java.smartcontract.types.ChannelConfig;
import org.ton.java.smartcontract.types.HighloadConfig;
import org.ton.java.smartcontract.types.LockupConfig;
import org.ton.java.smartcontract.wallet.v4.SubscriptionInfo;

import java.math.BigInteger;

@Builder
@Getter
@Setter
@ToString
public class Options {
    public byte[] secretKey;
    public byte[] publicKey;
    public long wc;
    public Address address;
    public BigInteger amount;
    public Cell code;
    public long seqno;
    public long queryId;
    public BigInteger highloadQueryId;
    public Object payload;
    public int sendMode;
    public Cell stateInit;
    public Long walletId;
    public LockupConfig lockupConfig;
    public HighloadConfig highloadConfig;
    public SubscriptionInfo subscriptionConfig;
    public String index; //dns item index, sha256
    public Address collectionAddress; // todo dns config
    public Cell collectionContent;
    public String collectionContentUri;
    public String collectionContentBaseUri;
    public String dnsItemCodeHex;
    public Address adminAddress;
    public String jettonContentUri; // todo jetton config
    public String jettonWalletCodeHex;
    public Address marketplaceAddress;
    public Address nftItemAddress;
    public String nftItemCodeHex;
    public BigInteger fullPrice;
    public BigInteger marketplaceFee;
    public Address royaltyAddress;
    public BigInteger royaltyAmount;
    public double royalty;

    public String nftItemContentBaseUri;

    //payments
    ChannelConfig channelConfig;

    public byte[] publicKeyA;
    public byte[] publicKeyB;
    public boolean isA;
    public TweetNaclFast.Signature.KeyPair myKeyPair;
    public byte[] hisPublicKey;
    public BigInteger excessFee;
    public ClosingConfig closingConfig;
}
