package org.ton.java.utils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.ToNumberPolicy;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URL;
import java.nio.charset.Charset;

import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
@RunWith(JUnit4.class)
public class TestTonSdkTestCasesNumbers {

    public static final String numbersTestFileUrl = "https://raw.githubusercontent.com/neodix42/ton-sdk-test-cases/main/numbers.json";
    Gson gson = new GsonBuilder().setObjectToNumberStrategy(ToNumberPolicy.LONG_OR_DOUBLE).create();

    String fileContentWithUseCases = IOUtils.toString(new URL(numbersTestFileUrl), Charset.defaultCharset());

    TonSdkTestCases tonSdkTestCases = gson.fromJson(fileContentWithUseCases, TonSdkTestCases.class);

    public TestTonSdkTestCasesNumbers() throws IOException {
    }

    @Test
    public void testNumbers1() {

        String testId = "numbers-1";
        TonSdkTestCases.TestCase testCase = tonSdkTestCases.getTestCases().get(testId);

        String description = testCase.getDescription();

        log.info("testId: {}", testId);
        log.info("description: {}", description);

        BigInteger inputNumberA = new BigInteger(testCase.getInput().get("inputNumberA").toString());
        BigInteger powerOfNumberA = new BigInteger(testCase.getInput().get("powerOfNumberA").toString());
        BigInteger inputNumberB = new BigInteger(testCase.getInput().get("inputNumberB").toString());
        BigInteger powerOfNumberB = new BigInteger(testCase.getInput().get("powerOfNumberB").toString());


        BigInteger a = inputNumberA.pow(powerOfNumberA.intValue());
        BigInteger b = inputNumberB.pow(powerOfNumberB.intValue());
        Boolean expectedPubKey = (Boolean) testCase.getExpectedOutput().get("AequalsB");

        assertThat(a.compareTo(b) == 0).isEqualTo(expectedPubKey);
    }

    @Test
    public void testNumbers2() {

        String testId = "numbers-2";
        TonSdkTestCases.TestCase testCase = tonSdkTestCases.getTestCases().get(testId);

        log.info("TestCase: {}", testCase);

        BigInteger toncoinsA = Utils.toNano(testCase.getInput().get("toncoinsA").toString());
        BigInteger toncoinsB = Utils.toNano(testCase.getInput().get("toncoinsB").toString());
        BigInteger toncoinsC = Utils.toNano(testCase.getInput().get("toncoinsC").toString());

        BigInteger sum = toncoinsA.add(toncoinsB).add(toncoinsC);
        log.info("sum {}", sum);
        String sumRounded = Utils.formatNanoValue(sum, 2);
        BigDecimal sumRoundedBigDec = new BigDecimal(Utils.formatCoins(sumRounded, 2));


        String expectedRounded = (String) testCase.getExpectedOutput().get("sumDecimals2");
        BigDecimal expectedSumRoundedBigDec = new BigDecimal(Utils.formatCoins(expectedRounded, 2));

        assertThat(sumRounded).isEqualTo(expectedRounded);
        assertThat(sumRoundedBigDec).isEqualTo(expectedSumRoundedBigDec);
    }

    @Test
    public void testNumbers3() {

        String testId = "numbers-3";
        TonSdkTestCases.TestCase testCase = tonSdkTestCases.getTestCases().get(testId);

        log.info("TestCase: {}", testCase);

        BigInteger toncoinsValueA = Utils.toNano(testCase.getInput().get("toncoinsValueA").toString());
        BigInteger toncoinsValueB = Utils.toNano(testCase.getInput().get("toncoinsValueB").toString());

        log.info("toncoinsValueA {}", toncoinsValueA);
        log.info("toncoinsValueB {}", toncoinsValueB);

        BigInteger nanoValueA = new BigInteger(testCase.getExpectedOutput().get("nanoValueA").toString());
        BigInteger nanoValueB = new BigInteger(testCase.getExpectedOutput().get("nanoValueB").toString());

        assertThat(toncoinsValueA).isEqualTo(nanoValueA);
        assertThat(toncoinsValueB).isEqualTo(nanoValueB);
    }

    @Test
    public void testNumbers4() {

        String testId = "numbers-4";
        TonSdkTestCases.TestCase testCase = tonSdkTestCases.getTestCases().get(testId);

        log.info("TestCase: {}", testCase);

        BigInteger nanoCoins = new BigInteger(testCase.getInput().get("nanoValue").toString());

        log.info("nanoCoinsValue {}", nanoCoins);

        String expectedStr = (String) testCase.getExpectedOutput().get("toncoins");
        assertThat(Utils.fromNano(nanoCoins)).isEqualTo(expectedStr);
    }

    @Test(expected = Error.class)
    public void testNumbers5() {

        String testId = "numbers-5";
        TonSdkTestCases.TestCase testCase = tonSdkTestCases.getTestCases().get(testId);

        log.info("TestCase: {}", testCase);
        Utils.toNano(testCase.getInput().get("toncoinsValue").toString());
    }

    @Test(expected = Error.class)
    public void testNumbers6() {

        String testId = "numbers-6";
        TonSdkTestCases.TestCase testCase = tonSdkTestCases.getTestCases().get(testId);

        log.info("TestCase: {}", testCase);
        Utils.fromNano(testCase.getInput().get("nanoValue").toString());
    }

    @Test(expected = Error.class)
    public void testNumbers7() {

        String testId = "numbers-7";
        TonSdkTestCases.TestCase testCase = tonSdkTestCases.getTestCases().get(testId);

        log.info("TestCase: {}", testCase);

        BigInteger base = new BigInteger(testCase.getInput().get("base").toString());
        BigInteger pow = new BigInteger(testCase.getInput().get("pow").toString());

        BigInteger res = base.pow(pow.intValue());
        Utils.fromNano(res);
    }

    @Test
    public void testNumbers8() {

        String testId = "numbers-8";
        TonSdkTestCases.TestCase testCase = tonSdkTestCases.getTestCases().get(testId);

        log.info("TestCase: {}", testCase);

        BigInteger base = new BigInteger(testCase.getInput().get("base").toString());
        BigInteger pow = new BigInteger(testCase.getInput().get("pow").toString());
        BigInteger minusOne = new BigInteger(testCase.getInput().get("minus").toString());

        BigInteger res = base.pow(pow.intValue()).subtract(minusOne);
        Utils.fromNano(res);
    }
}
