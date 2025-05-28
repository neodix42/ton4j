package org.ton.ton4j.smartcontract.integrationtests;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import com.iwebpp.crypto.TweetNaclFast;
import java.math.BigInteger;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.ton.ton4j.address.Address;
import org.ton.ton4j.cell.Cell;
import org.ton.ton4j.smartcontract.faucet.TestnetFaucet;
import org.ton.ton4j.smartcontract.types.WalletV1R2Config;
import org.ton.ton4j.smartcontract.wallet.v1.WalletV1R2;
import org.ton.ton4j.tonlib.types.ExtMessageInfo;
import org.ton.ton4j.utils.Utils;

@Slf4j
@RunWith(JUnit4.class)
public class TestWalletV1R2 extends CommonTest {

  @Test
  public void testNewWalletV1R2() throws InterruptedException {

    WalletV1R2 contract = WalletV1R2.builder().tonlib(tonlib).initialSeqno(2).build();

    String nonBounceableAddress = contract.getAddress().toNonBounceable();
    String bounceableAddress = contract.getAddress().toBounceable();

    log.info("non-bounceable address {}", nonBounceableAddress);
    log.info("    bounceable address {}", bounceableAddress);
    log.info("           raw address {}", contract.getAddress().toString(false));

    // top up new wallet using test-faucet-wallet
    BigInteger balance =
        TestnetFaucet.topUpContract(tonlib, Address.of(nonBounceableAddress), Utils.toNano(0.1));
    log.info("wallet {} balance: {}", contract.getName(), Utils.formatNanoValue(balance));

    ExtMessageInfo extMessageInfo = contract.deploy();
    assertThat(extMessageInfo.getError().getCode()).isZero();

    contract.waitForDeployment(45);

    log.info("wallet seqno: {}", contract.getSeqno());

    WalletV1R2Config config =
        WalletV1R2Config.builder()
            .seqno(contract.getSeqno())
            .destination(Address.of(TestnetFaucet.BOUNCEABLE))
            .amount(Utils.toNano(0.08))
            .comment("testNewWalletV1R2")
            .build();

    // transfer coins from new wallet (back to faucet)
    extMessageInfo = contract.send(config);
    assertThat(extMessageInfo.getError().getCode()).isZero();

    contract.waitForBalanceChange(45);

    balance = contract.getBalance();
    log.info("wallet {} new balance: {}", contract.getName(), Utils.formatNanoValue(balance));
    log.info("wallet seqno: {}", contract.getSeqno());
    assertThat(balance.longValue()).isLessThan(Utils.toNano(0.02).longValue());
  }

  @Test
  public void testWalletSignedExternally() throws InterruptedException {
    TweetNaclFast.Signature.KeyPair keyPair = Utils.generateSignatureKeyPair();
    byte[] publicKey = keyPair.getPublicKey();

    WalletV1R2 contract = WalletV1R2.builder().tonlib(tonlib).publicKey(publicKey).build();
    log.info("pub key: {}", Utils.bytesToHex(publicKey));

    BigInteger balance =
        TestnetFaucet.topUpContract(tonlib, contract.getAddress(), Utils.toNano(0.1));
    log.info("new wallet {} balance: {}", contract.getName(), Utils.formatNanoValue(balance));

    // deploy using externally signed body
    Cell deployBody = contract.createDeployMessage();

    byte[] signedDeployBodyHash =
        Utils.signData(keyPair.getPublicKey(), keyPair.getSecretKey(), deployBody.hash());

    ExtMessageInfo extMessageInfo = contract.deploy(signedDeployBodyHash);
    log.info("extMessageInfo {}", extMessageInfo);
    contract.waitForDeployment(120);

    // send toncoins
    WalletV1R2Config config =
        WalletV1R2Config.builder()
            .seqno(1)
            .destination(Address.of(TestnetFaucet.BOUNCEABLE))
            .amount(Utils.toNano(0.08))
            .comment("testWalletV1R2-signed-externally")
            .build();

    // transfer coins from new wallet (back to faucet) using externally signed body
    Cell transferBody = contract.createTransferBody(config);
    byte[] signedTransferBodyHash =
        Utils.signData(keyPair.getPublicKey(), keyPair.getSecretKey(), transferBody.hash());
    extMessageInfo = contract.send(config, signedTransferBodyHash);
    log.info("extMessageInfo: {}", extMessageInfo);
    contract.waitForBalanceChange(120);
    Assertions.assertThat(contract.getBalance()).isLessThan(Utils.toNano(0.03));
  }
}
