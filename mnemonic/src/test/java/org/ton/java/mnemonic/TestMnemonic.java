package org.ton.java.mnemonic;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;


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
        assertThat(Mnemonic.isValid(List.of("kangaroo", "hen", "toddler", "resist"), "")).isTrue();
        assertThat(Mnemonic.isValid(List.of("disease", "adult", "device", "grit"), "")).isTrue();

        assertThat(Mnemonic.isValid(List.of("disease", "adult", "device", "grit"), "password")).isFalse(); // o
        assertThat(Mnemonic.isValid(List.of("deal", "wrap", "runway", "possible"), "password")).isTrue();
        assertThat(Mnemonic.isValid(List.of("deal", "wrap", "runway", "possible"), "notthepassword")).isFalse(); // o
        assertThat(Mnemonic.isValid(List.of("deal", "wrap", "runway", "possible"), "")).isFalse(); //o
    }

    @Test
    public void testMnemonicSeed() throws NoSuchAlgorithmException, InvalidKeyException {
        assertThat(bytesToHex(Mnemonic.toSeed(List.of("kangaroo", "hen", "toddler", "resist")))).isEqualTo("a356fc9b35cb9b463adf65b2414bbebcec1d0d0d99fc4fc14e259395c128022d");
        assertThat(bytesToHex(Mnemonic.toSeed(List.of("disease", "adult", "device", "grit"), ""))).isEqualTo("fb1df381306619a2128295e73e05c6013211f589e8bebd602469cdf1fc04a1cb");
        assertThat(bytesToHex(Mnemonic.toSeed(List.of("deal", "wrap", "runway", "possible"), "password"))).isEqualTo("3078a0d183d0f0e88c4f8a5979590612f230a3228912838b66bcc9e9053b2584");
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
        byte[] key = Mnemonic.toSeed(List.of("victory", "ginger", "intact",
                "account", "response", "claim",
                "fitness", "park", "educate",
                "achieve", "index", "cupboard",
                "give", "spread", "enough",
                "tiger", "glove", "target",
                "cupboard", "expect", "craft",
                "type", "comfort", "speak"), "");
        byte[] data = new byte[0];
        byte[] res = Mnemonic.hmacSha512(key, data);
        System.out.println(bytesToHex(res));
    }
}
