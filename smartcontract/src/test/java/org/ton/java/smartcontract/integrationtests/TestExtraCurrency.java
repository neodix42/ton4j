package org.ton.java.smartcontract.integrationtests;

import static org.assertj.core.api.Assertions.assertThat;

import com.iwebpp.crypto.TweetNaclFast;
import java.math.BigInteger;
import java.util.Collections;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.ton.java.address.Address;
import org.ton.java.smartcontract.faucet.TestnetFaucet;
import org.ton.java.smartcontract.types.WalletV3Config;
import org.ton.java.smartcontract.wallet.v3.WalletV3R2;
import org.ton.java.tonlib.types.*;
import org.ton.java.utils.Utils;

@Slf4j
@RunWith(JUnit4.class)
public class TestExtraCurrency extends CommonTest {
  public String ecSwapAddress = "kQC_rkxBuZDwS81yvMSLzeXBNLCGFNofm0avwlMfNXCwoOgr";

  @Test
  public void testExtraCurrency() throws InterruptedException {

    TweetNaclFast.Signature.KeyPair keyPair = Utils.generateSignatureKeyPair();

    WalletV3R2 contract = WalletV3R2.builder().tonlib(tonlib).keyPair(keyPair).walletId(42).build();

    String nonBounceableAddress1 = contract.getAddress().toNonBounceable();
    String rawAddress = contract.getAddress().toRaw();

    log.info("    raw address: {}", rawAddress);
    log.info("pub-key {}", Utils.bytesToHex(contract.getKeyPair().getPublicKey()));

    BigInteger balance =
        TestnetFaucet.topUpContract(tonlib, Address.of(nonBounceableAddress1), Utils.toNano(4));
    log.info("topped up {}", Utils.formatNanoValue(balance));

    ExtMessageInfo extMessageInfo = contract.deploy();
    assertThat(extMessageInfo.getError().getCode()).isZero();
    contract.waitForDeployment();

    WalletV3Config config =
        WalletV3Config.builder()
            .walletId(42)
            .seqno(contract.getSeqno())
            .destination(Address.of(ecSwapAddress))
            .amount(Utils.toNano(3.7))
            .build();

    //  receive test extra-currency (ECHIDNA)
    extMessageInfo = contract.send(config);
    assertThat(extMessageInfo.getError().getCode()).isZero();

    contract.waitForBalanceChange();

    Utils.sleep(30);

    FullAccountState accountState = tonlib.getAccountState(Address.of(rawAddress));
    log.info("state {}", accountState);
    RawAccountState rawAccountState = tonlib.getRawAccountState(Address.of(rawAddress));
    log.info("rawState {}", rawAccountState);

    RawTransactions txs = tonlib.getRawTransactions(rawAddress, null, null);
    for (RawTransaction tx : txs.getTransactions()) {
      log.info("tx {}", tx);
    }

    log.info(
        "new extra-currency balance {} ECHIDNA",
        Utils.formatJettonValue(rawAccountState.getExtra_currencies().get(0).getAmount(), 8, 2));

    // send back extra-currency to ecSwap address
    config =
        WalletV3Config.builder()
            .walletId(42)
            .seqno(contract.getSeqno())
            .destination(Address.of(ecSwapAddress))
            .amount(Utils.toNano(0.1))
            .extraCurrencies(
                Collections.singletonList(
                    ExtraCurrency.builder().id(100).amount(BigInteger.valueOf(600000000)).build()))
            .build();

    extMessageInfo = contract.send(config);
    assertThat(extMessageInfo.getError().getCode()).isZero();

    contract.waitForBalanceChange();

    Utils.sleep(30);
    accountState = tonlib.getAccountState(Address.of(rawAddress));
    log.info("state {}", accountState);
    rawAccountState = tonlib.getRawAccountState(Address.of(rawAddress));
    log.info("rawState {}", rawAccountState);
  }
}
