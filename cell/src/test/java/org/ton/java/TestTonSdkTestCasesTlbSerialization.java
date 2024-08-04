package org.ton.java;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.ToNumberPolicy;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.ton.java.tlb.types.MsgAddress;
import org.ton.java.tlb.types.MsgAddressExtNone;
import org.ton.java.tlb.types.MsgAddressExternal;
import org.ton.java.utils.Utils;

import java.io.IOException;
import java.math.BigInteger;
import java.net.URL;
import java.nio.charset.Charset;

import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
@RunWith(JUnit4.class)
public class TestTonSdkTestCasesTlbSerialization {
    public static final String numbersTestFileUrl = "https://raw.githubusercontent.com/neodix42/ton-sdk-test-cases/main/tlb-serialization.json";
    Gson gson = new GsonBuilder().setObjectToNumberStrategy(ToNumberPolicy.LONG_OR_DOUBLE).create();
    String fileContentWithUseCases = IOUtils.toString(new URL(numbersTestFileUrl), Charset.defaultCharset());
    TonSdkTestCases tonSdkTestCases = gson.fromJson(fileContentWithUseCases, TonSdkTestCases.class);

    public TestTonSdkTestCasesTlbSerialization() throws IOException {
    }

    @Test
    public void testTlbSerialization1() {

        String testId = "tlb-serialization-1";
        TonSdkTestCases.TestCase testCase = tonSdkTestCases.getTestCases().get(testId);

        String description = testCase.getDescription();

        log.info("testId: {}", testId);
        log.info("description: {}", description);

        String bocAsHex = testCase.getInput().get("addr1_none").toString();
        MsgAddress addr1 = MsgAddressExtNone.builder().build();

        BigInteger addr2_external_len = new BigInteger(testCase.getInput().get("addr2_external_len").toString());
        String addr2_external_bits = testCase.getInput().get("addr2_external_bits").toString();

        MsgAddress addr2 = MsgAddressExternal.builder()
                .len(addr2_external_len.intValue())
                .externalAddress(new BigInteger(Utils.bitStringToHex(addr2_external_bits), 16))
                .build();
        String bocAsHexAddr1 = testCase.getExpectedOutput().get("bocAsHexAddr1").toString();
        String bocAsHexAddr2 = testCase.getExpectedOutput().get("bocAsHexAddr2").toString();

        assertThat(addr1.toCell().toHex(true)).isEqualTo(bocAsHexAddr1);
        assertThat(addr2.toCell().toHex(true)).isEqualTo(bocAsHexAddr2);
    }
}
