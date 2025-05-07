package org.ton.java.smartcontract.integrationtests;

import static org.assertj.core.api.Assertions.assertThat;

import com.iwebpp.crypto.TweetNaclFast;
import java.math.BigInteger;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.ton.java.address.Address;
import org.ton.java.cell.Cell;
import org.ton.java.smartcontract.faucet.TestnetFaucet;
import org.ton.java.smartcontract.types.WalletV3Config;
import org.ton.java.smartcontract.wallet.v3.WalletV3R1;
import org.ton.java.tonlib.types.ExtMessageInfo;
import org.ton.java.utils.Utils;

@Slf4j
public class TestWalletV3R1Short extends CommonTest {

  /*
   * addr - EQA-XwAkPLS-i4s9_N5v0CXGVFecw7lZV2rYeXDAimuWi9zI
   * pub key - 2c188d86ba469755554baad436663b8073145b29f117550432426c513e7c582a
   * prv key - c67cf48806f08929a49416ebebd97078100540ac8a3283646222b4d958b3e9e22c188d86ba469755554baad436663b8073145b29f117550432426c513e7c582a
   */
  @Test
  public void testWalletV3R1() throws InterruptedException {
    WalletV3R1 contract = WalletV3R1.builder().tonlib(tonlib).walletId(42).build();
    log.info("pub key: {}", Utils.bytesToHex(contract.getKeyPair().getPublicKey()));
    log.info("prv key: {}", Utils.bytesToHex(contract.getKeyPair().getSecretKey()));

    BigInteger balance =
        TestnetFaucet.topUpContract(tonlib, contract.getAddress(), Utils.toNano(0.1));
    log.info(
        "walletId {} new wallet {} balance: {}",
        contract.getWalletId(),
        contract.getName(),
        Utils.formatNanoValue(balance));

    ExtMessageInfo extMessageInfo = contract.deploy();
    log.info(extMessageInfo.toString());
    contract.waitForDeployment(60);
    // send toncoins
    WalletV3Config config =
        WalletV3Config.builder()
            .walletId(42)
            .seqno(contract.getSeqno())
            .destination(Address.of(TestnetFaucet.BOUNCEABLE))
            .amount(Utils.toNano(0.08))
            .comment("testWalletV3R1")
            .build();
    extMessageInfo = contract.send(config);
    log.info(extMessageInfo.toString());
  }

  @Test
  public void testWalletSignedExternally() throws InterruptedException {
    TweetNaclFast.Signature.KeyPair keyPair = Utils.generateSignatureKeyPair();
    byte[] publicKey = keyPair.getPublicKey();

    WalletV3R1 contract =
        WalletV3R1.builder().tonlib(tonlib).publicKey(publicKey).walletId(42).build();
    log.info("pub key: {}", Utils.bytesToHex(publicKey));

    BigInteger balance =
        TestnetFaucet.topUpContract(tonlib, contract.getAddress(), Utils.toNano(0.1));
    log.info(
        "walletId {} new wallet {} balance: {}",
        contract.getWalletId(),
        contract.getName(),
        Utils.formatNanoValue(balance));

    // deploy using externally signed body
    Cell deployBody = contract.createDeployMessage();

    byte[] signedDeployBodyHash =
        Utils.signData(keyPair.getPublicKey(), keyPair.getSecretKey(), deployBody.hash());

    ExtMessageInfo extMessageInfo = contract.deploy(signedDeployBodyHash);
    log.info("extMessageInfo {}", extMessageInfo);
    contract.waitForDeployment(120);

    // send toncoins
    WalletV3Config config =
        WalletV3Config.builder()
            .walletId(42)
            .seqno(contract.getSeqno())
            .destination(Address.of(TestnetFaucet.BOUNCEABLE))
            .amount(Utils.toNano(0.08))
            .comment("testWalletV3R1-signed-externally")
            .build();

    // transfer coins from new wallet (back to faucet) using externally signed body
    Cell transferBody = contract.createTransferBody(config);
    byte[] signedTransferBodyHash =
        Utils.signData(keyPair.getPublicKey(), keyPair.getSecretKey(), transferBody.hash());
    extMessageInfo = contract.send(config, signedTransferBodyHash);
    log.info("extMessageInfo: {}", extMessageInfo);
    contract.waitForBalanceChange(120);
    assertThat(contract.getBalance()).isLessThan(Utils.toNano(0.03));
  }
}
