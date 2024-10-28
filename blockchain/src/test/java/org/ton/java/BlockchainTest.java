package org.ton.java;

import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.ton.java.smartcontract.wallet.v5.WalletV5;

@Slf4j
@RunWith(JUnit4.class)
public class BlockchainTest {
  @Test
  public void testBlockchain() {
    WalletV5 walletV5 = WalletV5.builder().build();
    Blockchain blockchain =
        Blockchain.builder().network(Network.TESTNET).standardContract(walletV5).build();
    //    Blockchain blockchain =
    // Blockchain.builder().network(Network.MAINNET).customContractAsResource("simple.fc").build();
    blockchain.deploy(30);
  }
}
