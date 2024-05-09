package org.ton.java.smartcontract.integrationtests;

import com.iwebpp.crypto.TweetNaclFast;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.api.AssertionsForClassTypes;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.ton.java.address.Address;
import org.ton.java.smartcontract.TestFaucet;
import org.ton.java.smartcontract.types.WalletV3Config;
import org.ton.java.smartcontract.types.WalletVersion;
import org.ton.java.smartcontract.wallet.Options;
import org.ton.java.smartcontract.wallet.Wallet;
import org.ton.java.smartcontract.wallet.v3.WalletV3ContractR2;
import org.ton.java.tonlib.Tonlib;
import org.ton.java.tonlib.types.ExtMessageInfo;
import org.ton.java.tonlib.types.VerbosityLevel;
import org.ton.java.utils.Utils;

import java.math.BigInteger;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
@RunWith(JUnit4.class)
public class TestWalletV3R2DeployTransferShort extends CommonTest {

    @Test
    public void testWalletV3R2GetSeqno() {
        tonlib = Tonlib.builder()
                .testnet(false)
                .ignoreCache(false)
                .build();
        log.info("seqno {}", tonlib.getSeqno(Address.of("UQDUOClsfRg_f9zf96Vmeqba11KYqNXVhRqeO07E7SfxPB2H")));
    }

    @Test
    public void testWalletV3R2() throws InterruptedException {

        tonlib = Tonlib.builder()
                .testnet(true)
                .ignoreCache(false)
                .verbosityLevel(VerbosityLevel.DEBUG)
                .build();

        TweetNaclFast.Signature.KeyPair keyPair = Utils.generateSignatureKeyPair();

        Options options1 = Options.builder()
                .publicKey(keyPair.getPublicKey())
                .secretKey(keyPair.getSecretKey())
                .walletId(42L) //allows to create many wallets with the same public key (without risks of replaying messages between the wallets).
                .wc(0L)
                .build();

        WalletV3ContractR2 contract1 = new Wallet(WalletVersion.V3R2, options1).create();

        String nonBounceableAddress1 = contract1.getAddress().toString(true, true, false);
        String bounceableAddress1 = contract1.getAddress().toString(true, true, true);

        log.info("non-bounceable address 1: {}", nonBounceableAddress1);
        log.info("    bounceable address 1: {}", bounceableAddress1);
        String status = tonlib.getAccountStatus(Address.of(bounceableAddress1));
        log.info("account status {}", status);

        Options options2 = Options.builder()
                .publicKey(keyPair.getPublicKey())
                .secretKey(keyPair.getSecretKey())
                .walletId(98L)
                .wc(0L)
                .build();

        WalletV3ContractR2 contract2 = new Wallet(WalletVersion.V3R2, options2).create();

        String nonBounceableAddress2 = contract2.getAddress().toString(true, true, false);
        String bounceableAddress2 = contract2.getAddress().toString(true, true, true);

        log.info("non-bounceable address 2: {}", nonBounceableAddress2);
        log.info("    bounceable address 2: {}", bounceableAddress2);

        // top up new wallet using test-faucet-wallet
        BigInteger balance1 = TestFaucet.topUpContract(tonlib, Address.of(nonBounceableAddress1), Utils.toNano(1));
        log.info("walletId {} new wallet {} balance: {}", contract1.getWalletId(), contract1.getName(), Utils.formatNanoValue(balance1));

        BigInteger balance2 = TestFaucet.topUpContract(tonlib, Address.of(nonBounceableAddress2), Utils.toNano(1));
        log.info("walletId {} new wallet {} balance: {}", contract2.getWalletId(), contract2.getName(), Utils.formatNanoValue(balance2));


        Utils.sleep(15, "balance");

        WalletV3Config config = WalletV3Config.builder()
                .subWalletId(42)
                .build();
        ExtMessageInfo extMessageInfo = contract1.deploy(tonlib, config);
        assertThat(extMessageInfo.getError().getCode()).isZero();

        config = WalletV3Config.builder()
                .subWalletId(98)
                .build();

        extMessageInfo = contract2.deploy(tonlib, config);
        AssertionsForClassTypes.assertThat(extMessageInfo.getError().getCode()).isZero();

        Utils.sleep(30, "deploying");

        config = WalletV3Config.builder()
                .subWalletId(42)
                .seqno(contract1.getSeqno(tonlib))
                .destination(Address.of(TestFaucet.BOUNCEABLE))
                .amount(Utils.toNano(0.8))
                .mode(3)
                .validUntil((long) (Math.floor(new Date().getTime() / 1e3) + 60))
                .comment("testWalletV3R2-42")
                .build();

        // transfer coins from new wallet (back to faucet)
        extMessageInfo = contract1.sendTonCoins(tonlib, config);
        AssertionsForClassTypes.assertThat(extMessageInfo.getError().getCode()).isZero();
        Utils.sleep(30, "sending...");

        config = WalletV3Config.builder()
                .subWalletId(98)
                .seqno(contract2.getSeqno(tonlib))
                .destination(Address.of(TestFaucet.BOUNCEABLE))
                .amount(Utils.toNano(0.8))
                .mode(3)
                .validUntil((long) (Math.floor(new Date().getTime() / 1e3) + 60))
                .comment("testWalletV3R2-98")
                .build();

        extMessageInfo = contract2.sendTonCoins(tonlib, config);
        AssertionsForClassTypes.assertThat(extMessageInfo.getError().getCode()).isZero();
        Utils.sleep(30, "sending...");

        balance1 = new BigInteger(tonlib.getAccountState(Address.of(bounceableAddress1)).getBalance());
        log.info("walletId {} new wallet {} balance: {}", contract1.getWalletId(), contract1.getName(), Utils.formatNanoValue(balance1));

        balance2 = new BigInteger(tonlib.getAccountState(Address.of(bounceableAddress2)).getBalance());
        log.info("walletId {} new wallet {} balance: {}", contract2.getWalletId(), contract2.getName(), Utils.formatNanoValue(balance2));

        log.info("1 seqno {}", contract1.getSeqno(tonlib));
        log.info("1 pubkey {}", contract1.getPublicKey(tonlib));

        log.info("2 seqno {}", contract2.getSeqno(tonlib));
        log.info("2 pubkey {}", contract2.getPublicKey(tonlib));

        assertThat(contract1.getPublicKey(tonlib)).isEqualTo(contract2.getPublicKey(tonlib));
    }
}
