package org.ton.java.smartcontract.integrationtests;

import com.iwebpp.crypto.TweetNaclFast;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.ton.java.address.Address;
import org.ton.java.smartcontract.token.ft.JettonMinterStableCoin;
import org.ton.java.smartcontract.token.ft.JettonWalletStableCoin;
import org.ton.java.smartcontract.types.WalletV3Config;
import org.ton.java.smartcontract.utils.MsgUtils;
import org.ton.java.smartcontract.wallet.v3.WalletV3R2;
import org.ton.java.tonlib.Tonlib;
import org.ton.java.tonlib.types.ExtMessageInfo;
import org.ton.java.utils.Utils;

import java.math.BigInteger;

import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
@RunWith(JUnit4.class)
public class TestJettonStableCoinMainnet {
    public static final String USDT_MASTER_WALLET = "EQCxE6mUtQJKFnGfaROTKOt1lZbDiiX1kCixRv7Nw2Id_sDs";

    static Tonlib tonlib;


    @Test
    public void testJettonStableCoin() {
        // careful - mainnet
        tonlib = Tonlib.builder()
                .testnet(false)
                .ignoreCache(false)
//                .verbosityLevel(VerbosityLevel.DEBUG)
                .build();

        Address usdtMasterAddress = Address.of(USDT_MASTER_WALLET);

        //64 bytes private key of your wallet
        byte[] secretKey = Utils.hexToSignedBytes("");

        //use when you have 64 bytes private key
        TweetNaclFast.Signature.KeyPair keyPair = TweetNaclFast.Signature.keyPair_fromSecretKey(secretKey);

        //use when you have 32 bytes private key
        //TweetNaclFast.Signature.KeyPair keyPair = TweetNaclFast.Signature.keyPair_fromSeed(secretKey);


        // generate private key and get wallet address, that you top up later
        /*
        TweetNaclFast.Signature.KeyPair keyPair = Utils.generateSignatureKeyPair();
        */

        // use your wallet
        WalletV3R2 myWallet = WalletV3R2.builder()
                .tonlib(tonlib)
                .keyPair(keyPair)
                .walletId(42)
                .build();

        String nonBounceableAddress = myWallet.getAddress().toNonBounceable();
        String bounceableAddress = myWallet.getAddress().toBounceable();
        String rawAddress = myWallet.getAddress().toRaw();

        log.info("non-bounceable address: {}", nonBounceableAddress);
        log.info("    bounceable address: {}", bounceableAddress);
        log.info("    raw address: {}", rawAddress);
        log.info("pub-key {}", Utils.bytesToHex(myWallet.getKeyPair().getPublicKey()));
        log.info("prv-key {}", Utils.bytesToHex(myWallet.getKeyPair().getSecretKey()));

        String status = tonlib.getAccountStatus(Address.of(bounceableAddress));
        log.info("account status {}", status);

        String balance = tonlib.getAccountBalance(Address.of(bounceableAddress));
        log.info("account balance {}", Utils.formatNanoValue(balance));

        // myWallet.deploy();
        // myWallet.waitForDeployment(30);

        // get usdt jetton master (minter) address
        JettonMinterStableCoin usdtMasterWallet = JettonMinterStableCoin.builder()
                .tonlib(tonlib)
                .customAddress(usdtMasterAddress)
                .build();

        log.info("usdt total supply: {}", Utils.formatJettonValue(usdtMasterWallet.getTotalSupply(), 6, 2));

        // get my JettonWallet the one that holds my jettons (USDT) tokens
        JettonWalletStableCoin myJettonWallet = usdtMasterWallet.getJettonWallet(myWallet.getAddress());
        log.info("my jettonWallet balance: {}", Utils.formatJettonValue(myJettonWallet.getBalance(), 6, 2));

        // send my jettons to external address
        WalletV3Config walletV3Config = WalletV3Config.builder()
                .walletId(42)
                .seqno(myWallet.getSeqno())
                .destination(myJettonWallet.getAddress())
                .amount(Utils.toNano(0.02))
                .body(JettonWalletStableCoin.createTransferBody(
                        0,
                        BigInteger.valueOf(20000), // 2 cents
                        Address.of("EQDaVo6hnW_zf99qxX0fJGH4kOJbAVdXaJnJs4x37bNGJyTZ"), // recipient
                        null, // response address
                        BigInteger.ONE, // forward amount
                        MsgUtils.createTextMessageBody("gift")) // forward payload
                ).build();
        ExtMessageInfo extMessageInfo = myWallet.send(walletV3Config);
        assertThat(extMessageInfo.getError().getCode()).isZero();

        Utils.sleep(30, "transferring 0.02 USDT jettons to wallet EQDaVo6hnW_zf99qxX0fJGH4kOJbAVdXaJnJs4x37bNGJyTZ");
    }
}
