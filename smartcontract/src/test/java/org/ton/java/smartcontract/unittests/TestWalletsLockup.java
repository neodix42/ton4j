package org.ton.java.smartcontract.unittests;

import com.iwebpp.crypto.TweetNaclFast;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.ton.java.address.Address;
import org.ton.java.smartcontract.lockup.LockupWalletV1;
import org.ton.java.smartcontract.types.LockupConfig;
import org.ton.java.smartcontract.types.WalletVersion;
import org.ton.java.smartcontract.wallet.Options;
import org.ton.java.smartcontract.wallet.Wallet;
import org.ton.java.tlb.types.Message;
import org.ton.java.utils.Utils;

import java.util.List;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@Slf4j
@RunWith(JUnit4.class)
public class TestWalletsLockup {

    @Test
    public void testNewWalletLockup() {
        byte[] publicKey = Utils.hexToSignedBytes("82A0B2543D06FEC0AAC952E9EC738BE56AB1B6027FC0C1AA817AE14B4D1ED2FB");
        byte[] secretKey = Utils.hexToSignedBytes("F182111193F30D79D517F2339A1BA7C25FDF6C52142F0F2C1D960A1F1D65E1E4");
        TweetNaclFast.Signature.KeyPair keyPair = TweetNaclFast.Signature.keyPair_fromSeed(secretKey);

        Options options = Options.builder()
                .publicKey(keyPair.getPublicKey())
                .secretKey(keyPair.getSecretKey())
                .wc(0L)
                .lockupConfig(LockupConfig.builder()
                        .allowedDestinations(
                                List.of("Ef9eYuD_Mwol4jAtZ0lxZmhuv_92fvwzLW1hAFbJ657_iqRP",
                                        "kf_sPxv06KagKaRmOOKxeDQwApCx3i8IQOwv507XD51JOLka"))
                        .configPublicKey(Utils.bytesToHex(publicKey))
                        .build())
                .build();

        Wallet wallet = new Wallet(WalletVersion.lockup, options);
        LockupWalletV1 contract = wallet.create();

        Message msg = contract.createExternalMessage(contract.getAddress(), true, null);
        Address address = msg.getInit().getAddress();

        String info = "Creating lockup wallet in workchain " + options.wc + "\n" +
                "StateInit: " + msg.getInit().toCell().print() + "\n" +
                "new wallet address = " + address.toString(false) + "\n" +
                "Non-bounceable address (for init): " + address.toString(true, true, false, true) + "\n" +
                "Bounceable address (for later access): " + address.toString(true, true, true, true);
        log.info(info);

        assertThat(msg.getInit().getCode()).isNotNull();
    }
}
