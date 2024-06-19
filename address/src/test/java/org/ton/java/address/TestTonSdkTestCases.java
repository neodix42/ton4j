package org.ton.java.address;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.ToNumberPolicy;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.io.IOException;
import java.net.URL;
import java.nio.charset.Charset;

import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
@RunWith(JUnit4.class)
public class TestTonSdkTestCases {

    public static final String addressTestFileUrl = "https://raw.githubusercontent.com/neodix42/ton-sdk-test-cases/main/address.json";
    Gson gson = new GsonBuilder().setObjectToNumberStrategy(ToNumberPolicy.LONG_OR_DOUBLE).create();

    String fileContentWithUseCases = IOUtils.toString(new URL(addressTestFileUrl), Charset.defaultCharset());

    public TestTonSdkTestCases() throws IOException {
    }

    @Test
    public void testAddress1() {

        // read the JSON file with tests cases
        TonSdkTestCases tonSdkTestCases = gson.fromJson(fileContentWithUseCases, TonSdkTestCases.class);

        String testId = "address-1";
        // select particular test case by category name and test id
        TonSdkTestCases.TestCase testCase = tonSdkTestCases.getTestCases().get(testId);

        // fetch test's description and id. It's always good to show test id, since it is unique across all tests.
        String description = testCase.getDescription();

        log.info("testId: {}", testId);
        log.info("description: {}", description);

        // fetch input parameters and print test case details
        Address address = Address.of((String) testCase.getInput().get("inputRawAddress"));

        log.info("input parameters:");
        log.info("  address: {}", address.toRaw());

        // test the functionality of your library

        // fetch the expected result and compare it against the actual one
        String expectedBounceableUrlSafe = (String) testCase.getExpectedOutput().get("bounceableUrlSafe");
        String expectedNonBounceableUrlSafe = (String) testCase.getExpectedOutput().get("nonBounceableUrlSafe");
        String expectedBounceable = (String) testCase.getExpectedOutput().get("bounceable");
        String expectedNonBounceable = (String) testCase.getExpectedOutput().get("nonBounceable");
        String expectedBounceableUrlSafeTest = (String) testCase.getExpectedOutput().get("bounceableUrlSafeTest");
        String expectedNonBounceableUrlSafeTest = (String) testCase.getExpectedOutput().get("nonBounceableUrlSafeTest");
        String expectedBounceableTest = (String) testCase.getExpectedOutput().get("bounceableTest");
        String expectedNonBounceableTest = (String) testCase.getExpectedOutput().get("nonBounceableTest");

        assertThat(expectedBounceableUrlSafe).isEqualTo(address.toBounceable());
        assertThat(expectedNonBounceableUrlSafe).isEqualTo(address.toNonBounceable());
        assertThat(expectedBounceable).isEqualTo(address.toString(true, false, true));
        assertThat(expectedNonBounceable).isEqualTo(address.toString(true, false, false));
        assertThat(expectedBounceableUrlSafeTest).isEqualTo(address.toString(true, true, true, true));
        assertThat(expectedNonBounceableUrlSafeTest).isEqualTo(address.toString(true, true, false, true));
        assertThat(expectedBounceableTest).isEqualTo(address.toString(true, false, true, true));
        assertThat(expectedNonBounceableTest).isEqualTo(address.toString(true, false, false, true));
    }

    @Test
    public void testAddress2() {

        TonSdkTestCases tonSdkTestCases = gson.fromJson(fileContentWithUseCases, TonSdkTestCases.class);

        String testId = "address-2";
        TonSdkTestCases.TestCase testCase = tonSdkTestCases.getTestCases().get(testId);

        String description = testCase.getDescription();

        log.info("testId: {}", testId);
        log.info("description: {}", description);

        Address address = Address.of((String) testCase.getInput().get("inputRawAddress"));

        log.info("input parameters:");
        log.info("  address: {}", address.toRaw());

        String expectedBounceableUrlSafe = (String) testCase.getExpectedOutput().get("bounceableUrlSafe");
        String expectedNonBounceableUrlSafe = (String) testCase.getExpectedOutput().get("nonBounceableUrlSafe");
        String expectedBounceable = (String) testCase.getExpectedOutput().get("bounceable");
        String expectedNonBounceable = (String) testCase.getExpectedOutput().get("nonBounceable");

        assertThat(expectedBounceableUrlSafe).isEqualTo(address.toBounceable());
        assertThat(expectedNonBounceableUrlSafe).isEqualTo(address.toNonBounceable());
        assertThat(expectedBounceable).isEqualTo(address.toString(true, false, true));
        assertThat(expectedNonBounceable).isEqualTo(address.toString(true, false, false));
    }

    @Test(expected = Error.class)
    public void testAddress3() {

        TonSdkTestCases tonSdkTestCases = gson.fromJson(fileContentWithUseCases, TonSdkTestCases.class);

        String testId = "address-3";
        TonSdkTestCases.TestCase testCase = tonSdkTestCases.getTestCases().get(testId);

        String description = testCase.getDescription();

        log.info("testId: {}", testId);
        log.info("description: {}", description);

        Address address = Address.of((String) testCase.getInput().get("inputRawAddress"));
        log.info("address {}", address);
    }

    @Test(expected = Error.class)
    public void testAddress4() {

        TonSdkTestCases tonSdkTestCases = gson.fromJson(fileContentWithUseCases, TonSdkTestCases.class);

        String testId = "address-4";
        TonSdkTestCases.TestCase testCase = tonSdkTestCases.getTestCases().get(testId);

        String description = testCase.getDescription();

        log.info("testId: {}", testId);
        log.info("description: {}", description);

        Address address = Address.of((String) testCase.getInput().get("inputRawAddress"));
        log.info("address {}", address);
    }

    @Test(expected = Error.class)
    public void testAddress5() {

        TonSdkTestCases tonSdkTestCases = gson.fromJson(fileContentWithUseCases, TonSdkTestCases.class);

        String testId = "address-5";
        TonSdkTestCases.TestCase testCase = tonSdkTestCases.getTestCases().get(testId);

        String description = testCase.getDescription();

        log.info("testId: {}", testId);
        log.info("description: {}", description);

        Address address = Address.of((String) testCase.getInput().get("inputRawAddress"));
        log.info("address {}", address);
    }

    @Test
    public void testAddress6() {

        TonSdkTestCases tonSdkTestCases = gson.fromJson(fileContentWithUseCases, TonSdkTestCases.class);

        String testId = "address-6";
        TonSdkTestCases.TestCase testCase = tonSdkTestCases.getTestCases().get(testId);

        String description = testCase.getDescription();

        log.info("testId: {}", testId);
        log.info("description: {}", description);

        Address address = Address.of((String) testCase.getInput().get("inputRawAddress"));

        log.info("input parameters:");
        log.info("  address: {}", address.toRaw());

        String expectedBounceableUrlSafe = (String) testCase.getExpectedOutput().get("bounceableUrlSafe");
        String expectedNonBounceableUrlSafe = (String) testCase.getExpectedOutput().get("nonBounceableUrlSafe");
        String expectedBounceable = (String) testCase.getExpectedOutput().get("bounceable");
        String expectedNonBounceable = (String) testCase.getExpectedOutput().get("nonBounceable");

        assertThat(expectedBounceableUrlSafe).isEqualTo(address.toBounceable());
        assertThat(expectedNonBounceableUrlSafe).isEqualTo(address.toNonBounceable());
        assertThat(expectedBounceable).isEqualTo(address.toString(true, false, true));
        assertThat(expectedNonBounceable).isEqualTo(address.toString(true, false, false));
    }

    @Test
    public void testAddress7() {

        TonSdkTestCases tonSdkTestCases = gson.fromJson(fileContentWithUseCases, TonSdkTestCases.class);

        String testId = "address-7";
        TonSdkTestCases.TestCase testCase = tonSdkTestCases.getTestCases().get(testId);

        String description = testCase.getDescription();

        log.info("testId: {}", testId);
        log.info("description: {}", description);

        Address address = Address.of((String) testCase.getInput().get("inputRawAddress"));

        log.info("input parameters:");
        log.info("  address: {}", address.toRaw());

        String expectedBounceableUrlSafe = (String) testCase.getExpectedOutput().get("bounceableUrlSafe");
        String expectedNonBounceableUrlSafe = (String) testCase.getExpectedOutput().get("nonBounceableUrlSafe");
        String expectedBounceable = (String) testCase.getExpectedOutput().get("bounceable");
        String expectedNonBounceable = (String) testCase.getExpectedOutput().get("nonBounceable");

        assertThat(expectedBounceableUrlSafe).isEqualTo(address.toBounceable());
        assertThat(expectedNonBounceableUrlSafe).isEqualTo(address.toNonBounceable());
        assertThat(expectedBounceable).isEqualTo(address.toString(true, false, true));
        assertThat(expectedNonBounceable).isEqualTo(address.toString(true, false, false));
    }

    @Test
    public void testAddress8() {

        TonSdkTestCases tonSdkTestCases = gson.fromJson(fileContentWithUseCases, TonSdkTestCases.class);

        String testId = "address-8";
        TonSdkTestCases.TestCase testCase = tonSdkTestCases.getTestCases().get(testId);

        String description = testCase.getDescription();

        log.info("testId: {}", testId);
        log.info("description: {}", description);

        Address address = Address.of((String) testCase.getInput().get("bounceableUrlSafe"));

        log.info("input parameters:");
        log.info("  address: {}", address.toBounceable());

        String expectedRawAddress = (String) testCase.getExpectedOutput().get("rawAddress");

        assertThat(expectedRawAddress).isEqualTo(address.toRaw());
    }

    @Test
    public void testAddress9() {

        TonSdkTestCases tonSdkTestCases = gson.fromJson(fileContentWithUseCases, TonSdkTestCases.class);

        String testId = "address-9";
        TonSdkTestCases.TestCase testCase = tonSdkTestCases.getTestCases().get(testId);

        String description = testCase.getDescription();

        log.info("testId: {}", testId);
        log.info("description: {}", description);

        Address address = Address.of((String) testCase.getInput().get("nonBounceable"));

        log.info("input parameters:");
        log.info("  address: {}", address.toBounceable());

        String expectedRawAddress = (String) testCase.getExpectedOutput().get("rawAddress");

        assertThat(expectedRawAddress).isEqualTo(address.toRaw());
    }

    @Test(expected = Error.class)
    public void testAddress10() {

        TonSdkTestCases tonSdkTestCases = gson.fromJson(fileContentWithUseCases, TonSdkTestCases.class);

        String testId = "address-10";
        TonSdkTestCases.TestCase testCase = tonSdkTestCases.getTestCases().get(testId);

        String description = testCase.getDescription();

        log.info("testId: {}", testId);
        log.info("description: {}", description);

        Address address = Address.of((String) testCase.getInput().get("bounceableUrlSafe"));

        log.info("input parameters:");
        log.info("  address: {}", address.toBounceable());
    }

    @Test(expected = Error.class)
    public void testAddress11() {

        TonSdkTestCases tonSdkTestCases = gson.fromJson(fileContentWithUseCases, TonSdkTestCases.class);

        String testId = "address-11";
        TonSdkTestCases.TestCase testCase = tonSdkTestCases.getTestCases().get(testId);

        String description = testCase.getDescription();

        log.info("testId: {}", testId);
        log.info("description: {}", description);

        Address address = Address.of((String) testCase.getInput().get("bounceableUrlSafe"));

        log.info("input parameters:");
        log.info("  address: {}", address.toBounceable());
    }

    @Test(expected = Error.class)
    public void testAddress12() {

        TonSdkTestCases tonSdkTestCases = gson.fromJson(fileContentWithUseCases, TonSdkTestCases.class);

        String testId = "address-12";
        TonSdkTestCases.TestCase testCase = tonSdkTestCases.getTestCases().get(testId);

        String description = testCase.getDescription();

        log.info("testId: {}", testId);
        log.info("description: {}", description);

        Address address = Address.of((String) testCase.getInput().get("bounceableUrlSafe"));

        log.info("input parameters:");
        log.info("  address: {}", address.toBounceable());
    }
}
