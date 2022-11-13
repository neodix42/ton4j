package org.ton.java.smartcontract;

import com.iwebpp.crypto.TweetNaclFast;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;
import org.ton.java.smartcontract.wallet.v3.WalletV3ContractR1;

@Builder
@ToString
@Getter
public class TestWallet {
    TweetNaclFast.Signature.KeyPair keyPair;
    WalletV3ContractR1 wallet;
}
