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
public class TestTonSdkTestCases {

    public static final String numbersTestFileUrl = "https://raw.githubusercontent.com/neodix42/ton-sdk-test-cases/main/numbers.json";
    Gson gson = new GsonBuilder().setObjectToNumberStrategy(ToNumberPolicy.LONG_OR_DOUBLE).create();

    String fileContentWithUseCases = IOUtils.toString(new URL(numbersTestFileUrl), Charset.defaultCharset());

    TonSdkTestCases tonSdkTestCases = gson.fromJson(fileContentWithUseCases, TonSdkTestCases.class);

    public TestTonSdkTestCases() throws IOException {
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
        BigDecimal sumRoundedBigDec = new BigDecimal(Utils.formatNanoValue(sumRounded, 2));


        String expectedRounded = (String) testCase.getExpectedOutput().get("sumDecimals2");
        BigDecimal expectedSumRoundedBigDec = new BigDecimal(Utils.formatNanoValue(expectedRounded, 2));

        assertThat(sumRounded).isEqualTo(expectedRounded);
        assertThat(sumRoundedBigDec).isEqualTo(expectedSumRoundedBigDec);
    }
}
