package org.ton.java;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.ToNumberPolicy;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.ton.java.cell.Cell;
import org.ton.java.cell.CellBuilder;
import org.ton.java.cell.CellSlice;
import org.ton.java.tlb.types.ExternalMessageInfo;
import org.ton.java.tlb.types.InternalMessageInfo;

import java.io.IOException;
import java.math.BigInteger;
import java.net.URL;
import java.nio.charset.Charset;

import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
@RunWith(JUnit4.class)
public class TestTonSdkTestCasesTlbDeserialization {
    public static final String numbersTestFileUrl = "https://raw.githubusercontent.com/neodix42/ton-sdk-test-cases/main/tlb-deserialization.json";
    Gson gson = new GsonBuilder().setObjectToNumberStrategy(ToNumberPolicy.LONG_OR_DOUBLE).create();
    String fileContentWithUseCases = IOUtils.toString(new URL(numbersTestFileUrl), Charset.defaultCharset());
    TonSdkTestCases tonSdkTestCases = gson.fromJson(fileContentWithUseCases, TonSdkTestCases.class);

    public TestTonSdkTestCasesTlbDeserialization() throws IOException {
    }

    @Test
    public void testTlbDeserialization1() {

        String testId = "tlb-deserialization-1";
        TonSdkTestCases.TestCase testCase = tonSdkTestCases.getTestCases().get(testId);

        String description = testCase.getDescription();

        log.info("testId: {}", testId);
        log.info("description: {}", description);

        String bocAsHex = testCase.getInput().get("bocAsHex").toString();

        Cell c = CellBuilder.beginCell().fromBoc(bocAsHex).endCell();
        InternalMessageInfo internalMessageInfo = InternalMessageInfo.deserialize(CellSlice.beginParse(c));
        log.info("internalMessage {}", internalMessageInfo);

        Boolean ihrDisabled = Boolean.valueOf(testCase.getExpectedOutput().get("ihrDisabled").toString());
        Boolean bounce = Boolean.valueOf(testCase.getExpectedOutput().get("bounce").toString());
        Boolean bounced = Boolean.valueOf(testCase.getExpectedOutput().get("bounced").toString());
        String sourceAddress = testCase.getExpectedOutput().get("sourceAddress").toString();
        String destinationAddress = testCase.getExpectedOutput().get("destinationAddress").toString();
        BigInteger valueCoins = new BigInteger(testCase.getExpectedOutput().get("valueCoins").toString());
        BigInteger ihrFee = new BigInteger(testCase.getExpectedOutput().get("ihrFee").toString());
        BigInteger fwdFee = new BigInteger(testCase.getExpectedOutput().get("fwdFee").toString());
        BigInteger createdLt = new BigInteger(testCase.getExpectedOutput().get("createdLt").toString());
        BigInteger createdAt = new BigInteger(testCase.getExpectedOutput().get("createdAt").toString());

        assertThat(internalMessageInfo.getIHRDisabled()).isEqualTo(ihrDisabled);
        assertThat(internalMessageInfo.getBounce()).isEqualTo(bounce);
        assertThat(internalMessageInfo.getBounced()).isEqualTo(bounced);
        assertThat(internalMessageInfo.getSrcAddr().toAddress().toRaw()).isEqualTo(sourceAddress);
        assertThat(internalMessageInfo.getDstAddr().toAddress().toRaw()).isEqualTo(destinationAddress);
        assertThat(internalMessageInfo.getValue().getCoins()).isEqualTo(valueCoins);
        assertThat(internalMessageInfo.getIHRFee()).isEqualTo(ihrFee);
        assertThat(internalMessageInfo.getFwdFee()).isEqualTo(fwdFee);
        assertThat(internalMessageInfo.getCreatedLt()).isEqualTo(createdLt);
        assertThat(internalMessageInfo.getCreatedAt()).isEqualTo(createdAt.longValue());
    }

    @Test
    public void testTlbDeserialization2() {

        String testId = "tlb-deserialization-2";
        TonSdkTestCases.TestCase testCase = tonSdkTestCases.getTestCases().get(testId);

        String description = testCase.getDescription();

        log.info("testId: {}", testId);
        log.info("description: {}", description);

        String bocAsHex = testCase.getInput().get("bocAsHex").toString();

        Cell c = CellBuilder.beginCell().fromBoc(bocAsHex).endCell();
        ExternalMessageInfo externalMessageInfo = ExternalMessageInfo.deserialize(CellSlice.beginParse(c));
        log.info("ExternalMessageInfo {}", externalMessageInfo);

        Boolean ihrDisabled = Boolean.valueOf(testCase.getExpectedOutput().get("ihrDisabled").toString());
        Boolean bounce = Boolean.valueOf(testCase.getExpectedOutput().get("bounce").toString());
        Boolean bounced = Boolean.valueOf(testCase.getExpectedOutput().get("bounced").toString());
        String sourceAddress = testCase.getExpectedOutput().get("sourceAddress").toString();
    }
}
