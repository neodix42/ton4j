package org.ton.java.smartcontract.integrationtests;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import com.iwebpp.crypto.TweetNaclFast;
import java.math.BigInteger;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.ton.java.address.Address;
import org.ton.java.cell.Cell;
import org.ton.java.smartcontract.faucet.TestnetFaucet;
import org.ton.java.smartcontract.types.WalletV2R2Config;
import org.ton.java.smartcontract.wallet.v2.WalletV2R2;
import org.ton.java.tonlib.types.ExtMessageInfo;
import org.ton.java.utils.Utils;

@Slf4j
@RunWith(JUnit4.class)
public class TestWalletV2R2Short extends CommonTest {

  @Test
  public void testWalletV2R2() throws InterruptedException {
    TweetNaclFast.Signature.KeyPair keyPair = Utils.generateSignatureKeyPair();

    WalletV2R2 contract = WalletV2R2.builder().tonlib(tonlib).keyPair(keyPair).build();

    String nonBounceableAddress = contract.getAddress().toNonBounceable();
    String bounceableAddress = contract.getAddress().toBounceable();

    log.info("non-bounceable address {}", nonBounceableAddress);
    log.info("    bounceable address {}", bounceableAddress);
    log.info("pub-key {}", Utils.bytesToHex(contract.getKeyPair().getPublicKey()));
    log.info("prv-key {}", Utils.bytesToHex(contract.getKeyPair().getSecretKey()));

    // top up new wallet using test-faucet-wallet
    BigInteger balance =
        TestnetFaucet.topUpContract(tonlib, Address.of(nonBounceableAddress), Utils.toNano(1));
    log.info("new wallet {} balance: {}", contract.getName(), Utils.formatNanoValue(balance));

    ExtMessageInfo extMessageInfo = contract.deploy();
    assertThat(extMessageInfo.getError().getCode()).isZero();

    contract.waitForDeployment(20);

    // transfer coins from new wallet (back to faucet)
    WalletV2R2Config config =
        WalletV2R2Config.builder()
            .seqno(contract.getSeqno())
            .destination1(Address.of(TestnetFaucet.BOUNCEABLE))
            .amount1(Utils.toNano(0.1))
            .build();

    extMessageInfo = contract.send(config);
    assertThat(extMessageInfo.getError().getCode()).isZero();

    log.info("sending to one destination");
    contract.waitForBalanceChange(90);

    // multi send
    config =
        WalletV2R2Config.builder()
            .seqno(contract.getSeqno())
            .destination1(Address.of("EQA84DSUMyREa1Frp32wxFATnAVIXnWlYrbd3TFS1NLCbC-B"))
            .destination2(Address.of("EQCJZ3sJnes-o86xOa4LDDug6Lpz23RzyJ84CkTMIuVCCuan"))
            .destination3(Address.of("EQBjS7elE36MmEmE6-jbHQZNEEK0ObqRgaAxXWkx4pDGeefB"))
            .destination4(Address.of("EQAaGHUHfkpWFGs428ETmym4vbvRNxCA1o4sTkwqigKjgf-_"))
            .amount1(Utils.toNano(0.15))
            .amount2(Utils.toNano(0.15))
            .amount3(Utils.toNano(0.15))
            .amount4(Utils.toNano(0.15))
            .build();

    extMessageInfo = contract.send(config);
    assertThat(extMessageInfo.getError().getCode()).isZero();

    log.info("sending to four destinations");
    contract.waitForBalanceChange(90);

    balance = contract.getBalance();
    log.info("new wallet {} balance: {}", contract.getName(), Utils.formatNanoValue(balance));
    assertThat(balance.longValue()).isLessThan(Utils.toNano(0.3).longValue());
  }

  @Test
  public void testWalletSignedExternally() throws InterruptedException {
    TweetNaclFast.Signature.KeyPair keyPair = Utils.generateSignatureKeyPair();
    byte[] publicKey = keyPair.getPublicKey();

    WalletV2R2 contract = WalletV2R2.builder().tonlib(tonlib).publicKey(publicKey).build();
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
    WalletV2R2Config config =
        WalletV2R2Config.builder()
            .seqno(1)
            .destination1(Address.of(TestnetFaucet.BOUNCEABLE))
            .amount1(Utils.toNano(0.08))
            .comment("testWalletV2R2-signed-externally")
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
