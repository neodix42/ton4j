package org.ton.java.smartcontract.integrationtests;

import com.iwebpp.crypto.TweetNaclFast;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.ton.java.address.Address;
import org.ton.java.cell.CellBuilder;
import org.ton.java.smartcontract.TestFaucet;
import org.ton.java.smartcontract.types.WalletV3Config;
import org.ton.java.smartcontract.types.WalletVersion;
import org.ton.java.smartcontract.wallet.Options;
import org.ton.java.smartcontract.wallet.Wallet;
import org.ton.java.smartcontract.wallet.v3.WalletV3ContractR1;
import org.ton.java.tlb.types.Message;
import org.ton.java.tonlib.Tonlib;
import org.ton.java.tonlib.types.ExtMessageInfo;
import org.ton.java.tonlib.types.VerbosityLevel;
import org.ton.java.utils.Utils;

import java.math.BigInteger;
import java.time.Instant;
import java.util.Date;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@Slf4j
@RunWith(JUnit4.class)
public class TestWalletV3R1DeployTransfer extends CommonTest {
    @Test
    public void testWalletV3R1() throws InterruptedException {

        tonlib = Tonlib.builder()
                .testnet(true)
                .ignoreCache(false)
                .verbosityLevel(VerbosityLevel.DEBUG)
                .build();

        TweetNaclFast.Signature.KeyPair keyPair = Utils.generateSignatureKeyPair();

        Options options = Options.builder()
                .publicKey(keyPair.getPublicKey())
                .secretKey(keyPair.getSecretKey())
                .walletId(42L)
                .wc(0L)
                .build();

        WalletV3ContractR1 contract = new Wallet(WalletVersion.V3R1, options).create();

        Message msg = contract.createExternalMessage(contract.getAddress(),
                true,
                CellBuilder.beginCell()
                        .storeUint(42, 32) // subwallet
                        .storeUint(Instant.now().getEpochSecond() + 5 * 60L, 32)  //valid-until
                        .storeUint(0, 32) //seqno
                        .endCell());
        Address address = msg.getInit().getAddress();

        String nonBounceableAddress = address.toString(true, true, false, true);
        String bounceableAddress = address.toString(true, true, true, true);

        String my = "Creating new wallet in workchain " + options.wc + "\n";
        my = my + "Loading private key from file new-wallet.pk" + "\n";
        my = my + "StateInit: " + msg.getInit().toCell().print() + "\n";
        my = my + "new wallet address = " + address.toString(false) + "\n";
        my = my + "(Saving address to file new-wallet.addr)" + "\n";
        my = my + "Non-bounceable address (for init): " + nonBounceableAddress + "\n";
        my = my + "Bounceable address (for later access): " + bounceableAddress + "\n";
        my = my + "signing message: " + msg.getBody().print() + "\n";
        my = my + "External message for initialization is " + msg.toCell().print() + "\n";
        my = my + Utils.bytesToHex(msg.getBody().toBoc()).toUpperCase() + "\n";
        my = my + "(Saved wallet creating query to file new-wallet-query.boc)" + "\n";
        log.info(my);

        // top up new wallet using test-faucet-wallet
        BigInteger balance = TestFaucet.topUpContract(tonlib, Address.of(nonBounceableAddress), Utils.toNano(1));
        log.info("new wallet {} balance: {}", contract.getName(), Utils.formatNanoValue(balance));

        // deploy new wallet
        ExtMessageInfo extMessageInfo = tonlib.sendRawMessage(msg.toCell().toBase64());
        assertThat(extMessageInfo.getError().getCode()).isZero();

        Utils.sleep(30, "deploying");

        // try to transfer coins from new wallet (back to faucet)
        WalletV3Config config = WalletV3Config.builder()
                .seqno(contract.getSeqno(tonlib))
                .subWalletId(42)
                .destination(Address.of(TestFaucet.BOUNCEABLE))
                .amount(Utils.toNano(0.8))
                .mode(3)
                .validUntil((long) (Math.floor(new Date().getTime() / 1e3) + 60))
                .comment("testWalletV3R1")
                .build();

        extMessageInfo = contract.sendTonCoins(tonlib, config);
        assertThat(extMessageInfo.getError().getCode()).isZero();

        Utils.sleep(40);

        balance = new BigInteger(tonlib.getAccountState(address).getBalance());
        log.info("new wallet {} balance: {}", contract.getName(), Utils.formatNanoValue(balance));
        assertThat(balance.longValue()).isLessThan(Utils.toNano(0.2).longValue());
    }
}
