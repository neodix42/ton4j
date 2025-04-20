package org.ton.java.smartcontract.integrationtests;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import com.iwebpp.crypto.TweetNaclFast;
import java.math.BigInteger;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.ton.java.address.Address;
import org.ton.java.smartcontract.faucet.TestnetFaucet;
import org.ton.java.smartcontract.types.WalletV2R1Config;
import org.ton.java.smartcontract.wallet.v2.WalletV2R1;
import org.ton.java.tonlib.types.ExtMessageInfo;
import org.ton.java.tonlib.types.RawTransaction;
import org.ton.java.utils.Utils;

@Slf4j
@RunWith(JUnit4.class)
public class TestWalletV2R1Short extends CommonTest {

  @Test
  public void testWalletV2R1() throws InterruptedException {

    TweetNaclFast.Signature.KeyPair keyPair = Utils.generateSignatureKeyPair();

    WalletV2R1 contract = WalletV2R1.builder().tonlib(tonlib).keyPair(keyPair).build();

    String nonBounceableAddress = contract.getAddress().toNonBounceable();
    String bounceableAddress = contract.getAddress().toBounceable();

    log.info("non-bounceable address {}", nonBounceableAddress);
    log.info("    bounceable address {}", bounceableAddress);

    // top up new wallet using test-faucet-wallet
    BigInteger balance =
        TestnetFaucet.topUpContract(tonlib, Address.of(nonBounceableAddress), Utils.toNano(1));
    log.info("new wallet {} balance: {}", contract.getName(), Utils.formatNanoValue(balance));

    ExtMessageInfo extMessageInfo = contract.deploy();
    assertThat(extMessageInfo.getError().getCode()).isZero();

    contract.waitForDeployment();

    // transfer coins from new wallet (back to faucet)
    WalletV2R1Config config =
        WalletV2R1Config.builder()
            .seqno(contract.getSeqno())
            .destination1(Address.of(TestnetFaucet.BOUNCEABLE))
            .amount1(Utils.toNano(0.1))
            .build();

    RawTransaction rawTransaction = contract.sendWithConfirmation(config);
    assertThat(rawTransaction).isNotNull();

    log.info("sending to 4 destinations...");
    config =
        WalletV2R1Config.builder()
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

    rawTransaction = contract.sendWithConfirmation(config);
    assertThat(rawTransaction).isNotNull();

    Utils.sleep(30);

    balance = contract.getBalance();
    log.info("new wallet {} balance: {}", contract.getName(), Utils.formatNanoValue(balance));
    assertThat(balance.longValue()).isLessThan(Utils.toNano(0.3).longValue());

    log.info("seqno {}", contract.getSeqno());
  }
}
