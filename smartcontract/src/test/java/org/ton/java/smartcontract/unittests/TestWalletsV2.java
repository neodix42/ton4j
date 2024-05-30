package org.ton.java.smartcontract.unittests;

import com.iwebpp.crypto.TweetNaclFast;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.ton.java.address.Address;
import org.ton.java.smartcontract.types.WalletV2R2Config;
import org.ton.java.smartcontract.utils.MsgUtils;
import org.ton.java.smartcontract.wallet.v2.WalletV2R2;
import org.ton.java.tlb.types.Message;
import org.ton.java.utils.Utils;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@Slf4j
@RunWith(JUnit4.class)
public class TestWalletsV2 {
    /**
     * >fift -s new-wallet-v2.fif 0
     */
    @Test
    public void testNewWalletV2R2() {
        // echo "F182111193F30D79D517F2339A1BA7C25FDF6C52142F0F2C1D960A1F1D65E1E4" | xxd -r -p - > new-wallet.pk
        byte[] secretKey = Utils.hexToSignedBytes("F182111193F30D79D517F2339A1BA7C25FDF6C52142F0F2C1D960A1F1D65E1E4");
        TweetNaclFast.Signature.KeyPair keyPair = TweetNaclFast.Signature.keyPair_fromSeed(secretKey);

        WalletV2R2 contract = WalletV2R2.builder()
                .wc(0)
                .keyPair(keyPair)
                .build();

        String codeAsHex = contract.getStateInit().getCode().bitStringToHex();
        String dataAsHex = contract.getStateInit().getData().bitStringToHex();
        String rawAddress = contract.getAddress().toRaw();

        assertThat(codeAsHex).isEqualTo("FF0020DD2082014C97BA218201339CBAB19C71B0ED44D0D31FD70BFFE304E0A4F2608308D71820D31FD31F01F823BBF263ED44D0D31FD3FFD15131BAF2A103F901541042F910F2A2F800029320D74A96D307D402FB00E8D1A4C8CB1FCBFFC9ED54");
        assertThat(dataAsHex).isEqualTo("0000000082A0B2543D06FEC0AAC952E9EC738BE56AB1B6027FC0C1AA817AE14B4D1ED2FB");
        assertThat(rawAddress).isEqualTo("0:334e8f91f1cd72f83983768bc2cfbe24de6908d963d553e48e152fc5e20b1bbd");

        Message msg = contract.prepareDeployMsg();
        // external message for serialization
        assertThat(msg.toCell().bitStringToHex()).isEqualTo("8800669D1F23E39AE5F07306ED17859F7C49BCD211B2C7AAA7C91C2A5F8BC416377A119006018BC6501587E3EF5710DADC3E58439A9C8BDDAB55067E5C128C3EC1D29255FF7E266DDE2E6639333FBC20FB35A1FE2D2C94BA0D136E6973E87306ECFF40200000001FFFFFFFF_");
        // final boc
        assertThat(Utils.bytesToHex(msg.toCell().toBoc(true)).toUpperCase()).isEqualTo("B5EE9C724101030100F90002D78800669D1F23E39AE5F07306ED17859F7C49BCD211B2C7AAA7C91C2A5F8BC416377A119006018BC6501587E3EF5710DADC3E58439A9C8BDDAB55067E5C128C3EC1D29255FF7E266DDE2E6639333FBC20FB35A1FE2D2C94BA0D136E6973E87306ECFF40200000001FFFFFFFF0010200C2FF0020DD2082014C97BA218201339CBAB19C71B0ED44D0D31FD70BFFE304E0A4F2608308D71820D31FD31F01F823BBF263ED44D0D31FD3FFD15131BAF2A103F901541042F910F2A2F800029320D74A96D307D402FB00E8D1A4C8CB1FCBFFC9ED5400480000000082A0B2543D06FEC0AAC952E9EC738BE56AB1B6027FC0C1AA817AE14B4D1ED2FBC8A57A12");
    }

    /**
     * >fift -s wallet-v2.fif new-wallet 0:258e549638a6980ae5d3c76382afd3f4f32e34482dafc3751e3358589c8de00d 1 1
     */
    @Test
    public void testCreateTransferMessageWalletV2R2WithBounce() {
        byte[] secretKey = Utils.hexToSignedBytes("F182111193F30D79D517F2339A1BA7C25FDF6C52142F0F2C1D960A1F1D65E1E4");
        TweetNaclFast.Signature.KeyPair keyPair = TweetNaclFast.Signature.keyPair_fromSeed(secretKey);

        WalletV2R2 contract = WalletV2R2.builder()
                .wc(0)
                .keyPair(keyPair)
                .build();

        WalletV2R2Config config = WalletV2R2Config.builder()
                .destination1(Address.of("0:258e549638a6980ae5d3c76382afd3f4f32e34482dafc3751e3358589c8de00d"))
                .seqno(1L)
                .amount1(Utils.toNano(1))
                .bounce(true)
                .validUntil(1000)
                .build();

        Message msg = contract.prepareExternalMsg(config);
        // external message for serialization
        assertThat(msg.toCell().bitStringToHex()).isEqualTo("8800669D1F23E39AE5F07306ED17859F7C49BCD211B2C7AAA7C91C2A5F8BC416377A06F089B575DECC26124E3F4E729FF1A3CFE51AEAC3BD9943E39980533A295245D7E4DD122D3CCDA07E6E6FD9DC2C2BBBB410C143E6555E0C253781E9578CA950080000000800001F401C_");
        // external message in BoC format
        assertThat(Utils.bytesToHex(msg.toCell().toBoc(true)).toUpperCase()).isEqualTo("B5EE9C724101020100A50001D78800669D1F23E39AE5F07306ED17859F7C49BCD211B2C7AAA7C91C2A5F8BC416377A06F089B575DECC26124E3F4E729FF1A3CFE51AEAC3BD9943E39980533A295245D7E4DD122D3CCDA07E6E6FD9DC2C2BBBB410C143E6555E0C253781E9578CA950080000000800001F401C010068620012C72A4B1C534C0572E9E3B1C157E9FA79971A2416D7E1BA8F19AC2C4E46F006A1DCD65000000000000000000000000000004E41735C");
    }

    /**
     * >fift -s wallet-v2.fif new-wallet 0:258e549638a6980ae5d3c76382afd3f4f32e34482dafc3751e3358589c8de00d 1 1 -n -t 1000
     */
    @Test
    public void testCreateTransferMessageWalletV2R2NoBounce() {
        byte[] secretKey = Utils.hexToSignedBytes("F182111193F30D79D517F2339A1BA7C25FDF6C52142F0F2C1D960A1F1D65E1E4");
        TweetNaclFast.Signature.KeyPair keyPair = TweetNaclFast.Signature.keyPair_fromSeed(secretKey);

        WalletV2R2 contract = WalletV2R2.builder()
                .wc(0)
                .keyPair(keyPair)
                .build();

        WalletV2R2Config config = WalletV2R2Config.builder()
                .destination1(Address.of("0:258e549638a6980ae5d3c76382afd3f4f32e34482dafc3751e3358589c8de00d"))
                .seqno(1L)
                .amount1(Utils.toNano(1))
                .bounce(false)
                .validUntil(1000)
                .build();

        Message msg = contract.prepareExternalMsg(config);
        // external message for serialization
        assertThat(msg.toCell().bitStringToHex()).isEqualTo("8800669D1F23E39AE5F07306ED17859F7C49BCD211B2C7AAA7C91C2A5F8BC416377A07929F4590118FED68E4A880C580ED817E96EBBDB760E2DD942BAA5BC38AE32C0F9B0405BDF7B385E01A314224282958F180DA96443F852F85C117F0BFBB01A8400000000800001F401C_");
        // external message in BoC format
        assertThat(Utils.bytesToHex(msg.toCell().toBoc(true)).toUpperCase()).isEqualTo("B5EE9C724101020100A50001D78800669D1F23E39AE5F07306ED17859F7C49BCD211B2C7AAA7C91C2A5F8BC416377A07929F4590118FED68E4A880C580ED817E96EBBDB760E2DD942BAA5BC38AE32C0F9B0405BDF7B385E01A314224282958F180DA96443F852F85C117F0BFBB01A8400000000800001F401C010068420012C72A4B1C534C0572E9E3B1C157E9FA79971A2416D7E1BA8F19AC2C4E46F006A1DCD650000000000000000000000000000057DEA87E");
    }

    /**
     * >fift -s wallet-v2.fif new-wallet 0:258e549638a6980ae5d3c76382afd3f4f32e34482dafc3751e3358589c8de00d 1 1 -t 1000 -C gift
     */
    @Test
    public void testCreateTransferMessageWalletV2R2WithBounceAndComment() {
        byte[] secretKey = Utils.hexToSignedBytes("F182111193F30D79D517F2339A1BA7C25FDF6C52142F0F2C1D960A1F1D65E1E4");
        TweetNaclFast.Signature.KeyPair keyPair = TweetNaclFast.Signature.keyPair_fromSeed(secretKey);

        WalletV2R2 contract = WalletV2R2.builder()
                .wc(0)
                .keyPair(keyPair)
                .build();

        WalletV2R2Config config = WalletV2R2Config.builder()
                .destination1(Address.of("0:258e549638a6980ae5d3c76382afd3f4f32e34482dafc3751e3358589c8de00d"))
                .seqno(1L)
                .amount1(Utils.toNano(1))
                .bounce(true)
                .validUntil(1000)
                .body(MsgUtils.createTextMessageBody("gift"))
                .build();

        Message msg = contract.prepareExternalMsg(config);
        // external message for serialization
        assertThat(msg.toCell().bitStringToHex()).isEqualTo("8800669D1F23E39AE5F07306ED17859F7C49BCD211B2C7AAA7C91C2A5F8BC416377A079FC2D3275DAD59B116CC6B3BE16C22CBF91798DDFA0CD29ECC3BC0DA4288008B3CAA6C728442E0B7CBDE3D3D29C9D380184A597D7808C1C3940E308BC13A38200000000800001F401C_");
        // external message in BoC format
        assertThat(Utils.bytesToHex(msg.toCell().toBoc(true)).toUpperCase()).isEqualTo("B5EE9C724101020100AD0001D78800669D1F23E39AE5F07306ED17859F7C49BCD211B2C7AAA7C91C2A5F8BC416377A079FC2D3275DAD59B116CC6B3BE16C22CBF91798DDFA0CD29ECC3BC0DA4288008B3CAA6C728442E0B7CBDE3D3D29C9D380184A597D7808C1C3940E308BC13A38200000000800001F401C010078620012C72A4B1C534C0572E9E3B1C157E9FA79971A2416D7E1BA8F19AC2C4E46F006A1DCD6500000000000000000000000000000000000006769667480D93246");
    }
}
