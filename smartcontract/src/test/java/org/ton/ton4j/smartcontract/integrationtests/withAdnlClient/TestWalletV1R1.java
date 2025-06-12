package org.ton.ton4j.smartcontract.integrationtests.withAdnlClient;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import com.iwebpp.crypto.TweetNaclFast;
import java.math.BigInteger;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.ton.java.adnl.AdnlLiteClient;
import org.ton.ton4j.address.Address;
import org.ton.ton4j.smartcontract.faucet.TestnetFaucet;
import org.ton.ton4j.smartcontract.types.WalletV1R1Config;
import org.ton.ton4j.smartcontract.wallet.v1.WalletV1R1;
import org.ton.ton4j.tonlib.types.ExtMessageInfo;
import org.ton.ton4j.utils.Utils;

@Slf4j
@RunWith(JUnit4.class)
public class TestWalletV1R1 {

  @Test
  public void testNewWalletV1R1AdnlClient() throws Exception {

    TweetNaclFast.Signature.KeyPair keyPair = Utils.generateSignatureKeyPair();
    AdnlLiteClient adnlLiteClient =
        AdnlLiteClient.builder()
            .configUrl(Utils.getGlobalConfigUrlTestnetGithub())
            .liteServerIndex(0)
            .build();

    WalletV1R1 contract =
        WalletV1R1.builder().adnlLiteClient(adnlLiteClient).wc(0).keyPair(keyPair).build();

    log.info("Wallet version {}", contract.getName());
    log.info("Wallet pub-key {}", Utils.bytesToHex(contract.getKeyPair().getPublicKey()));

    String nonBounceableAddress = contract.getAddress().toNonBounceable();
    String bounceableAddress = contract.getAddress().toBounceable();

    log.info("non-bounceable address {}", nonBounceableAddress);
    log.info("    bounceable address {}", bounceableAddress);
    log.info("           raw address {}", contract.getAddress().toString(false));

    // top up new wallet using test-faucet-wallet
    BigInteger balance =
        TestnetFaucet.topUpContract(
            adnlLiteClient, Address.of(nonBounceableAddress), Utils.toNano(0.1));
    log.info(
        "new faucet wallet {} balance: {}",
        contract.getAddress().toRaw(),
        Utils.formatNanoValue(balance));

    ExtMessageInfo extMessageInfo = contract.deploy();
    assertThat(extMessageInfo.getError().getCode()).isZero();
    contract.waitForDeployment(45);

    balance = contract.getBalance();
    log.info("    wallet balance: {}", Utils.formatNanoValue(balance));

    WalletV1R1Config config =
        WalletV1R1Config.builder()
            .seqno(1)
            .destination(Address.of(TestnetFaucet.BOUNCEABLE))
            .amount(Utils.toNano(0.08))
            .comment("testNewWalletV1R1")
            .build();

    // transfer coins from new wallet (back to faucet)
    extMessageInfo = contract.send(config);
    assertThat(extMessageInfo.getError().getCode()).isZero();

    contract.waitForBalanceChange(45);

    balance = contract.getBalance();
    log.info("new wallet balance: {}", Utils.formatNanoValue(balance));

    assertThat(balance.longValue()).isLessThan(Utils.toNano(0.02).longValue());
  }
}
