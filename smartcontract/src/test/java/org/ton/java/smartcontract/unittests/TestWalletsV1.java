package org.ton.java.smartcontract.unittests;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import com.iwebpp.crypto.TweetNaclFast;
import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Files;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.ton.java.address.Address;
import org.ton.java.mnemonic.Mnemonic;
import org.ton.java.mnemonic.Pair;
import org.ton.java.smartcontract.highload.HighloadWallet;
import org.ton.java.smartcontract.types.WalletV1R3Config;
import org.ton.java.smartcontract.utils.MsgUtils;
import org.ton.java.smartcontract.wallet.v1.WalletV1R1;
import org.ton.java.smartcontract.wallet.v1.WalletV1R2;
import org.ton.java.smartcontract.wallet.v1.WalletV1R3;
import org.ton.java.tlb.Message;
import org.ton.java.utils.Utils;

@Slf4j
@RunWith(JUnit4.class)
public class TestWalletsV1 {

  /** >fift -s new-wallet.fif 0 */
  @Test
  public void testNewWalletV1R3() {
    // echo "F182111193F30D79D517F2339A1BA7C25FDF6C52142F0F2C1D960A1F1D65E1E4" | xxd -r -p - >
    // new-wallet.pk
    byte[] secretKey =
        Utils.hexToSignedBytes("F182111193F30D79D517F2339A1BA7C25FDF6C52142F0F2C1D960A1F1D65E1E4");
    TweetNaclFast.Signature.KeyPair keyPair = TweetNaclFast.Signature.keyPair_fromSeed(secretKey);

    WalletV1R3 contract = WalletV1R3.builder().wc(0).keyPair(keyPair).build();

    String codeAsHex = contract.getStateInit().getCode().bitStringToHex();
    String dataAsHex = contract.getStateInit().getData().bitStringToHex();
    String rawAddress = contract.getAddress().toRaw();

    assertThat(codeAsHex)
        .isEqualTo(
            "FF0020DD2082014C97BA218201339CBAB19C71B0ED44D0D31FD70BFFE304E0A4F260810200D71820D70B1FED44D0D31FD3FFD15112BAF2A122F901541044F910F2A2F80001D31F3120D74A96D307D402FB00DED1A4C8CB1FCBFFC9ED54");
    assertThat(dataAsHex)
        .isEqualTo("0000000082A0B2543D06FEC0AAC952E9EC738BE56AB1B6027FC0C1AA817AE14B4D1ED2FB");
    assertThat(rawAddress)
        .isEqualTo("0:258e549638a6980ae5d3c76382afd3f4f32e34482dafc3751e3358589c8de00d");

    Message msg = contract.prepareDeployMsg();
    // external message for serialization
    assertThat(msg.toCell().bitStringToHex())
        .isEqualTo(
            "88004B1CA92C714D3015CBA78EC7055FA7E9E65C68905B5F86EA3C66B0B1391BC01A11900AF60938844B0DDEE0D7F5C6C6B55C2D7F661170E029B8978AACCD402F2FF03FDD08D94398DB0826DA42FA96A6CBA73D232370025BF544D3A954208C990600A00000001_");
    // final boc
    assertThat(Utils.bytesToHex(msg.toCell().toBoc(true)).toUpperCase())
        .isEqualTo(
            "B5EE9C724101030100F10002CF88004B1CA92C714D3015CBA78EC7055FA7E9E65C68905B5F86EA3C66B0B1391BC01A11900AF60938844B0DDEE0D7F5C6C6B55C2D7F661170E029B8978AACCD402F2FF03FDD08D94398DB0826DA42FA96A6CBA73D232370025BF544D3A954208C990600A000000010010200BAFF0020DD2082014C97BA218201339CBAB19C71B0ED44D0D31FD70BFFE304E0A4F260810200D71820D70B1FED44D0D31FD3FFD15112BAF2A122F901541044F910F2A2F80001D31F3120D74A96D307D402FB00DED1A4C8CB1FCBFFC9ED5400480000000082A0B2543D06FEC0AAC952E9EC738BE56AB1B6027FC0C1AA817AE14B4D1ED2FB111EE9AE");
  }

  /**
   * >fift -s wallet.fif new-wallet
   * 0:258e549638a6980ae5d3c76382afd3f4f32e34482dafc3751e3358589c8de00d 1 1
   */
  @Test
  public void testCreateTransferMessageWalletV1R3WithBounce() {
    byte[] secretKey =
        Utils.hexToSignedBytes("F182111193F30D79D517F2339A1BA7C25FDF6C52142F0F2C1D960A1F1D65E1E4");
    TweetNaclFast.Signature.KeyPair keyPair = TweetNaclFast.Signature.keyPair_fromSeed(secretKey);

    WalletV1R3 contract = WalletV1R3.builder().wc(0).keyPair(keyPair).build();

    WalletV1R3Config config =
        WalletV1R3Config.builder()
            .destination(
                Address.of("0:258e549638a6980ae5d3c76382afd3f4f32e34482dafc3751e3358589c8de00d"))
            .seqno(1L)
            .amount(Utils.toNano(1))
            .bounce(true)
            .build();

    Message msg = contract.prepareExternalMsg(config);
    // external message for serialization
    assertThat(msg.toCell().bitStringToHex())
        .isEqualTo(
            "88004B1CA92C714D3015CBA78EC7055FA7E9E65C68905B5F86EA3C66B0B1391BC01A006AA620FE8C8C8A6FCC86C8D92F9B2B9D34F84A0D5CF090CBFD922D795AB6C40E6D7F58481574B29674FF06F5F79401A2E230009A50C63FB6DB045A16E44D8818000000081C_");
    // external message in BoC format
    assertThat(Utils.bytesToHex(msg.toCell().toBoc(true)).toUpperCase())
        .isEqualTo(
            "B5EE9C724101020100A10001CF88004B1CA92C714D3015CBA78EC7055FA7E9E65C68905B5F86EA3C66B0B1391BC01A006AA620FE8C8C8A6FCC86C8D92F9B2B9D34F84A0D5CF090CBFD922D795AB6C40E6D7F58481574B29674FF06F5F79401A2E230009A50C63FB6DB045A16E44D8818000000081C010068620012C72A4B1C534C0572E9E3B1C157E9FA79971A2416D7E1BA8F19AC2C4E46F006A1DCD6500000000000000000000000000000426D429D");
  }

  /**
   * >fift -s wallet.fif new-wallet
   * 0:258e549638a6980ae5d3c76382afd3f4f32e34482dafc3751e3358589c8de00d 1 1 -n
   */
  @Test
  public void testCreateTransferMessageWalletV1R3NoBounce() {
    byte[] secretKey =
        Utils.hexToSignedBytes("F182111193F30D79D517F2339A1BA7C25FDF6C52142F0F2C1D960A1F1D65E1E4");
    TweetNaclFast.Signature.KeyPair keyPair = TweetNaclFast.Signature.keyPair_fromSeed(secretKey);

    WalletV1R3 contract = WalletV1R3.builder().wc(0).keyPair(keyPair).build();

    WalletV1R3Config config =
        WalletV1R3Config.builder()
            .destination(
                Address.of("0:258e549638a6980ae5d3c76382afd3f4f32e34482dafc3751e3358589c8de00d"))
            .seqno(1L)
            .amount(Utils.toNano(1))
            .bounce(false)
            .build();

    Message msg = contract.prepareExternalMsg(config);
    // external message for serialization
    assertThat(msg.toCell().bitStringToHex())
        .isEqualTo(
            "88004B1CA92C714D3015CBA78EC7055FA7E9E65C68905B5F86EA3C66B0B1391BC01A046F5F6B2149017D1A00412E7B7D6D6191A312A4C7538B5E336D5FC197C83CAA9B8EF152B4177FDE30C2A53641339FB84BD26995129046FDC7E61CE1A8FD11E870000000081C_");
    // external message in BoC format
    assertThat(Utils.bytesToHex(msg.toCell().toBoc(true)).toUpperCase())
        .isEqualTo(
            "B5EE9C724101020100A10001CF88004B1CA92C714D3015CBA78EC7055FA7E9E65C68905B5F86EA3C66B0B1391BC01A046F5F6B2149017D1A00412E7B7D6D6191A312A4C7538B5E336D5FC197C83CAA9B8EF152B4177FDE30C2A53641339FB84BD26995129046FDC7E61CE1A8FD11E870000000081C010068420012C72A4B1C534C0572E9E3B1C157E9FA79971A2416D7E1BA8F19AC2C4E46F006A1DCD6500000000000000000000000000000BD829330");
  }

  /**
   * >fift -s wallet.fif new-wallet
   * 0:258e549638a6980ae5d3c76382afd3f4f32e34482dafc3751e3358589c8de00d 1 1 -C gift
   */
  @Test
  public void testCreateTransferMessageWalletV1R3WithBounceAndComment() {
    byte[] secretKey =
        Utils.hexToSignedBytes("F182111193F30D79D517F2339A1BA7C25FDF6C52142F0F2C1D960A1F1D65E1E4");
    TweetNaclFast.Signature.KeyPair keyPair = TweetNaclFast.Signature.keyPair_fromSeed(secretKey);

    WalletV1R3 contract = WalletV1R3.builder().wc(0).keyPair(keyPair).build();

    WalletV1R3Config config =
        WalletV1R3Config.builder()
            .destination(
                Address.of("0:258e549638a6980ae5d3c76382afd3f4f32e34482dafc3751e3358589c8de00d"))
            .seqno(1L)
            .amount(Utils.toNano(1))
            .bounce(true)
            .comment("gift")
            .build();

    Message msg = contract.prepareExternalMsg(config);
    // external message for serialization
    assertThat(msg.toCell().bitStringToHex())
        .isEqualTo(
            "88004B1CA92C714D3015CBA78EC7055FA7E9E65C68905B5F86EA3C66B0B1391BC01A01379865D586C84313606E3416D43112CA38BAAD0B11E1169F16874DD5191367619613A6475E3736142E25D697EFA00DDA7B10B2309980168FFA34363D224E2078000000081C_");
    // external message in BoC format
    assertThat(Utils.bytesToHex(msg.toCell().toBoc(true)).toUpperCase())
        .isEqualTo(
            "B5EE9C724101020100A90001CF88004B1CA92C714D3015CBA78EC7055FA7E9E65C68905B5F86EA3C66B0B1391BC01A01379865D586C84313606E3416D43112CA38BAAD0B11E1169F16874DD5191367619613A6475E3736142E25D697EFA00DDA7B10B2309980168FFA34363D224E2078000000081C010078620012C72A4B1C534C0572E9E3B1C157E9FA79971A2416D7E1BA8F19AC2C4E46F006A1DCD65000000000000000000000000000000000000067696674B66E00DC");
  }

  @Test
  public void testNewWalletV1R1() {
    byte[] secretKey =
        Utils.hexToSignedBytes("F182111193F30D79D517F2339A1BA7C25FDF6C52142F0F2C1D960A1F1D65E1E4");
    TweetNaclFast.Signature.KeyPair keyPair = TweetNaclFast.Signature.keyPair_fromSeed(secretKey);

    WalletV1R1 contract = WalletV1R1.builder().wc(0).keyPair(keyPair).build();
    assertThat(contract.getAddress()).isNotNull();
  }

  @Test
  public void testNewWalletV1R2() {
    WalletV1R2 contract = WalletV1R2.builder().wc(0).build();
    assertThat(contract.getAddress()).isNotNull();
    log.info("pubKey " + Utils.bytesToHex(contract.getKeyPair().getPublicKey()));
    log.info("secKey " + Utils.bytesToHex(contract.getKeyPair().getSecretKey()));
  }

  @Test
  public void testNewWalletV1R2Mnemonic() throws NoSuchAlgorithmException, InvalidKeyException {
    List<String> mnemonic = Mnemonic.generate(24);
    Pair keyPair = Mnemonic.toKeyPair(mnemonic);

    log.info("pubKey " + Utils.bytesToHex(keyPair.getPublicKey()));
    log.info("secKey " + Utils.bytesToHex(keyPair.getSecretKey()));

    TweetNaclFast.Signature.KeyPair keyPairSig =
        TweetNaclFast.Signature.keyPair_fromSeed(keyPair.getSecretKey());

    log.info("pubKey " + Utils.bytesToHex(keyPairSig.getPublicKey()));
    log.info("secKey " + Utils.bytesToHex(keyPairSig.getSecretKey()));

    WalletV1R1 contract = WalletV1R1.builder().wc(0).keyPair(keyPairSig).build();
    assertThat(contract.getAddress()).isNotNull();

    Message msg =
        MsgUtils.createExternalMessageWithSignedBody(
            keyPairSig, contract.getAddress(), contract.getStateInit(), null);
    log.info("msg {}", msg.getInit().getAddress().toString(false));
  }

  @Test
  public void testGenerateFromMnemonic()
      throws NoSuchAlgorithmException, InvalidKeyException, IOException {
    List<String> mnemonic = Mnemonic.generate(24);
    log.info(mnemonic.toString());
    Pair keyPair = Mnemonic.toKeyPair(mnemonic);

    log.info("pubKey " + Utils.bytesToHex(keyPair.getPublicKey()));
    log.info("secKey " + Utils.bytesToHex(keyPair.getSecretKey()));

    Files.write(new File("2.ok").toPath(), keyPair.getSecretKey());
  }

  @Test
  public void testUseMnemonic2() throws NoSuchAlgorithmException, InvalidKeyException {
    Pair pair =
        Mnemonic.toKeyPair(
            "cement frequent produce tattoo casino tired road seat emotion nominee gloom busy father poet jealous all mail return one planet frozen over earth move");

    log.info("pubKey " + Utils.bytesToHex(pair.getPublicKey()));
    log.info("secKey " + Utils.bytesToHex(pair.getSecretKey()));

    TweetNaclFast.Signature.KeyPair keyPair =
        TweetNaclFast.Signature.keyPair_fromSeed(pair.getSecretKey());

    HighloadWallet contract1 =
        HighloadWallet.builder().keyPair(keyPair).walletId(42).queryId(BigInteger.ZERO).build();

    String nonBounceableAddress1 = contract1.getAddress().toNonBounceable();
    String bounceableAddress1 = contract1.getAddress().toBounceable();
    String rawAddress1 = contract1.getAddress().toRaw();

    log.info("non-bounceable address 1: {}", nonBounceableAddress1);
    log.info("    bounceable address 1: {}", bounceableAddress1);
    log.info("    raw address 1: {}", rawAddress1);
  }
}
