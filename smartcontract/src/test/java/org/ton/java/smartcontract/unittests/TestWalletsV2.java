package org.ton.java.smartcontract.unittests;

import com.iwebpp.crypto.TweetNaclFast;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.ton.java.address.Address;
import org.ton.java.smartcontract.types.ExternalMessage;
import org.ton.java.smartcontract.types.InitExternalMessage;
import org.ton.java.smartcontract.types.WalletVersion;
import org.ton.java.smartcontract.wallet.Options;
import org.ton.java.smartcontract.wallet.Wallet;
import org.ton.java.smartcontract.wallet.v2.WalletV2ContractR2;
import org.ton.java.utils.Utils;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@Slf4j
@RunWith(JUnit4.class)
public class TestWalletsV2 {

    /**
     * test new-wallet-v2.fif
     */
    @Test
    public void testNewWalletV2() {
        byte[] publicKey = Utils.hexToSignedBytes("82A0B2543D06FEC0AAC952E9EC738BE56AB1B6027FC0C1AA817AE14B4D1ED2FB");
        byte[] secretKey = Utils.hexToSignedBytes("F182111193F30D79D517F2339A1BA7C25FDF6C52142F0F2C1D960A1F1D65E1E4");
        TweetNaclFast.Signature.KeyPair keyPair = TweetNaclFast.Signature.keyPair_fromSeed(secretKey);

        Options options = Options.builder()
                .publicKey(keyPair.getPublicKey())
                .wc(0L)
                .build();

        Wallet wallet = new Wallet(WalletVersion.V2R2, options);
        WalletV2ContractR2 contract = wallet.create();

        assertThat(options.code.bits.toHex()).isEqualTo("FF0020DD2082014C97BA218201339CBAB19C71B0ED44D0D31FD70BFFE304E0A4F2608308D71820D31FD31F01F823BBF263ED44D0D31FD3FFD15131BAF2A103F901541042F910F2A2F800029320D74A96D307D402FB00E8D1A4C8CB1FCBFFC9ED54");

        InitExternalMessage msg = contract.createInitExternalMessage(keyPair.getSecretKey());
        Address address = msg.address;

        String my = "Creating new advanced wallet in workchain " + options.wc + "\n" +
                "Loading private key from file new-wallet.pk" + "\n" +
                "StateInit: " + msg.stateInit.print() + "\n" +
                "new wallet address = " + address.toString(false) + "\n" +
                "(Saving address to file new-wallet.addr)" + "\n" +
                "Non-bounceable address (for init): " + address.toString(true, true, false, true) + "\n" +
                "Bounceable address (for later access): " + address.toString(true, true, true, true) + "\n" +
                "signing message: " + msg.signingMessage.print() + "\n" +
                "External message for initialization is " + msg.message.print() + "\n" +
                Utils.bytesToHex(msg.message.toBoc()).toUpperCase() + "\n" +
                "(Saved wallet creating query to file new-wallet-query.boc)" + "\n";
        log.info(my);

        assertThat(address.toString(false)).isEqualTo("0:334e8f91f1cd72f83983768bc2cfbe24de6908d963d553e48e152fc5e20b1bbd");

        assertThat(msg.message.bits.toHex()).isEqualTo("8800669D1F23E39AE5F07306ED17859F7C49BCD211B2C7AAA7C91C2A5F8BC416377A119006018BC6501587E3EF5710DADC3E58439A9C8BDDAB55067E5C128C3EC1D29255FF7E266DDE2E6639333FBC20FB35A1FE2D2C94BA0D136E6973E87306ECFF40200000001FFFFFFFF_");
    }

    /**
     * test wallet-v2.fif
     */
    @Test
    public void testCreateTransferMessageWalletV2() {
        byte[] publicKey = Utils.hexToSignedBytes("82A0B2543D06FEC0AAC952E9EC738BE56AB1B6027FC0C1AA817AE14B4D1ED2FB");
        byte[] secretKey = Utils.hexToSignedBytes("F182111193F30D79D517F2339A1BA7C25FDF6C52142F0F2C1D960A1F1D65E1E4");
        TweetNaclFast.Signature.KeyPair keyPair = TweetNaclFast.Signature.keyPair_fromSeed(secretKey);

        Options options = Options.builder()
                .publicKey(keyPair.getPublicKey())
                .wc(0L)
                .build();

        Wallet wallet = new Wallet(WalletVersion.V2R2, options);
        WalletV2ContractR2 contract = wallet.create();

        assertThat(options.code.bits.toHex()).isEqualTo("FF0020DD2082014C97BA218201339CBAB19C71B0ED44D0D31FD70BFFE304E0A4F2608308D71820D31FD31F01F823BBF263ED44D0D31FD3FFD15131BAF2A103F901541042F910F2A2F800029320D74A96D307D402FB00E8D1A4C8CB1FCBFFC9ED54");

        ExternalMessage msg = contract.createTransferMessage(
                keyPair.getSecretKey(),
                "0:334e8f91f1cd72f83983768bc2cfbe24de6908d963d553e48e152fc5e20b1bbd",
                Utils.toNano(1), // gram
                1L);
        Address address = msg.address;

        String my = "Source wallet address =  " + address.toString(false) + "\n" +
                address.toString(true, true, true, true) + "\n" +
                "Loading private key from file new-wallet.pk" + "\n" +
                "Transferring GR$1. to account 0QAzTo-R8c1y-DmDdovCz74k3mkI2WPVU-SOFS_F4gsbvSPI = 0:334e8f91f1cd72f83983768bc2cfbe24de6908d963d553e48e152fc5e20b1bbd seqno=0x0 bounce=0\n" +
                "Body of transfer message is x{}" + "\n\n" +
                "signing message: " + msg.signingMessage.print() + "\n" +
                "resulting external message: " + msg.message.print() + "\n" +
                Utils.bytesToHex(msg.message.toBoc()).toUpperCase() + "\n" +
                "Query expires in 60 seconds\n" +
                "(Saved to file wallet-query.boc)\n";

        log.info(my);

        String fiftOutput = "Source wallet address = 0:334e8f91f1cd72f83983768bc2cfbe24de6908d963d553e48e152fc5e20b1bbd\n" +
                "kQAzTo-R8c1y-DmDdovCz74k3mkI2WPVU-SOFS_F4gsbvX4N\n" +
                "Loading private key from file new-wallet.pk\n" +
                "Transferring GR$1. to account 0QAzTo-R8c1y-DmDdovCz74k3mkI2WPVU-SOFS_F4gsbvSPI = 0:334e8f91f1cd72f83983768bc2cfbe24de6908d963d553e48e152fc5e20b1bbd seqno=0x0 bounce=0\n" +
                "Body of transfer message is x{}\n" +
                "signing message: x{00000000628BFA0103}\n" +
                " x{420019A747C8F8E6B97C1CC1BB45E167DF126F34846CB1EAA9F2470A97E2F1058DDEA1DCD6500000000000000000000000000000}\n" +
                "resulting external message: x{8800669D1F23E39AE5F07306ED17859F7C49BCD211B2C7AAA7C91C2A5F8BC416377A059314542963BC08ADA7973158C812CA993B60B34A8A95719176AE40EE047ED672205DB7B8F7A552C821B441D54F66E93DF8DDCA6D102FF3BA5FC917A0F05CB80800000003145FD0081C_}\n" +
                " x{420019A747C8F8E6B97C1CC1BB45E167DF126F34846CB1EAA9F2470A97E2F1058DDEA1DCD6500000000000000000000000000000}\n" +
                "B5EE9C724101020100A50001D78800669D1F23E39AE5F07306ED17859F7C49BCD211B2C7AAA7C91C2A5F8BC416377A059314542963BC08ADA7973158C812CA993B60B34A8A95719176AE40EE047ED672205DB7B8F7A552C821B441D54F66E93DF8DDCA6D102FF3BA5FC917A0F05CB80800000003145FD0081C010068420019A747C8F8E6B97C1CC1BB45E167DF126F34846CB1EAA9F2470A97E2F1058DDEA1DCD650000000000000000000000000000018CD4CCD\n" +
                "Query expires in 60 seconds\n" +
                "(Saved to file wallet-query.boc)";

        log.info(fiftOutput);

        //different due to a variable expiration timestamp
        //assertThat(msg.message.bits.toHex()).isEqualTo("8800669D1F23E39AE5F07306ED17859F7C49BCD211B2C7AAA7C91C2A5F8BC416377A059314542963BC08ADA7973158C812CA993B60B34A8A95719176AE40EE047ED672205DB7B8F7A552C821B441D54F66E93DF8DDCA6D102FF3BA5FC917A0F05CB80800000003145FD0081C_");
    }
}
