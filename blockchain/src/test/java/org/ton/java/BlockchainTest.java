package org.ton.java;

import static org.assertj.core.api.Assertions.assertThat;

import com.iwebpp.crypto.TweetNaclFast;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.ton.java.address.Address;
import org.ton.java.cell.CellBuilder;
import org.ton.java.smartcontract.faucet.TestnetFaucet;
import org.ton.java.smartcontract.types.WalletV3Config;
import org.ton.java.smartcontract.wallet.v3.WalletV3R2;
import org.ton.java.smartcontract.wallet.v5.WalletV5;
import org.ton.java.tlb.types.Message;
import org.ton.java.utils.Utils;

@Slf4j
@RunWith(JUnit4.class)
public class BlockchainTest {
  @Test
  public void testDeployV3R2ContractOnEmulator() {
    TweetNaclFast.Signature.KeyPair keyPair = Utils.generateSignatureKeyPair();
    WalletV3R2 wallet = WalletV3R2.builder().keyPair(keyPair).walletId(42).build();
    Blockchain blockchain = Blockchain.builder().network(Network.EMULATOR).contract(wallet).build();
    assertThat(blockchain.deploy(30)).isTrue();
  }

  @Test
  public void testDeployV5ContractOnEmulator() {
    TweetNaclFast.Signature.KeyPair keyPair = Utils.generateSignatureKeyPair();
    WalletV5 wallet =
        WalletV5.builder().keyPair(keyPair).walletId(42).isSigAuthAllowed(true).build();
    Blockchain blockchain = Blockchain.builder().network(Network.EMULATOR).contract(wallet).build();
    assertThat(blockchain.deploy(30)).isTrue();
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
    assertThat(blockchain.deploy(30)).isTrue();
  }

  @Test
  public void testDeployV5ContractOnTestnet() {
    TweetNaclFast.Signature.KeyPair keyPair = Utils.generateSignatureKeyPair();
    WalletV5 wallet =
        WalletV5.builder().keyPair(keyPair).walletId(42).isSigAuthAllowed(true).build();
    Blockchain blockchain = Blockchain.builder().network(Network.TESTNET).contract(wallet).build();
    assertThat(blockchain.deploy(30)).isTrue();
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
    assertThat(blockchain.deploy(30)).isTrue();
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
    assertThat(blockchain.deploy(30)).isTrue();
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
    assertThat(blockchain.deploy(30)).isTrue();
  }

  @Test
  public void testGetMethodsV3R2ContractOnEmulator() {
    TweetNaclFast.Signature.KeyPair keyPair = Utils.generateSignatureKeyPair();
    WalletV3R2 wallet = WalletV3R2.builder().keyPair(keyPair).walletId(42).build();
    Blockchain blockchain = Blockchain.builder().network(Network.EMULATOR).contract(wallet).build();
    assertThat(blockchain.deploy(30)).isTrue();
    GetterResult result = blockchain.runGetMethod("seqno");
    log.info("result {}", result);
    log.info("seqno {}", blockchain.runGetSeqNo());
    log.info("pubKey {}", blockchain.runGetPublicKey());
  }

  @Test
  public void testGetMethodsV5ContractOnEmulator() {
    TweetNaclFast.Signature.KeyPair keyPair = Utils.generateSignatureKeyPair();
    WalletV5 wallet =
        WalletV5.builder().keyPair(keyPair).walletId(42).isSigAuthAllowed(true).build();
    Blockchain blockchain = Blockchain.builder().network(Network.EMULATOR).contract(wallet).build();
    assertThat(blockchain.deploy(30)).isTrue();
    GetterResult result = blockchain.runGetMethod("seqno");
    log.info("result {}", result);
    log.info("seqno {}", blockchain.runGetSeqNo());
    log.info("pubKey {}", blockchain.runGetPublicKey());
    log.info("subWalletId {}", blockchain.runGetSubWalletId());
  }

  @Test
  public void testGetMethodsCustomContractOnEmulator() {
    Blockchain blockchain =
        Blockchain.builder()
            .network(Network.EMULATOR)
            .customContractAsResource("simple.fc")
            .customContractDataCell(
                CellBuilder.beginCell()
                    .storeUint(0, 32)
                    .storeInt(Utils.getRandomInt(), 32)
                    .endCell())
            .build();
    GetterResult result = blockchain.runGetMethod("unique");
    log.info("result {}", result);
    log.info("seqno {}", blockchain.runGetSeqNo());
  }

  @Test
  public void testGetMethodsV3R2ContractOnTestnet() {
    TweetNaclFast.Signature.KeyPair keyPair = Utils.generateSignatureKeyPair();
    WalletV3R2 wallet = WalletV3R2.builder().keyPair(keyPair).walletId(42).build();
    Blockchain blockchain = Blockchain.builder().network(Network.TESTNET).contract(wallet).build();
    assertThat(blockchain.deploy(30)).isTrue();
    GetterResult result = blockchain.runGetMethod("seqno");
    log.info("result {}", result);
    log.info("seqno {}", blockchain.runGetSeqNo());
    log.info("pubKey {}", blockchain.runGetPublicKey());
  }

  @Test
  public void testGetMethodsV5ContractOnTestnet() {
    TweetNaclFast.Signature.KeyPair keyPair = Utils.generateSignatureKeyPair();
    WalletV5 wallet =
        WalletV5.builder().keyPair(keyPair).walletId(42).isSigAuthAllowed(true).build();
    Blockchain blockchain = Blockchain.builder().network(Network.TESTNET).contract(wallet).build();
    assertThat(blockchain.deploy(30)).isTrue();
    GetterResult result = blockchain.runGetMethod("seqno");
    log.info("result {}", result);
    log.info("seqno {}", blockchain.runGetSeqNo());
    log.info("pubKey {}", blockchain.runGetPublicKey());
    log.info("subWalletId {}", blockchain.runGetSubWalletId());
  }

  @Test
  public void testGetMethodsCustomContractOnTestnet() {
    Blockchain blockchain =
        Blockchain.builder()
            .network(Network.TESTNET)
            .customContractAsResource("simple.fc")
            .customContractDataCell(
                CellBuilder.beginCell()
                    .storeUint(0, 32)
                    .storeInt(Utils.getRandomInt(), 32)
                    .endCell())
            .build();
    assertThat(blockchain.deploy(30)).isTrue();
    GetterResult result = blockchain.runGetMethod("unique");
    log.info("result {}", result);
    log.info("seqno {}", blockchain.runGetSeqNo());
  }

  @Test
  public void testGetMethodsCustomContractOnTestnetTolk() {
    Blockchain blockchain =
        Blockchain.builder()
            .network(Network.TESTNET)
            .customContractAsResource("simple.tolk")
            .customContractDataCell(
                CellBuilder.beginCell()
                    .storeUint(0, 32)
                    .storeInt(Utils.getRandomInt(), 32)
                    .endCell())
            .build();
    assertThat(blockchain.deploy(30)).isTrue();
    GetterResult result = blockchain.runGetMethod("unique");
    log.info("result {}", result);
    log.info("seqno {}", blockchain.runGetSeqNo());
  }

  @Test
  public void testSendMessageV3R2ContractOnTestnet() {
    TweetNaclFast.Signature.KeyPair keyPair = Utils.generateSignatureKeyPair();
    WalletV3R2 wallet = WalletV3R2.builder().keyPair(keyPair).walletId(42).build();
    Blockchain blockchain = Blockchain.builder().network(Network.TESTNET).contract(wallet).build();
    assertThat(blockchain.deploy(30)).isTrue();

    WalletV3Config configA =
        WalletV3Config.builder()
            .walletId(42)
            .seqno(blockchain.runGetSeqNo().longValue())
            .destination(Address.of(TestnetFaucet.FAUCET_ADDRESS_RAW))
            .amount(Utils.toNano(0.05))
            .comment("ton4j-test")
            .mode(3)
            .build();

    Message msg = wallet.prepareExternalMsg(configA);

    blockchain.sendExternal(msg);
  }

  @Test
  public void testSendMessageV3R2ContractOnEmulator() {
    TweetNaclFast.Signature.KeyPair keyPair = Utils.generateSignatureKeyPair();
    WalletV3R2 wallet = WalletV3R2.builder().keyPair(keyPair).walletId(42).build();
    Blockchain blockchain = Blockchain.builder().network(Network.EMULATOR).contract(wallet).build();
    assertThat(blockchain.deploy(30)).isTrue();

    WalletV3Config configA =
        WalletV3Config.builder()
            .walletId(42)
            .seqno(blockchain.runGetSeqNo().longValue())
            .destination(Address.of(TestnetFaucet.FAUCET_ADDRESS_RAW))
            .amount(Utils.toNano(0.05))
            .comment("ton4j-test")
            .mode(3)
            .build();

    Message msg = wallet.prepareExternalMsg(configA);

    blockchain.sendExternal(msg);
  }

  @Test
  public void testSendMessageCustomContractOnTestnetTolk() {
    Blockchain blockchain =
        Blockchain.builder()
            .network(Network.TESTNET)
            .customContractAsResource("simple.tolk")
            .customContractDataCell(
                CellBuilder.beginCell()
                    .storeUint(0, 32)
                    .storeInt(Utils.getRandomInt(), 32)
                    .endCell())
            .build();
    assertThat(blockchain.deploy(30)).isTrue();
    GetterResult result = blockchain.runGetMethod("unique");
    System.out.printf("result %s\n", result);
    System.out.printf("returned seqno %s\n", blockchain.runGetSeqNo());
  }
}
