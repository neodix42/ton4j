package org.ton.java.smartcontract.integrationtests;

import com.iwebpp.crypto.TweetNaclFast;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.ton.java.address.Address;
import org.ton.java.smartcontract.TestFaucet;
import org.ton.java.smartcontract.lockup.LockupWalletV1;
import org.ton.java.smartcontract.types.LockupConfig;
import org.ton.java.smartcontract.types.LockupWalletV1Config;
import org.ton.java.smartcontract.types.WalletVersion;
import org.ton.java.smartcontract.wallet.Options;
import org.ton.java.smartcontract.wallet.Wallet;
import org.ton.java.tlb.types.Message;
import org.ton.java.tonlib.types.ExtMessageInfo;
import org.ton.java.utils.Utils;

import java.io.IOException;
import java.math.BigInteger;
import java.util.List;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@Slf4j
@RunWith(JUnit4.class)
public class TestLockupWalletDeployTransfer extends CommonTest {

    @Test
    public void testNewWalletLockup() throws InterruptedException {
//        byte[] secondPublicKey = Utils.hexToBytes("82A0B2543D06FEC0AAC952E9EC738BE56AB1B6027FC0C1AA817AE14B4D1ED2FB");
//        byte[] secondSecretKey = Utils.hexToBytes("F182111193F30D79D517F2339A1BA7C25FDF6C52142F0F2C1D960A1F1D65E1E4");
        TweetNaclFast.Signature.KeyPair keyPair = Utils.generateSignatureKeyPair();

        Options options = Options.builder()
                .publicKey(keyPair.getPublicKey())
                .wc(0L)
                .lockupConfig(LockupConfig.builder()
                        .configPublicKey(Utils.bytesToHex(keyPair.getPublicKey()))
                        // important to specify totalRestrictedValue! otherwise wallet will send to prohibited addresses
                        // can be more than total balance wallet
                        .totalRestrictedValue(Utils.toNano(5000000))
                        .allowedDestinations(List.of(
                                TestFaucet.BOUNCEABLE,
                                "kf_YRLxA4Oe_e3FwvJ8CJgK9YDgeUprNQW3Or3B8ksegmjbj"))
                        .build())
                .build();


        Wallet wallet = new Wallet(WalletVersion.lockup, options);
        LockupWalletV1 contract = wallet.create();

        Message msg = contract.createExternalMessage(contract.getAddress(), true, null);
        Address address = msg.getInit().getAddress();

        String nonBounceableAddress = address.toString(true, true, false);
        String bounceableAddress = address.toString(true, true, true);

        log.info("non-bounceable address {}", nonBounceableAddress);
        log.info("    bounceable address {}", bounceableAddress);
        log.info("    bounceable address {}", address.toString(false));

        assertThat(msg.getInit().getCode()).isNotNull();


//        Tonlib tonlib = Tonlib.builder()
//                .testnet(true)
////                .pathToGlobalConfig("G:\\Git_Projects\\MyLocalTon\\myLocalTon\\genesis\\db\\my-ton-global.config.json")
//                .build();
        // top up new wallet using test-faucet-wallet
        BigInteger balance = TestFaucet.topUpContract(tonlib, Address.of(nonBounceableAddress), Utils.toNano(5));
        log.info("new {} wallet balance: {}", contract.getName(), Utils.formatNanoValue(balance));

        Utils.sleep(5);

        ExtMessageInfo extMessageInfo = contract.deploy(tonlib, LockupWalletV1Config.builder().build());
        assertThat(extMessageInfo.getError().getCode()).isZero();

        Utils.sleep(60, "deploying");

        log.info("seqno {}", contract.getSeqno(tonlib));
        log.info("sub-wallet id {}", contract.getWalletId(tonlib));
        log.info("public key {}", contract.getPublicKey(tonlib));

        log.info("liquid balance {}", Utils.formatNanoValue(contract.getLiquidBalance(tonlib)));
        log.info("restricted balance {}", Utils.formatNanoValue(contract.getNominalRestrictedBalance(tonlib)));
        log.info("time-locked balance {}", Utils.formatNanoValue(contract.getNominalLockedBalance(tonlib)));
        // below returns -1 - means true
        log.info("destination allowed {}", contract.check_destination(tonlib, TestFaucet.BOUNCEABLE));
        log.info("destination allowed {}", contract.check_destination(tonlib, "kf_YRLxA4Oe_e3FwvJ8CJgK9YDgeUprNQW3Or3B8ksegmjbj"));
        log.info("destination allowed {}", contract.check_destination(tonlib, "EQDZno6LOWYJRHPpRv-MM3qrhFPk6OHOxVOg1HvEEAtJxK3y"));

        // try to transfer coins from new lockup wallet to allowed address (back to faucet)
        log.info("sending toncoins to allowed address...");
        LockupWalletV1Config config = LockupWalletV1Config.builder()
                .destination(Address.of(TestFaucet.BOUNCEABLE))
                .amount(Utils.toNano(4))
                .comment("send-to-allowed-1")
                .build();
        contract.sendTonCoins(tonlib, config);
        Utils.sleep(70);

        balance = new BigInteger(tonlib.getAccountState(address).getBalance());
        log.info("new lockup wallet balance: {}", Utils.formatNanoValue(balance));
        assertThat(balance.longValue()).isLessThan(Utils.toNano(4).longValue());

        log.info("sending toncoins to prohibited address 1st time ...");
        config = LockupWalletV1Config.builder()
                .destination(Address.of("EQDZno6LOWYJRHPpRv-MM3qrhFPk6OHOxVOg1HvEEAtJxK3y"))
                .amount(Utils.toNano(1.5))
                .comment("send-to-prohibited-1")
                .build();
        contract.sendTonCoins(tonlib, config);
        Utils.sleep(70);

        log.info("liquid balance {}", Utils.formatNanoValue(contract.getLiquidBalance(tonlib)));
        log.info("restricted balance {}", Utils.formatNanoValue(contract.getNominalRestrictedBalance(tonlib)));
        log.info("time-locked balance {}", Utils.formatNanoValue(contract.getNominalLockedBalance(tonlib)));

        balance = new BigInteger(tonlib.getAccountState(address).getBalance());
        log.info("new lockup wallet balance: {}", Utils.formatNanoValue(balance));

        log.info("sending toncoins to prohibited address 2nd time ...");
        config = LockupWalletV1Config.builder()
                .destination(Address.of("0f_N_wfrFUwuWVkwpqmkRRYIJRzByJRobEwRCJTeQ8lq06n9"))
                .amount(Utils.toNano(1.6))
                .comment("send-to-prohibited-2")
                .build();
        contract.sendTonCoins(tonlib, config);
        Utils.sleep(70);

        log.info("liquid balance {}", Utils.formatNanoValue(contract.getLiquidBalance(tonlib)));
        log.info("restricted balance {}", Utils.formatNanoValue(contract.getNominalRestrictedBalance(tonlib)));
        log.info("time-locked balance {}", Utils.formatNanoValue(contract.getNominalLockedBalance(tonlib)));

        balance = new BigInteger(tonlib.getAccountState(address).getBalance());
        log.info("new lockup wallet balance: {}", Utils.formatNanoValue(balance));


        assertThat(balance.longValue()).isGreaterThan(Utils.toNano(0.9).longValue());

        log.info("sending toncoins to allowed address...");
        config = LockupWalletV1Config.builder()
                .destination(Address.of("kf_YRLxA4Oe_e3FwvJ8CJgK9YDgeUprNQW3Or3B8ksegmjbj"))
                .amount(Utils.toNano(0.5))
                .build();
        contract.sendTonCoins(tonlib, config);
        Utils.sleep(70);

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

        Options options = Options.builder()
                .publicKey(sigKeyPair.getPublicKey())
                .wc(-1L)
                .lockupConfig(LockupConfig.builder()
                        .configPublicKey(Utils.bytesToHex(sigKeyPair.getPublicKey())) // same as owner
                        .totalRestrictedValue(Utils.toNano(5_000_000))
                        .allowedDestinations(List.of(
                                elector.toString(),
                                myWallet.toString())
                        ).build())
                .build();


        Wallet wallet = new Wallet(WalletVersion.lockup, options);
        LockupWalletV1 contract = wallet.create();

        Message msg = contract.createExternalMessage(contract.getAddress(), true, null);
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

        ExtMessageInfo extMessageInfo = contract.deploy(tonlib, LockupWalletV1Config.builder().build());
        assertThat(extMessageInfo.getError().getCode()).isZero();

        Utils.sleep(30, "deploying");

        log.info("seqno {}", contract.getSeqno(tonlib));
        address.saveToFile("restricted-validator-wallet-001.addr");
    }
}
