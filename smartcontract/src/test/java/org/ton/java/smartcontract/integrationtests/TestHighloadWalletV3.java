package org.ton.java.smartcontract.integrationtests;

import com.iwebpp.crypto.TweetNaclFast;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.ton.java.address.Address;
import org.ton.java.smartcontract.TestFaucet;
import org.ton.java.smartcontract.highload.HighloadWalletV3;
import org.ton.java.smartcontract.types.HighloadQueryId;
import org.ton.java.smartcontract.types.HighloadV3Config;
import org.ton.java.smartcontract.types.WalletVersion;
import org.ton.java.smartcontract.wallet.Options;
import org.ton.java.smartcontract.wallet.Wallet;
import org.ton.java.utils.Utils;

import java.math.BigInteger;
import java.time.Instant;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@Slf4j
@RunWith(JUnit4.class)
public class TestHighloadWalletV3 extends CommonTest {

    @Test
    public void testSimple() throws InterruptedException {
        TweetNaclFast.Signature.KeyPair keyPair = Utils.generateSignatureKeyPair();

        Options options = Options.builder()
                .publicKey(keyPair.getPublicKey())
                .walletId(42L)
                .timeout(60 * 60)
                .wc(0L)
                .build();

        Wallet wallet = new Wallet(WalletVersion.highloadV3, options);
        HighloadWalletV3 contract = wallet.create();

        String nonBounceableAddress = contract.getAddress().toString(true, true, false);
        String bounceableAddress = contract.getAddress().toString(true, true, true);

        log.info("non-bounceable address {}", nonBounceableAddress);
        log.info("    bounceable address {}", bounceableAddress);
        log.info("           raw address {}", contract.getAddress().toString(false));

        // top up new wallet using test-faucet-wallet        
        BigInteger balance = TestFaucet.topUpContract(tonlib, Address.of(nonBounceableAddress), Utils.toNano(5));
        Utils.sleep(10, "topping up...");
        log.info("new wallet {} balance: {}", contract.getName(), Utils.formatNanoValue(balance));

        contract.deploy(tonlib, keyPair.getSecretKey());

        Utils.sleep(60, "deploying");

        HighloadV3Config config = HighloadV3Config
            .builder()
            .amount(BigInteger.valueOf(10000000))
            .body(null)
            .createdAt(Instant.now().getEpochSecond() - 10)
            .destination(Address.of("EQAyjRKDnEpTBNfRHqYdnzGEQjdY4KG3gxgqiG3DpDY46u8G"))
            .mode((byte) 3)
            .queryId(0)
            .build();

        contract.sendTonCoins(tonlib, keyPair.getSecretKey(), config);
    }

    @Test
    public void testHighloadQueryId() throws InterruptedException {
        HighloadQueryId qid = new HighloadQueryId();
        assertThat(qid.getQueryId()).isEqualTo(0);
        qid = qid.getNext();
        assertThat(qid.getQueryId()).isEqualTo(1);
        for (int i = 0; i < 1022; i++) {
            qid = qid.getNext();
        }
        assertThat(qid.getQueryId()).isEqualTo(1024);
        assertThat(qid.toSeqno()).isEqualTo(1023);

        qid = HighloadQueryId.fromShiftAndBitNumber(8191, 1020);
        assertThat(qid.hasNext()).isTrue();
        qid = qid.getNext();
        assertThat(qid.hasNext()).isFalse();

        int nqid = qid.getQueryId();
        qid = HighloadQueryId.fromSeqno(qid.toSeqno());
        assertThat(nqid).isEqualTo(qid.getQueryId());
        qid = HighloadQueryId.fromQueryId(qid.getQueryId());
        assertThat(nqid).isEqualTo(qid.getQueryId());
    }

}
