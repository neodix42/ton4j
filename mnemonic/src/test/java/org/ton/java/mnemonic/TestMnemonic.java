package org.ton.java.mnemonic;

import static org.assertj.core.api.Assertions.assertThat;

import com.iwebpp.crypto.TweetNaclFast;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.ton.java.utils.Utils;

@RunWith(JUnit4.class)
public class TestMnemonic {
  @Test
  public void testMnemonicWithoutPassword() throws NoSuchAlgorithmException, InvalidKeyException {
    String pwd = "";
    List<String> mnemonic = Mnemonic.generate(24, pwd);
    assertThat(Mnemonic.isValid(mnemonic, pwd)).isTrue();
    assertThat(Mnemonic.isBasicSeed(Mnemonic.toEntropy(mnemonic, pwd))).isTrue();
    assertThat(Mnemonic.isPasswordSeed(Mnemonic.toEntropy(mnemonic, pwd))).isFalse();
    assertThat(Mnemonic.isPasswordNeeded(mnemonic)).isFalse();
  }

  @Test
  public void testMnemonicWithPassword() throws NoSuchAlgorithmException, InvalidKeyException {
    String pwd = "password";
    List<String> mnemonic = Mnemonic.generate(24, pwd);
    assertThat(Mnemonic.isValid(mnemonic, "")).isFalse();
    assertThat(Mnemonic.isValid(mnemonic, pwd)).isTrue();
    assertThat(Mnemonic.isBasicSeed(Mnemonic.toEntropy(mnemonic))).isFalse();
    assertThat(Mnemonic.isPasswordSeed(Mnemonic.toEntropy(mnemonic))).isTrue();
    assertThat(Mnemonic.isPasswordNeeded(mnemonic)).isTrue();
  }

  @Test
  public void testMnemonicValidation() throws NoSuchAlgorithmException, InvalidKeyException {

    assertThat(
            Mnemonic.isValid(
                Arrays.asList(
                    "audit",
                    "magic",
                    "blossom",
                    "digital",
                    "dad",
                    "buffalo",
                    "river",
                    "junior",
                    "minimum",
                    "congress",
                    "banner",
                    "garage",
                    "flag",
                    "tuna",
                    "onion",
                    "pair",
                    "balance",
                    "spice",
                    "reason",
                    "gossip",
                    "cotton",
                    "stock",
                    "skate",
                    "faith"),
                ""))
        .isTrue();
    assertThat(Mnemonic.isValid(Arrays.asList("kangaroo", "hen", "toddler", "resist"), ""))
        .isTrue();
    assertThat(Mnemonic.isValid(Arrays.asList("disease", "adult", "device", "grit"), "")).isTrue();

    assertThat(Mnemonic.isValid(Arrays.asList("disease", "adult", "device", "grit"), "password"))
        .isFalse();
    assertThat(Mnemonic.isValid(Arrays.asList("deal", "wrap", "runway", "possible"), "password"))
        .isTrue();
    assertThat(
            Mnemonic.isValid(Arrays.asList("deal", "wrap", "runway", "possible"), "notthepassword"))
        .isFalse();
    assertThat(Mnemonic.isValid(Arrays.asList("deal", "wrap", "runway", "possible"), "")).isFalse();
  }

  @Test
  public void testMnemonicSeed() throws NoSuchAlgorithmException, InvalidKeyException {
    assertThat(bytesToHex(Mnemonic.toSeed(Arrays.asList("kangaroo", "hen", "toddler", "resist"))))
        .isEqualTo("a356fc9b35cb9b463adf65b2414bbebcec1d0d0d99fc4fc14e259395c128022d");
    assertThat(bytesToHex(Mnemonic.toSeed(Arrays.asList("disease", "adult", "device", "grit"), "")))
        .isEqualTo("fb1df381306619a2128295e73e05c6013211f589e8bebd602469cdf1fc04a1cb");
    assertThat(
            bytesToHex(
                Mnemonic.toSeed(Arrays.asList("deal", "wrap", "runway", "possible"), "password")))
        .isEqualTo("3078a0d183d0f0e88c4f8a5979590612f230a3228912838b66bcc9e9053b2584");
  }

  private String bytesToHex(byte[] raw) {
    String hexes = "0123456789ABCDEF";
    final StringBuilder hex = new StringBuilder(2 * raw.length);
    for (final byte b : raw) {
      hex.append(hexes.charAt((b & 0xF0) >> 4)).append(hexes.charAt((b & 0x0F)));
    }
    return hex.toString().toLowerCase();
  }

  @Test
  public void testMnemonicSeed2() throws NoSuchAlgorithmException, InvalidKeyException {
    byte[] key =
        Mnemonic.toSeed(
            Arrays.asList(
                "victory",
                "ginger",
                "intact",
                "account",
                "response",
                "claim",
                "fitness",
                "park",
                "educate",
                "achieve",
                "index",
                "cupboard",
                "give",
                "spread",
                "enough",
                "tiger",
                "glove",
                "target",
                "cupboard",
                "expect",
                "craft",
                "type",
                "comfort",
                "speak"),
            "");
    byte[] data = new byte[0];
    byte[] res = Mnemonic.hmacSha512(key, data);
    System.out.println(bytesToHex(res));
  }

  @Test
  public void testMnemonicToKeyPair() throws NoSuchAlgorithmException, InvalidKeyException {

    Pair key =
        Mnemonic.toKeyPair(
            Arrays.asList(
                "audit",
                "magic",
                "blossom",
                "digital",
                "dad",
                "buffalo",
                "river",
                "junior",
                "minimum",
                "congress",
                "banner",
                "garage",
                "flag",
                "tuna",
                "onion",
                "pair",
                "balance",
                "spice",
                "reason",
                "gossip",
                "cotton",
                "stock",
                "skate",
                "faith"));
    System.out.println(bytesToHex(key.getPublicKey()));
    System.out.println(bytesToHex(key.getSecretKey()));
  }

  @Test
  public void testSigning() {

    TweetNaclFast.Signature.KeyPair keyPair = Utils.generateSignatureKeyPair();
    System.out.println("pubKey: " + bytesToHex(keyPair.getPublicKey()));
    System.out.println("prvKey: " + bytesToHex(keyPair.getSecretKey()));
    String testMsg = "ABC";

    // Using bouncy castle Ed25519 from Mnemonic module
    byte[] signature =
        Ed25519.sign(keyPair.getSecretKey(), testMsg.getBytes(StandardCharsets.UTF_8));
    System.out.println("sig: " + bytesToHex(signature));

    // Using TweetNaclFast from TweetNacl-java-8 module
    byte[] signature2 =
        Utils.signData(
            keyPair.getPublicKey(),
            keyPair.getSecretKey(),
            testMsg.getBytes(StandardCharsets.UTF_8));
    System.out.println("sig: " + bytesToHex(signature2));

    assertThat(signature).isEqualTo(signature2);
  }

  @Test
  public void testVerification() {

    String signature =
        "e189001b6c1afcb13a7f2f3e6cee5b0f40834fd1d5302c09389c0fbb99fe342de71af4db21b137bf4ff0cd967a8a22f5ed8d4a857f3e8ea55ba14460259d2202";
    String pubKey = "d86009539b964dc57dc0feb7bd478356161c578459e747e0bf7d3d0bf7a290dd";
    String prvKey =
        "7f8c3387483e25f1ee0dcb36b190ab2996c6aa4824d5bee55fb9b83274e8cedad86009539b964dc57dc0feb7bd478356161c578459e747e0bf7d3d0bf7a290dd";

    System.out.println("pubKey: " + pubKey);
    System.out.println("prvKey: " + prvKey);
    String testMsg = "ABC";

    // Using bouncy castle Ed25519 from Mnemonic module
    boolean result =
        Ed25519.verify(
            Utils.hexToSignedBytes(pubKey),
            testMsg.getBytes(StandardCharsets.UTF_8),
            Utils.hexToSignedBytes(signature));
    System.out.println("result: " + result);
  }
}
