package org.ton.java.smartcontract.integrationtests;

import com.iwebpp.crypto.TweetNaclFast;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.ton.java.address.Address;
import org.ton.java.cell.CellBuilder;
import org.ton.java.smartcontract.TestFaucet;
import org.ton.java.smartcontract.lockup.LockupWalletV1;
import org.ton.java.smartcontract.types.LockupConfig;
import org.ton.java.smartcontract.types.LockupWalletV1Config;
import org.ton.java.smartcontract.utils.MsgUtils;
import org.ton.java.tlb.types.Message;
import org.ton.java.tonlib.types.ExtMessageInfo;
import org.ton.java.utils.Utils;

import java.io.IOException;
import java.math.BigInteger;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@Slf4j
@RunWith(JUnit4.class)
public class TestLockupWallet extends CommonTest {

    @Test
    public void testNewWalletLockup() throws InterruptedException {

        TweetNaclFast.Signature.KeyPair keyPair = Utils.generateSignatureKeyPair();

//        Options options = Options.builder()
//                .publicKey(keyPair.getPublicKey())
//                .secretKey(keyPair.getSecretKey())
//                .wc(0L)
//                .lockupConfig(LockupConfig.builder()
//                        .configPublicKey(Utils.bytesToHex(keyPair.getPublicKey()))
//                        // important to specify totalRestrictedValue! otherwise wallet will send to prohibited addresses
//                        // can be more than total balance wallet
//                        .totalRestrictedValue(Utils.toNano(5_000_000))
//                        .allowedDestinations(List.of(
//                                TestFaucet.BOUNCEABLE,
//                                "kf_YRLxA4Oe_e3FwvJ8CJgK9YDgeUprNQW3Or3B8ksegmjbj"))
//                        .build())
//                .build();


//        LockupWalletV1 contract = new Wallet(WalletVersion.lockup, options).create();

        LockupWalletV1 contract = LockupWalletV1.builder()
                .keyPair(keyPair)
                .lockupConfig(LockupConfig.builder()
                        .configPublicKey(Utils.bytesToHex(keyPair.getPublicKey()))
                        // important to specify totalRestrictedValue! otherwise wallet will send to prohibited addresses
                        // can be more than total balance wallet
                        .totalRestrictedValue(Utils.toNano(5_000_000))
                        .allowedDestinations(List.of(
                                TestFaucet.BOUNCEABLE,
                                "kf_YRLxA4Oe_e3FwvJ8CJgK9YDgeUprNQW3Or3B8ksegmjbj"))
                        .build())
                .build();

        Message msg = MsgUtils.createExternalMessageWithSignedBody(keyPair,
                contract.getAddress(), contract.getStateInit(),
                CellBuilder.beginCell()
                        .storeUint(contract.getWalletId(), 32) // subwallet-id
                        .storeUint(Instant.now().getEpochSecond() + 5 * 60L, 32) // valid-until
                        .storeUint(0, 32) // seqno
                        .endCell());
        Address address = msg.getInit().getAddress();

        String nonBounceableAddress = address.toString(true, true, false);
        String bounceableAddress = address.toString(true, true, true);

        log.info("non-bounceable address {}", nonBounceableAddress);
        log.info("    bounceable address {}", bounceableAddress);
        log.info("    bounceable address {}", address.toString(false));

        assertThat(msg.getInit().getCode()).isNotNull();

        // top up new wallet using test-faucet-wallet
        BigInteger balance = TestFaucet.topUpContract(tonlib, Address.of(nonBounceableAddress), Utils.toNano(5));
        log.info("new {} wallet balance: {}", contract.getName(), Utils.formatNanoValue(balance));

        Utils.sleep(5);

//        ExtMessageInfo extMessageInfo = contract.deploy(tonlib, config); // also valid deployment
        ExtMessageInfo extMessageInfo = tonlib.sendRawMessage(msg.toCell().toBase64());
        assertThat(extMessageInfo.getError().getCode()).isZero();

        Utils.sleep(60, "deploying");

        log.info("seqno {}", contract.getSeqno());
        log.info("sub-wallet id {}", contract.getWalletId(tonlib));
        log.info("public key {}", contract.getPublicKey(tonlib));

        log.info("liquid balance {}", Utils.formatNanoValue(contract.getLiquidBalance(tonlib)));
        log.info("restricted balance {}", Utils.formatNanoValue(contract.getNominalRestrictedBalance(tonlib)));
        log.info("time-locked balance {}", Utils.formatNanoValue(contract.getNominalLockedBalance(tonlib)));
        // below returns -1 - means true
        log.info("destination 1 allowed {}", contract.check_destination(tonlib, TestFaucet.BOUNCEABLE));
        log.info("destination 2 allowed {}", contract.check_destination(tonlib, "kf_YRLxA4Oe_e3FwvJ8CJgK9YDgeUprNQW3Or3B8ksegmjbj"));
        log.info("destination 3 allowed {}", contract.check_destination(tonlib, "EQDZno6LOWYJRHPpRv-MM3qrhFPk6OHOxVOg1HvEEAtJxK3y"));

        // try to transfer coins from new lockup wallet to allowed address (back to faucet)
        log.info("sending toncoins to allowed address...");
        LockupWalletV1Config config = LockupWalletV1Config.builder()
                .seqno(contract.getSeqno())
                .mode(3)
                .validUntil(Instant.now().getEpochSecond() + 5 * 60L)
                .destination(Address.of(TestFaucet.BOUNCEABLE))
                .amount(Utils.toNano(4))
                .comment("send-to-allowed-1")
                .build();

        extMessageInfo = contract.sendTonCoins(tonlib, config);
        assertThat(extMessageInfo.getError().getCode()).isZero();

        Utils.sleep(50);

        balance = new BigInteger(tonlib.getAccountState(address).getBalance());
        log.info("new lockup wallet balance: {}", Utils.formatNanoValue(balance));
        assertThat(balance.longValue()).isLessThan(Utils.toNano(4).longValue());

        log.info("sending toncoins to prohibited address 1st time ...");
        config = LockupWalletV1Config.builder()
                .seqno(contract.getSeqno())
                .mode(3)
                .validUntil(Instant.now().getEpochSecond() + 5 * 60L)
                .destination(Address.of("EQDZno6LOWYJRHPpRv-MM3qrhFPk6OHOxVOg1HvEEAtJxK3y"))
                .amount(Utils.toNano(1.5))
                .comment("send-to-prohibited-1")
                .build();
        extMessageInfo = contract.sendTonCoins(tonlib, config);
        assertThat(extMessageInfo.getError().getCode()).isZero();
        Utils.sleep(50);

        log.info("liquid balance {}", Utils.formatNanoValue(contract.getLiquidBalance(tonlib)));
        log.info("restricted balance {}", Utils.formatNanoValue(contract.getNominalRestrictedBalance(tonlib)));
        log.info("time-locked balance {}", Utils.formatNanoValue(contract.getNominalLockedBalance(tonlib)));

        balance = new BigInteger(tonlib.getAccountState(address).getBalance());
        log.info("new lockup wallet balance: {}", Utils.formatNanoValue(balance));

        log.info("sending toncoins to prohibited address 2nd time ...");
        config = LockupWalletV1Config.builder()
                .seqno(contract.getSeqno())
                .mode(3)
                .validUntil(Instant.now().getEpochSecond() + 5 * 60L)
                .destination(Address.of("0f_N_wfrFUwuWVkwpqmkRRYIJRzByJRobEwRCJTeQ8lq06n9"))
                .amount(Utils.toNano(1.6))
                .comment("send-to-prohibited-2")
                .build();
        extMessageInfo = contract.sendTonCoins(tonlib, config);
        assertThat(extMessageInfo.getError().getCode()).isZero();
        Utils.sleep(50);

        log.info("liquid balance {}", Utils.formatNanoValue(contract.getLiquidBalance(tonlib)));
        log.info("restricted balance {}", Utils.formatNanoValue(contract.getNominalRestrictedBalance(tonlib)));
        log.info("time-locked balance {}", Utils.formatNanoValue(contract.getNominalLockedBalance(tonlib)));

        balance = new BigInteger(tonlib.getAccountState(address).getBalance());
        log.info("new lockup wallet balance: {}", Utils.formatNanoValue(balance));


        assertThat(balance.longValue()).isGreaterThan(Utils.toNano(0.9).longValue());

        log.info("sending toncoins to allowed address...");
        config = LockupWalletV1Config.builder()
                .seqno(contract.getSeqno())
                .mode(3)
                .validUntil(Instant.now().getEpochSecond() + 5 * 60L)
                .destination(Address.of("kf_YRLxA4Oe_e3FwvJ8CJgK9YDgeUprNQW3Or3B8ksegmjbj"))
                .amount(Utils.toNano(0.5))
                .build();
        extMessageInfo = contract.sendTonCoins(tonlib, config);
        assertThat(extMessageInfo.getError().getCode()).isZero();
        Utils.sleep(50);

        log.info("liquid balance {}", Utils.formatNanoValue(contract.getLiquidBalance(tonlib)));
        log.info("restricted balance {}", Utils.formatNanoValue(contract.getNominalRestrictedBalance(tonlib)));
        log.info("time-locked balance {}", Utils.formatNanoValue(contract.getNominalLockedBalance(tonlib)));

        balance = new BigInteger(tonlib.getAccountState(address).getBalance());
        log.info("new lockup wallet balance: {}", Utils.formatNanoValue(balance));
        assertThat(balance.longValue()).isLessThan(Utils.toNano(0.7).longValue());
    }

    @Test
    public void testUseNewWalletLockup() throws IOException, InterruptedException {

        Address elector = Address.of("-1:3333333333333333333333333333333333333333333333333333333333333333");
        Address myWallet = Address.of("kf_YRLxA4Oe_e3FwvJ8CJgK9YDgeUprNQW3Or3B8ksegmjbj");

        TweetNaclFast.Box.KeyPair boxKeyPair = Utils.generateKeyPair();
        TweetNaclFast.Signature.KeyPair sigKeyPair = Utils.generateSignatureKeyPairFromSeed(boxKeyPair.getSecretKey());

        log.info("restricted-validator-wallet-001.pk {}", Utils.bytesToHex(boxKeyPair.getSecretKey()));

        // echo 'hex-prv-key' | xxd -r -p  > /usr/local/bin/mytoncore/wallets/restricted-validator-wallet-001.pk

//        Options options = Options.builder()
//                .publicKey(sigKeyPair.getPublicKey())
//                .secretKey(sigKeyPair.getSecretKey())
//                .wc(0L)
//                .lockupConfig(LockupConfig.builder()
//                        .configPublicKey(Utils.bytesToHex(sigKeyPair.getPublicKey())) // same as owner
//                        .totalRestrictedValue(Utils.toNano(5_000_000))
//                        .allowedDestinations(List.of(
//                                elector.toString(),
//                                myWallet.toString())
//                        ).build())
//                .build();


//        LockupWalletV1 contract = new Wallet(WalletVersion.lockup, options).create();

        LockupWalletV1 contract = LockupWalletV1.builder()
                .keyPair(sigKeyPair)
                .lockupConfig(LockupConfig.builder()
                        .configPublicKey(Utils.bytesToHex(sigKeyPair.getPublicKey())) // same as owner
                        .totalRestrictedValue(Utils.toNano(5_000_000))
                        .allowedDestinations(List.of(
                                elector.toString(),
                                myWallet.toString())
                        ).build())
                .build();

        Message msg = MsgUtils.createExternalMessageWithSignedBody(sigKeyPair, contract.getAddress(), contract.getStateInit(),
                CellBuilder.beginCell()
                        .storeUint(contract.getWalletId(), 32) // subwallet-id
                        .storeUint(Instant.now().getEpochSecond() + 60L, 32) // valid-until
                        .storeUint(0, 32) // seqno
                        .endCell());
        Address address = msg.getInit().getAddress();

        String nonBounceableAddress = address.toString(true, true, false, true);
        String bounceableAddress = address.toString(true, true, true, true);

        log.info("non-bounceable address {}", nonBounceableAddress);
        log.info("    bounceable address {}", bounceableAddress);


//        Tonlib tonlib = Tonlib.builder().testnet(true).build();
        // top up new wallet using test-faucet-wallet
        BigInteger balance = TestFaucet.topUpContract(tonlib, Address.of(nonBounceableAddress), Utils.toNano(5));
        log.info("new {} wallet balance: {}", contract.getName(), Utils.formatNanoValue(balance));

        Utils.sleep(5);

//        ExtMessageInfo extMessageInfo = contract.deploy(tonlib, LockupWalletV1Config.builder().build());
        ExtMessageInfo extMessageInfo = tonlib.sendRawMessage(msg.toCell().toBase64());

        assertThat(extMessageInfo.getError().getCode()).isZero();

        Utils.sleep(30, "deploying");

        log.info("seqno {}", contract.getSeqno());
        address.saveToFile("restricted-validator-wallet-001.addr");
    }
}
