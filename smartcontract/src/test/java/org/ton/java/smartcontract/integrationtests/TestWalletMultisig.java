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

        TweetNaclFast.Signature.KeyPair keyPair = Utils.generateSignatureKeyPair();
        TweetNaclFast.Signature.KeyPair keyPair2 = Utils.generateSignatureKeyPair();

        Options options = Options.builder()
                .publicKey(keyPair.getPublicKey())
                .walletId(new Random().nextLong() & 0xffffffffL)
                .multisigConfig(MultisigConfig.builder()
                        .k(2)
                        .n(3)
                        .rootI(0)
                        .owners(List.of(OwnerInfo.builder().build()))
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

        contract.deploy(tonlib, keyPair.getSecretKey());

        Utils.sleep(30, "deploying");

        log.info("owners publicKeys {}", contract.getPublicKeys(tonlib));


        List<OwnerInfo> ownersPublicKeys = List.of(
                OwnerInfo.builder()
                        .publicKey(keyPair.getPublicKey())
                        .flood(0)
                        .build(),
                OwnerInfo.builder()
                        .publicKey(keyPair2.getPublicKey())
                        .flood(1)
                        .build()
        );

        Cell stateInit = contract.createInitState(tonlib, 1, 2, 3, contract.createOwnersInfosDict(ownersPublicKeys));

        log.info("state-init {}", stateInit.toHex(false));

        balance = new BigInteger(tonlib.getAccountState(Address.of(bounceableAddress)).getBalance());
        log.info("new wallet {} balance: {}", contract.getName(), Utils.formatNanoValue(balance));
    }
}
