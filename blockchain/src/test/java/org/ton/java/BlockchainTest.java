package org.ton.java;

import com.iwebpp.crypto.TweetNaclFast;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.ton.java.smartcontract.wallet.v5.WalletV5;
import org.ton.java.utils.Utils;

@Slf4j
@RunWith(JUnit4.class)
public class BlockchainTest {
  @Test
  public void testBlockchain() {
    TweetNaclFast.Signature.KeyPair keyPair = Utils.generateSignatureKeyPair();
    //    WalletV3R2 wallet = WalletV3R2.builder().keyPair(keyPair).walletId(42).build();
    WalletV5 wallet =
        WalletV5.builder().keyPair(keyPair).walletId(42).isSigAuthAllowed(true).build();
    Blockchain blockchain =
        Blockchain.builder()
            //            .network(Network.MY_LOCAL_TON)
            .network(Network.EMULATOR)
            //            .network(Network.TESTNET)
            .myLocalTonInstallationPath("G:/Git_Projects/MyLocalTon/myLocalTon")
            .standardContract(wallet)
            //            .initialDeployTopUpAmount(Utils.toNano(0.2))
            .build();
    //    Blockchain blockchain =
    // Blockchain.builder().network(Network.MAINNET).customContractAsResource("simple.fc").build();
    blockchain.deploy(30);
  }
}
