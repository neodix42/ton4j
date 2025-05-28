package org.ton.ton4j.utils;

import com.iwebpp.crypto.TweetNaclFast;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@Slf4j
@RunWith(JUnit4.class)
public class TestEncryption {

    @Test
    public void testMd5() {
        String md5 = Utils.md5("ABC".getBytes());
        assertThat(md5).isEqualTo("902fbdd2b1df0c4f70b4a5d23525e932");
    }

    @Test
    public void testSha1() {
        String sha1 = Utils.sha1("ABC".getBytes());
        assertThat(sha1).isEqualTo("3c01bdbb26f358bab27f267924aa2c9a03fcfdb8");
    }

    @Test
    public void testSha256() {
        String sha256 = Utils.sha256("ABC");
        assertThat(sha256).isEqualTo("b5d4045c3f466fa91fe2cc6abe79232a1a57cdf104f7a26e716e0a1e2789df78");
    }

    @Test
    public void testSha256Bytes() {
        String sha256 = Utils.sha256("ABC".getBytes());
        assertThat(sha256).isEqualTo("b5d4045c3f466fa91fe2cc6abe79232a1a57cdf104f7a26e716e0a1e2789df78");
    }

    @Test
    public void testBoxAndSignature() {
        byte[] secretKey = Utils.hexToSignedBytes("F182111193F30D79D517F2339A1BA7C25FDF6C52142F0F2C1D960A1F1D65E1E4");

        TweetNaclFast.Box.KeyPair keyPairBox = Utils.generateKeyPairFromSecretKey(secretKey);
        TweetNaclFast.Signature.KeyPair keyPairSignature = Utils.generateSignatureKeyPairFromSeed(secretKey);

        log.info("Box");
        log.info(Utils.bytesToHex(keyPairBox.getPublicKey()));
        log.info(Utils.bytesToHex(keyPairBox.getSecretKey()));

        log.info("Signature");
        log.info(Utils.bytesToHex(keyPairSignature.getPublicKey()));
        log.info(Utils.bytesToHex(keyPairSignature.getSecretKey()));
    }

    @Test
    public void testKeyPairSignatureSignFromSeed() {
        byte[] secretKey = Utils.hexToSignedBytes("F182111193F30D79D517F2339A1BA7C25FDF6C52142F0F2C1D960A1F1D65E1E4");
        TweetNaclFast.Signature.KeyPair keyPair = Utils.generateSignatureKeyPairFromSeed(secretKey);

        byte[] pubKey = keyPair.getPublicKey();
        byte[] secKey = keyPair.getSecretKey();

        log.info("pubKey: {}", Utils.bytesToHex(pubKey));
        log.info("secKey: {}", Utils.bytesToHex(secKey));

        String msg = "ABC";
        String msgHashSha256 = Utils.sha256(msg);

        byte[] signedMsg = Utils.signData(pubKey, secKey, Utils.hexToSignedBytes(msgHashSha256));
        log.info(Utils.bytesToHex(signedMsg));
    }

    @Test
    public void testKeyPairSignatureSignKeyPair() {
        TweetNaclFast.Signature.KeyPair keyPair = Utils.generateSignatureKeyPair();

        byte[] pubKey = keyPair.getPublicKey();
        byte[] secKey = keyPair.getSecretKey(); // 64 bytes

        log.info("pubKey: {}", Utils.bytesToHex(pubKey));
        log.info("pubKeyBase64: {}", Utils.bytesToBase64(pubKey));
        log.info("secKey: {}", Utils.bytesToHex(secKey));

        String msg = "ABC";
        String msgHashSha256 = Utils.sha256(msg);
        log.info("msg hash: {} ", msgHashSha256);

        byte[] signedMsg = Utils.signData(pubKey, secKey, Utils.hexToSignedBytes(msgHashSha256));
        log.info(Utils.bytesToHex(signedMsg));
    }
}
