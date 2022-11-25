package org.ton.java.smartcontract.integrationtests;

import com.iwebpp.crypto.TweetNaclFast;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.ton.java.address.Address;
import org.ton.java.cell.Cell;
import org.ton.java.smartcontract.TestFaucet;
import org.ton.java.smartcontract.multisig.MultisigWallet;
import org.ton.java.smartcontract.types.MultisigConfig;
import org.ton.java.smartcontract.types.OwnerInfo;
import org.ton.java.smartcontract.types.WalletVersion;
import org.ton.java.smartcontract.wallet.Options;
import org.ton.java.smartcontract.wallet.Wallet;
import org.ton.java.tonlib.Tonlib;
import org.ton.java.tonlib.types.VerbosityLevel;
import org.ton.java.utils.Utils;

import java.math.BigInteger;
import java.util.List;
import java.util.Random;

@Slf4j
@RunWith(JUnit4.class)
public class TestWalletMultisig {

    @Test
    public void testWalletMultisig() throws InterruptedException {

        Tonlib tonlib = Tonlib.builder()
                .testnet(true)
                .verbosityLevel(VerbosityLevel.DEBUG)
                .build();

        TweetNaclFast.Signature.KeyPair ownerKeyPair = Utils.generateSignatureKeyPair();
        TweetNaclFast.Signature.KeyPair keyPair2 = Utils.generateSignatureKeyPair();
        TweetNaclFast.Signature.KeyPair keyPair3 = Utils.generateSignatureKeyPair();
        TweetNaclFast.Signature.KeyPair keyPair4 = Utils.generateSignatureKeyPair();
        TweetNaclFast.Signature.KeyPair keyPair5 = Utils.generateSignatureKeyPair();

        log.info("pubKey0 {}", Utils.bytesToHex(ownerKeyPair.getPublicKey()));
        log.info("pubKey2 {}", Utils.bytesToHex(keyPair2.getPublicKey()));
        log.info("pubKey3 {}", Utils.bytesToHex(keyPair3.getPublicKey()));
        log.info("pubKey4 {}", Utils.bytesToHex(keyPair4.getPublicKey()));
        log.info("pubKey5 {}", Utils.bytesToHex(keyPair5.getPublicKey()));

        Options options = Options.builder()
                .publicKey(ownerKeyPair.getPublicKey())
                .walletId(new Random().nextLong() & 0xffffffffL)
                .multisigConfig(MultisigConfig.builder()
                        .k(3)
                        .n(5)
                        .rootI(0)
                        .owners(List.of(
                                OwnerInfo.builder()
                                        .publicKey(ownerKeyPair.getPublicKey())
                                        .flood(1)
                                        .build(),
                                OwnerInfo.builder()
                                        .publicKey(keyPair2.getPublicKey())
                                        .flood(2)
                                        .build(),
                                OwnerInfo.builder()
                                        .publicKey(keyPair3.getPublicKey())
                                        .flood(3)
                                        .build(),
                                OwnerInfo.builder()
                                        .publicKey(keyPair4.getPublicKey())
                                        .flood(4)
                                        .build(),
                                OwnerInfo.builder()
                                        .publicKey(keyPair5.getPublicKey())
                                        .flood(5)
                                        .build()
                        ))
                        .build())

                .build();

        Wallet wallet = new Wallet(WalletVersion.multisig, options);
        MultisigWallet contract = wallet.create();

        String nonBounceableAddress = contract.getAddress().toString(true, true, false);
        String bounceableAddress = contract.getAddress().toString(true, true, true);

        log.info("non-bounceable address {}", nonBounceableAddress);
        log.info("    bounceable address {}", bounceableAddress);

        // top up new wallet using test-faucet-wallet        
        BigInteger balance = TestFaucet.topUpContract(tonlib, Address.of(nonBounceableAddress), Utils.toNano(5));
        Utils.sleep(10, "topping up...");
        log.info("new wallet {} balance: {}", contract.getName(), Utils.formatNanoValue(balance));

        contract.deploy(tonlib, ownerKeyPair.getSecretKey());

        Utils.sleep(40, "deploying");

        log.info("owners publicKeys {}", contract.getPublicKeys(tonlib));

        // get init-state by input parameters
        List<OwnerInfo> ownersPublicKeys = List.of(
                OwnerInfo.builder()
                        .publicKey(ownerKeyPair.getPublicKey())
                        .flood(0)
                        .build(),
                OwnerInfo.builder()
                        .publicKey(keyPair2.getPublicKey())
                        .flood(1)
                        .build()
        );

        Cell stateInit = contract.getInitState(tonlib, 1, 2, 3, contract.createOwnersInfosDict(ownersPublicKeys));

        log.info("state-init {}", stateInit.toHex(false));

        balance = new BigInteger(tonlib.getAccountState(Address.of(bounceableAddress)).getBalance());
        log.info("new wallet {} balance: {}", contract.getName(), Utils.formatNanoValue(balance));
    }
}
