package org.ton.java;

import com.iwebpp.crypto.TweetNaclFast;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.ton.java.cell.CellBuilder;
import org.ton.java.smartcontract.wallet.v5.WalletV5;
import org.ton.java.utils.Utils;

@Slf4j
@RunWith(JUnit4.class)
public class BlockchainTest {
  @Test
  public void testDeployV5ContractOnEmulator() {
    TweetNaclFast.Signature.KeyPair keyPair = Utils.generateSignatureKeyPair();
    WalletV5 wallet =
        WalletV5.builder().keyPair(keyPair).walletId(42).isSigAuthAllowed(true).build();
    Blockchain blockchain = Blockchain.builder().network(Network.EMULATOR).contract(wallet).build();
    blockchain.deploy(30);
  }

  @Test
  public void testDeployV5ContractOnMyLocalTon() {
    TweetNaclFast.Signature.KeyPair keyPair = Utils.generateSignatureKeyPair();
    WalletV5 wallet =
        WalletV5.builder().keyPair(keyPair).walletId(42).isSigAuthAllowed(true).build();
    Blockchain blockchain =
        Blockchain.builder()
            .network(Network.MY_LOCAL_TON)
            .myLocalTonInstallationPath("G:/Git_Projects/MyLocalTon/myLocalTon")
            .contract(wallet)
            .build();
    blockchain.deploy(30);
  }

  @Test
  public void testDeployV5ContractOnTestnet() {
    TweetNaclFast.Signature.KeyPair keyPair = Utils.generateSignatureKeyPair();
    WalletV5 wallet =
        WalletV5.builder().keyPair(keyPair).walletId(42).isSigAuthAllowed(true).build();
    Blockchain blockchain = Blockchain.builder().network(Network.TESTNET).contract(wallet).build();
    blockchain.deploy(30);
  }

  @Test
  public void testDeployCustomContractContractOnEmulator() {

    Blockchain blockchain =
        Blockchain.builder()
            .network(Network.EMULATOR)
            .customContractAsResource("simple.fc")
            .customContractDataCell(
                CellBuilder.beginCell()
                    .storeUint(0, 32) // seqno
                    .storeInt(
                        Utils.getRandomInt(),
                        32) // unique integer, to make contract address random each time
                    .endCell())
            .build();
    blockchain.deploy(30);
  }

  @Test
  public void testDeployCustomContractContractOnTestnet() {

    Blockchain blockchain =
        Blockchain.builder()
            .network(Network.TESTNET)
            .customContractAsResource("simple.fc")
            .customContractDataCell(
                CellBuilder.beginCell()
                    .storeUint(0, 32) // seqno
                    .storeInt(Utils.getRandomInt(), 32)
                    .endCell())
            .build();
    blockchain.deploy(30);
  }

  @Test
  public void testDeployCustomContractContractWithBodyOnTestnet() {

    Blockchain blockchain =
        Blockchain.builder()
            .network(Network.TESTNET)
            .customContractAsResource("simple.fc")
            .customContractDataCell(
                CellBuilder.beginCell()
                    .storeUint(1, 32) // seqno
                    .storeInt(Utils.getRandomInt(), 32)
                    .endCell())
            .customContractBodyCell(
                CellBuilder.beginCell()
                    .storeUint(1, 32) // seqno
                    .endCell())
            .build();
    blockchain.deploy(30);
  }
}
