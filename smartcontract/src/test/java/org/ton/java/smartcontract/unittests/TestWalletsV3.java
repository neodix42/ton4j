package org.ton.java.smartcontract.unittests;

import com.iwebpp.crypto.TweetNaclFast;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.ton.java.address.Address;
import org.ton.java.smartcontract.types.WalletV3Config;
import org.ton.java.smartcontract.utils.MsgUtils;
import org.ton.java.smartcontract.wallet.v3.WalletV3R2;
import org.ton.java.tlb.types.Message;
import org.ton.java.utils.Utils;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@Slf4j
@RunWith(JUnit4.class)
public class TestWalletsV3 {

    /**
     * test new-wallet-v3.fif R2
     * >fift -s new-wallet-v3.fif 0 698983191
     */
    @Test
    public void testNewWalletV3() {
        byte[] secretKey = Utils.hexToSignedBytes("F182111193F30D79D517F2339A1BA7C25FDF6C52142F0F2C1D960A1F1D65E1E4");
        TweetNaclFast.Signature.KeyPair keyPair = TweetNaclFast.Signature.keyPair_fromSeed(secretKey);

        WalletV3R2 contract = WalletV3R2.builder()
                .wc(0)
                .keyPair(keyPair)
                .walletId(698983191L)
                .build();

        assertThat(contract.getStateInit().getCode().getBits().toHex()).isEqualTo("FF0020DD2082014C97BA218201339CBAB19F71B0ED44D0D31FD31F31D70BFFE304E0A4F2608308D71820D31FD31FD31FF82313BBF263ED44D0D31FD31FD3FFD15132BAF2A15144BAF2A204F901541055F910F2A3F8009320D74A96D307D402FB00E8D101A4C8CB1FCB1FCBFFC9ED54");

        Message msg = MsgUtils.createExternalMessageWithSignedBody(contract.getKeyPair(), contract.getAddress(), contract.getStateInit(), null);
        Address address = msg.getInit().getAddress();

        String my = "Creating new advanced wallet in workchain " + contract.getWc() + "\n" +
                "with unique wallet id " + contract.getWalletId() + "\n" +
                "Loading private key from file new-wallet.pk" + "\n" +
                "StateInit: " + msg.getInit().toCell().print() + ", hex: " + msg.getInit().toCell().toHex() + "\n" +
                "StateInit.code: " + "hex: " + msg.getInit().getCode().toHex() + "\n" +
                "StateInit.data: " + "hex: " + msg.getInit().getCode().toHex() + "\n" +
                "message.body: " + "hex: " + msg.getBody().toString() + "\n" +
                "new wallet address = " + address.toString(false) + "\n" +
                "(Saving address to file new-wallet.addr)" + "\n" +
                "Non-bounceable address (for init): " + address.toString(true, true, false, true) + "\n" +
                "Bounceable address (for later access): " + address.toString(true, true, true, true) + "\n" +
                "signing message: " + msg.getBody().print() + ", hex: " + msg.getBody().toHex() + "\n" +
                "External message for initialization is " + msg.toCell().print() + "\n" +
                Utils.bytesToHex(msg.toCell().toBoc()).toUpperCase() + "\n" +
                "(Saved wallet creating query to file new-wallet-query.boc)" + "\n";
        log.info(my);

        String fiftOutput = "Creating new advanced v3 wallet in workchain 0\n" +
                "with unique wallet id 698983191\n" +
                "Loading private key from file new-wallet.pk\n" +
                "StateInit: x{34_}\n" +
                " x{FF0020DD2082014C97BA9730ED44D0D70B1FE0A4F2608308D71820D31FD31FD31FF82313BBF263ED44D0D31FD31FD3FFD15132BAF2A15144BAF2A204F901541055F910F2A3F8009320D74A96D307D402FB00E8D101A4C8CB1FCB1FCBFFC9ED54}\n" +
                " x{0000000029A9A31782A0B2543D06FEC0AAC952E9EC738BE56AB1B6027FC0C1AA817AE14B4D1ED2FB}\n" +
                "new wallet address = 0:2cf55953e92efbeadab7ba725c3f93a0b23f842cbba72d7b8e6f510a70e422e3\n" +
                "(Saving address to file new-wallet.addr)\n" +
                "Non-bounceable address (for init): 0QAs9VlT6S776tq3unJcP5Ogsj-ELLunLXuOb1EKcOQi4-QO\n" +
                "Bounceable address (for later access): kQAs9VlT6S776tq3unJcP5Ogsj-ELLunLXuOb1EKcOQi47nL\n" +
                "signing message: x{29A9A317FFFFFFFF00000000}\n" +
                "External message for initialization is x{880059EAB2A7D25DF7D5B56F74E4B87F2741647F0859774E5AF71CDEA214E1C845C6119529DEF4481C60CD81087FC7B058797AFDCEBCC1BE127EE2C4707C1E1C0F3D12F955EC3DE1C63E714876A931F6C6F13E6980284238AA9F94B0EC5859B37C4DE1E5353462FFFFFFFFE00000001_}\n" +
                " x{FF0020DD2082014C97BA9730ED44D0D70B1FE0A4F2608308D71820D31FD31FD31FF82313BBF263ED44D0D31FD31FD3FFD15132BAF2A15144BAF2A204F901541055F910F2A3F8009320D74A96D307D402FB00E8D101A4C8CB1FCB1FCBFFC9ED54}\n" +
                " x{0000000029A9A31782A0B2543D06FEC0AAC952E9EC738BE56AB1B6027FC0C1AA817AE14B4D1ED2FB}\n" +
                "B5EE9C72410203010001000002DF880059EAB2A7D25DF7D5B56F74E4B87F2741647F0859774E5AF71CDEA214E1C845C6119529DEF4481C60CD81087FC7B058797AFDCEBCC1BE127EE2C4707C1E1C0F3D12F955EC3DE1C63E714876A931F6C6F13E6980284238AA9F94B0EC5859B37C4DE1E5353462FFFFFFFFE000000010010200C0FF0020DD2082014C97BA9730ED44D0D70B1FE0A4F2608308D71820D31FD31FD31FF82313BBF263ED44D0D31FD31FD3FFD15132BAF2A15144BAF2A204F901541055F910F2A3F8009320D74A96D307D402FB00E8D101A4C8CB1FCB1FCBFFC9ED5400500000000029A9A31782A0B2543D06FEC0AAC952E9EC738BE56AB1B6027FC0C1AA817AE14B4D1ED2FB56AD484D\n" +
                "(Saved wallet creating query to file new-wallet-query.boc)";

        log.info(fiftOutput);
        assertThat(msg.getBody().getBits().toHex()).isEqualTo("29A9A317FFFFFFFF00000000");
        assertThat(msg.toCell().getBits().toHex()).isEqualTo("88009F4CFD8AB69CB20864160E3A40E4F578643B5B5B409C51A0215DA579D95E49F6119529DEF4481C60CD81087FC7B058797AFDCEBCC1BE127EE2C4707C1E1C0F3D12F955EC3DE1C63E714876A931F6C6F13E6980284238AA9F94B0EC5859B37C4DE1E5353462FFFFFFFFE00000001_");
        assertThat(address.toString(false)).isEqualTo("0:4fa67ec55b4e5904320b071d20727abc321dadada04e28d010aed2bcecaf24fb");
    }

    /**
     * test wallet-v3.fif
     */
    @Test
    public void testCreateTransferMessageWalletV3() {
        byte[] secretKey = Utils.hexToSignedBytes("F182111193F30D79D517F2339A1BA7C25FDF6C52142F0F2C1D960A1F1D65E1E4");
        TweetNaclFast.Signature.KeyPair keyPair = TweetNaclFast.Signature.keyPair_fromSeed(secretKey);

        WalletV3R2 contract = WalletV3R2.builder()
                .wc(0)
                .keyPair(keyPair)
                .walletId(698983191L)
                .build();

        WalletV3Config config = WalletV3Config.builder()
                .destination(Address.of("0:2cf55953e92efbeadab7ba725c3f93a0b23f842cbba72d7b8e6f510a70e422e3"))
                .amount(Utils.toNano(1))
                .seqno(0L)
                .build();

        Message msg = contract.prepareMsg(config);

        Address address = msg.getInit().getAddress();

        String my = "Creating new advanced wallet in workchain " + contract.getWc() + "\n" +
                "with unique wallet id " + contract.getWalletId() + "\n" +
                "Loading private key from file new-wallet.pk" + "\n" +
                "StateInit: " + msg.getInit().toCell().print() + "\n" +
                "new wallet address = " + address.toString(false) + "\n" +
                "(Saving address to file new-wallet.addr)" + "\n" +
                "Non-bounceable address (for init): " + address.toString(true, true, false, true) + "\n" +
                "Bounceable address (for later access): " + address.toString(true, true, true, true) + "\n" +
                "signing message: " + msg.getBody().print() + "\n" +
                "External message for initialization is " + msg.toCell().print() + "\n" +
                Utils.bytesToHex(msg.toCell().toBoc()).toUpperCase() + "\n" +
                "(Saved wallet creating query to file new-wallet-query.boc)" + "\n";
        log.info(my);
        assertThat(msg.getBody()).isNotNull();
    }
}
