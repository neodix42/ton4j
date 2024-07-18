package org.ton.java.utils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.ToNumberPolicy;
import com.iwebpp.crypto.TweetNaclFast;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.io.IOException;
import java.math.BigInteger;
import java.net.URL;
import java.nio.charset.Charset;

import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
@RunWith(JUnit4.class)
public class TestTonSdkTestCasesCryptography {

    public static final String numbersTestFileUrl = "https://raw.githubusercontent.com/neodix42/ton-sdk-test-cases/main/cryptography.json";
    Gson gson = new GsonBuilder().setObjectToNumberStrategy(ToNumberPolicy.LONG_OR_DOUBLE).create();

    String fileContentWithUseCases = IOUtils.toString(new URL(numbersTestFileUrl), Charset.defaultCharset());

    TonSdkTestCases tonSdkTestCases = gson.fromJson(fileContentWithUseCases, TonSdkTestCases.class);

    public TestTonSdkTestCasesCryptography() throws IOException {
    }

    @Test
    public void testCryptography1() {

        String testId = "cryptography-1";
        TonSdkTestCases.TestCase testCase = tonSdkTestCases.getTestCases().get(testId);

        String description = testCase.getDescription();

        log.info("testId: {}", testId);
        log.info("description: {}", description);

        String inputString1 = testCase.getInput().get("inputString1").toString();
        String inputString2 = testCase.getInput().get("inputString2").toString();
        String inputString3 = testCase.getInput().get("inputString3").toString();
        String inputString4 = Utils.generateString(1000000, "a");


        long actual1 = Utils.getCRC32ChecksumAsLong(inputString1.getBytes());
        long actual2 = Utils.getCRC32ChecksumAsLong(inputString2.getBytes());
        long actual3 = Utils.getCRC32ChecksumAsLong(inputString3.getBytes());
        long actual4 = Utils.getCRC32ChecksumAsLong(inputString4.getBytes());

        BigInteger expectedAsUnsignedInt1 = new BigInteger(testCase.getExpectedOutput().get("resultAsUnsignedInt1").toString());
        BigInteger expectedAsUnsignedInt2 = new BigInteger(testCase.getExpectedOutput().get("resultAsUnsignedInt2").toString());
        BigInteger expectedAsUnsignedInt3 = new BigInteger(testCase.getExpectedOutput().get("resultAsUnsignedInt3").toString());
        BigInteger expectedAsUnsignedInt4 = new BigInteger(testCase.getExpectedOutput().get("resultAsUnsignedInt4").toString());

        assertThat(actual1).isEqualTo(expectedAsUnsignedInt1.longValue());
        assertThat(actual2).isEqualTo(expectedAsUnsignedInt2.longValue());
        assertThat(actual3).isEqualTo(expectedAsUnsignedInt3.longValue());
        assertThat(actual4).isEqualTo(expectedAsUnsignedInt4.longValue());
    }

    @Test
    public void testCryptography2() {

        String testId = "cryptography-2";
        TonSdkTestCases.TestCase testCase = tonSdkTestCases.getTestCases().get(testId);

        String description = testCase.getDescription();

        log.info("testId: {}", testId);
        log.info("description: {}", description);

        String inputString1 = testCase.getInput().get("inputString1").toString();
        String inputString2 = testCase.getInput().get("inputString2").toString();
        String inputString3 = testCase.getInput().get("inputString3").toString();
        String inputString4 = Utils.generateString(1000000, "a");


        int actual1 = Utils.getCRC16ChecksumAsInt(inputString1.getBytes());
        int actual2 = Utils.getCRC16ChecksumAsInt(inputString2.getBytes());
        int actual3 = Utils.getCRC16ChecksumAsInt(inputString3.getBytes());
        int actual4 = Utils.getCRC16ChecksumAsInt(inputString4.getBytes());

        BigInteger expectedAsUnsignedInt1 = new BigInteger(testCase.getExpectedOutput().get("resultAsUnsignedInt1").toString());
        BigInteger expectedAsUnsignedInt2 = new BigInteger(testCase.getExpectedOutput().get("resultAsUnsignedInt2").toString());
        BigInteger expectedAsUnsignedInt3 = new BigInteger(testCase.getExpectedOutput().get("resultAsUnsignedInt3").toString());
        BigInteger expectedAsUnsignedInt4 = new BigInteger(testCase.getExpectedOutput().get("resultAsUnsignedInt4").toString());

        assertThat(actual1).isEqualTo(expectedAsUnsignedInt1.intValue());
        assertThat(actual2).isEqualTo(expectedAsUnsignedInt2.intValue());
        assertThat(actual3).isEqualTo(expectedAsUnsignedInt3.intValue());
        assertThat(actual4).isEqualTo(expectedAsUnsignedInt4.intValue());
    }

    @Test
    public void testCryptography3() {

        String testId = "cryptography-3";
        TonSdkTestCases.TestCase testCase = tonSdkTestCases.getTestCases().get(testId);

        String description = testCase.getDescription();

        log.info("testId: {}", testId);
        log.info("description: {}", description);

        String inputString1 = testCase.getInput().get("inputString1").toString();
        String inputString2 = testCase.getInput().get("inputString2").toString();
        String inputString3 = testCase.getInput().get("inputString3").toString();
        String inputString4 = Utils.generateString(1000000, "a");


        byte[] actual1 = Utils.sha256AsArray(inputString1.getBytes());
        byte[] actual2 = Utils.sha256AsArray(inputString2.getBytes());
        byte[] actual3 = Utils.sha256AsArray(inputString3.getBytes());
        byte[] actual4 = Utils.sha256AsArray(inputString4.getBytes());

        String expectedBase641 = testCase.getExpectedOutput().get("resultBase641").toString();
        String expectedBase642 = testCase.getExpectedOutput().get("resultBase642").toString();
        String expectedBase643 = testCase.getExpectedOutput().get("resultBase643").toString();
        String expectedBase644 = testCase.getExpectedOutput().get("resultBase644").toString();

        assertThat(Utils.bytesToBase64(actual1)).isEqualTo(expectedBase641);
        assertThat(Utils.bytesToBase64(actual2)).isEqualTo(expectedBase642);
        assertThat(Utils.bytesToBase64(actual3)).isEqualTo(expectedBase643);
        assertThat(Utils.bytesToBase64(actual4)).isEqualTo(expectedBase644);
    }

    @Test
    public void testCryptography4() {

        String testId = "cryptography-4";
        TonSdkTestCases.TestCase testCase = tonSdkTestCases.getTestCases().get(testId);

        String description = testCase.getDescription();

        log.info("testId: {}", testId);
        log.info("description: {}", description);

        String inputString1 = testCase.getInput().get("inputString1").toString();
        String inputString2 = testCase.getInput().get("inputString2").toString();
        String inputString3 = testCase.getInput().get("inputString3").toString();
        String inputString4 = Utils.generateString(1000000, "a");


        byte[] actual1 = Utils.sha1AsArray(inputString1.getBytes());
        byte[] actual2 = Utils.sha1AsArray(inputString2.getBytes());
        byte[] actual3 = Utils.sha1AsArray(inputString3.getBytes());
        byte[] actual4 = Utils.sha1AsArray(inputString4.getBytes());

        String expectedBase641 = testCase.getExpectedOutput().get("resultBase641").toString();
        String expectedBase642 = testCase.getExpectedOutput().get("resultBase642").toString();
        String expectedBase643 = testCase.getExpectedOutput().get("resultBase643").toString();
        String expectedBase644 = testCase.getExpectedOutput().get("resultBase644").toString();

        assertThat(Utils.bytesToBase64(actual1)).isEqualTo(expectedBase641);
        assertThat(Utils.bytesToBase64(actual2)).isEqualTo(expectedBase642);
        assertThat(Utils.bytesToBase64(actual3)).isEqualTo(expectedBase643);
        assertThat(Utils.bytesToBase64(actual4)).isEqualTo(expectedBase644);
    }

    @Test
    public void testCryptography5() throws IOException {

        String testId = "cryptography-5";
        // select particular test case by category name and test id
        TonSdkTestCases.TestCase testCase = tonSdkTestCases.getTestCases().get(testId);

        // print test case details
        log.info("TestCase: {}", testCase);

        // fetch input parameters
        String prvKey = testCase.getInput().get("privateKey").toString();

        String inputString1 = testCase.getInput().get("inputString1").toString();
        String inputString2 = testCase.getInput().get("inputString2").toString();
        String inputString3 = testCase.getInput().get("inputString3").toString();
        String inputString4 = Utils.generateString(1000000, "a");


        byte[] secretKey = Utils.hexToSignedBytes(prvKey);
        TweetNaclFast.Signature.KeyPair keyPair = Utils.generateSignatureKeyPairFromSeed(secretKey);

        byte[] pubKey = keyPair.getPublicKey();
        byte[] secKey = keyPair.getSecretKey();

        String actualPubKeyAsHex = Utils.bytesToHex(pubKey);

        byte[] actualSigned1 = Utils.signData(pubKey, secKey, inputString1.getBytes());
        byte[] actualSigned2 = Utils.signData(pubKey, secKey, inputString2.getBytes());
        byte[] actualSigned3 = Utils.signData(pubKey, secKey, inputString3.getBytes());
        byte[] actualSigned4 = Utils.signData(pubKey, secKey, inputString4.getBytes());

        // fetch expected results
        String expectedPubKey = (String) testCase.getExpectedOutput().get("publicKey");
        String expectedBase641 = testCase.getExpectedOutput().get("resultBase641").toString();
        String expectedBase642 = testCase.getExpectedOutput().get("resultBase642").toString();
        String expectedBase643 = testCase.getExpectedOutput().get("resultBase643").toString();
        String expectedBase644 = testCase.getExpectedOutput().get("resultBase644").toString();

        assertThat(actualPubKeyAsHex).isEqualTo(expectedPubKey);
        assertThat(Utils.bytesToBase64(actualSigned1)).isEqualTo(expectedBase641);
        assertThat(Utils.bytesToBase64(actualSigned2)).isEqualTo(expectedBase642);
        assertThat(Utils.bytesToBase64(actualSigned3)).isEqualTo(expectedBase643);
        assertThat(Utils.bytesToBase64(actualSigned4)).isEqualTo(expectedBase644);
    }

    @Test
    public void testCryptography6() {

        String testId = "cryptography-6";
        TonSdkTestCases.TestCase testCase = tonSdkTestCases.getTestCases().get(testId);

        String description = testCase.getDescription();

        log.info("testId: {}", testId);
        log.info("description: {}", description);

        String inputString1 = testCase.getInput().get("inputString1").toString();
        String inputString2 = testCase.getInput().get("inputString2").toString();
        String inputString3 = testCase.getInput().get("inputString3").toString();
        String inputString4 = Utils.generateString(1000000, "a");


        byte[] actual1 = Utils.md5AsArray(inputString1.getBytes());
        byte[] actual2 = Utils.md5AsArray(inputString2.getBytes());
        byte[] actual3 = Utils.md5AsArray(inputString3.getBytes());
        byte[] actual4 = Utils.md5AsArray(inputString4.getBytes());

        String expectedBase641 = testCase.getExpectedOutput().get("resultBase641").toString();
        String expectedBase642 = testCase.getExpectedOutput().get("resultBase642").toString();
        String expectedBase643 = testCase.getExpectedOutput().get("resultBase643").toString();
        String expectedBase644 = testCase.getExpectedOutput().get("resultBase644").toString();

        assertThat(Utils.bytesToBase64(actual1)).isEqualTo(expectedBase641);
        assertThat(Utils.bytesToBase64(actual2)).isEqualTo(expectedBase642);
        assertThat(Utils.bytesToBase64(actual3)).isEqualTo(expectedBase643);
        assertThat(Utils.bytesToBase64(actual4)).isEqualTo(expectedBase644);
    }
}
