package org.ton.ton4j.sdk;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.ToNumberPolicy;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.ton.ton4j.address.Address;
import org.ton.ton4j.cell.Cell;
import org.ton.ton4j.cell.CellBuilder;
import org.ton.ton4j.cell.CellSlice;
import org.ton.ton4j.utils.Utils;

import java.io.IOException;
import java.math.BigInteger;
import java.net.URL;
import java.nio.charset.Charset;

import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
@RunWith(JUnit4.class)
public class TestTonSdkTestCasesCellDeserialization {
  public static final String numbersTestFileUrl =
      "https://raw.githubusercontent.com/neodix42/ton-sdk-test-cases/main/cell-deserialization.json";
  Gson gson = new GsonBuilder().setObjectToNumberStrategy(ToNumberPolicy.LONG_OR_DOUBLE).create();
  String fileContentWithUseCases =
      IOUtils.toString(new URL(numbersTestFileUrl), Charset.defaultCharset());
  TonSdkTestCases tonSdkTestCases = gson.fromJson(fileContentWithUseCases, TonSdkTestCases.class);

  public TestTonSdkTestCasesCellDeserialization() throws IOException {}

  @Test
  public void testCellDeserialization1() {

    String testId = "cell-deserialization-1";
    TonSdkTestCases.TestCase testCase = tonSdkTestCases.getTestCases().get(testId);

    String description = testCase.getDescription();

    log.info("testId: {}", testId);
    log.info("description: {}", description);

    String bocAsHexWithCrc = testCase.getInput().get("bocAsHexWithCrc").toString();

    Cell cell = CellBuilder.beginCell().fromBoc(bocAsHexWithCrc).endCell();
    int actualInt = CellSlice.beginParse(cell).loadUint(7).intValue();

    int expectedLoadUint7 =
        Integer.parseInt(testCase.getExpectedOutput().get("loadUint7").toString());
    String expectedCellPrint = testCase.getExpectedOutput().get("bitStringToFiftHex").toString();

    assertThat(actualInt).isEqualTo(expectedLoadUint7);

    assertThat(StringUtils.trim(cell.print())).isEqualTo(expectedCellPrint);
  }

  @Test
  public void testCellDeserialization2() {

    String testId = "cell-deserialization-2";
    TonSdkTestCases.TestCase testCase = tonSdkTestCases.getTestCases().get(testId);

    String description = testCase.getDescription();

    log.info("testId: {}", testId);
    log.info("description: {}", description);

    String bocAsHexWithCrcOnly = testCase.getInput().get("bocAsHexWithCrcOnly").toString();

    Cell cell5 = CellBuilder.beginCell().fromBoc(bocAsHexWithCrcOnly).endCell();

    byte[] actualCell5Bytes = CellSlice.beginParse(cell5).loadBytes(16);

    Cell cell4 = cell5.getRefs().get(0);
    Cell cell3 = cell5.getRefs().get(1);
    Cell cell2 = cell5.getRefs().get(2);
    Cell cell1 = cell5.getRefs().get(3);

    Boolean expectedCell1Bits =
        Boolean.parseBoolean(testCase.getExpectedOutput().get("cell1_bits").toString());
    byte[] expectedCell2Bytes =
        gson.fromJson(testCase.getExpectedOutput().get("cell2_bytes").toString(), byte[].class);
    byte[] expectedCell5Bytes =
        gson.fromJson(testCase.getExpectedOutput().get("cell5_bytes").toString(), byte[].class);
    Address expectedAddressCell3 = CellSlice.beginParse(cell3).loadAddress();
    Cell cell2InsideCell3 = CellSlice.beginParse(cell3).loadRef();
    BigInteger expectedToncoinsCell4 = CellSlice.beginParse(cell4).loadCoins();
    Cell cell3InsideCell4 = CellSlice.beginParse(cell4).loadRef();

    assertThat(CellSlice.beginParse(cell1).loadBit()).isEqualTo(expectedCell1Bits);
    assertThat(CellSlice.beginParse(cell3).loadAddress()).isEqualTo(expectedAddressCell3);
    assertThat(CellSlice.beginParse(cell3InsideCell4).loadAddress())
        .isEqualTo(expectedAddressCell3);
    assertThat(CellSlice.beginParse(cell4).loadCoins()).isEqualTo(expectedToncoinsCell4);

    assertThat(CellSlice.beginParse(cell2).loadBytes(8)).isEqualTo(expectedCell2Bytes);
    assertThat(CellSlice.beginParse(cell2InsideCell3).loadBytes(8)).isEqualTo(expectedCell2Bytes);
    assertThat(actualCell5Bytes).isEqualTo(expectedCell5Bytes);
  }

  @Test
  public void testCellDeserialization3() {

    String testId = "cell-deserialization-3";
    TonSdkTestCases.TestCase testCase = tonSdkTestCases.getTestCases().get(testId);

    String description = testCase.getDescription();

    log.info("testId: {}", testId);
    log.info("description: {}", description);

    String bocAsBase64 = testCase.getInput().get("bocAsBase64").toString();
    Cell cell = CellBuilder.beginCell().fromBocBase64(bocAsBase64).endCell();

    String expectedFiftOutput = testCase.getExpectedOutput().get("expectedFiftOutput").toString();
    String convertBackToBocAsBase64 =
        testCase.getExpectedOutput().get("convertBackToBocAsBase64").toString();

    assertThat(StringUtils.trim(cell.print())).isEqualTo(expectedFiftOutput);
    //        assertThat(cell.toBase64(true)).isEqualTo(bocAsBase64);
  }

  @Test
  public void testCellDeserialization4() {

    String testId = "cell-deserialization-4";
    TonSdkTestCases.TestCase testCase = tonSdkTestCases.getTestCases().get(testId);

    String description = testCase.getDescription();

    log.info("testId: {}", testId);
    log.info("description: {}", description);

    String bocAsHex = testCase.getInput().get("bocAsHex").toString();
    CellBuilder.beginCell().fromBoc(bocAsHex).endCell();
  }

  @Test
  public void testCellDeserialization5() {

    String testId = "cell-deserialization-5";
    TonSdkTestCases.TestCase testCase = tonSdkTestCases.getTestCases().get(testId);

    String description = testCase.getDescription();

    log.info("testId: {}", testId);
    log.info("description: {}", description);

    String bocAsHexWithCrc = testCase.getInput().get("bocAsHexWithCrc").toString();
    Cell cell = CellBuilder.beginCell().fromBoc(bocAsHexWithCrc).endCell();

    String expectedBocAsHexWithCrc = testCase.getExpectedOutput().get("bocAsHexWithCrc").toString();

    assertThat(cell.toHex(true).toUpperCase()).isEqualTo(expectedBocAsHexWithCrc);
  }

  @Test
  public void testCellDeserialization6() {

    String testId = "cell-deserialization-6";
    TonSdkTestCases.TestCase testCase = tonSdkTestCases.getTestCases().get(testId);

    String description = testCase.getDescription();

    log.info("testId: {}", testId);
    log.info("description: {}", description);

    String bocAsHex = testCase.getInput().get("bocAsHex").toString();
    Cell cell = CellBuilder.beginCell().fromBoc(bocAsHex).endCell();

    String expectedHash = testCase.getExpectedOutput().get("hash").toString();

    assertThat(Utils.bytesToHex(cell.getHash())).isEqualTo(expectedHash);
  }

  @Test
  public void testCellDeserialization7() {

    String testId = "cell-deserialization-7";
    TonSdkTestCases.TestCase testCase = tonSdkTestCases.getTestCases().get(testId);

    String description = testCase.getDescription();

    log.info("testId: {}", testId);
    log.info("description: {}", description);

    String bocAsBse64 = testCase.getInput().get("bocAsBase64").toString();
    Cell cell = CellBuilder.beginCell().fromBocBase64(bocAsBse64).endCell();

    String expectedHash = testCase.getExpectedOutput().get("hash").toString();

    // hash does not coincide with the fift result, needs to be fixed
    assertThat(Utils.bytesToHex(cell.getHash()).toUpperCase()).isEqualTo(expectedHash);
  }

  @Test
  public void testCellDeserialization8() {

    String testId = "cell-deserialization-8";
    TonSdkTestCases.TestCase testCase = tonSdkTestCases.getTestCases().get(testId);

    String description = testCase.getDescription();

    log.info("testId: {}", testId);
    log.info("description: {}", description);

    String bocAsHexWithCrc = testCase.getInput().get("bocAsHexWithCrc").toString();
    Cell cell = CellBuilder.beginCell().fromBoc(bocAsHexWithCrc).endCell();

    String expectedHash = testCase.getExpectedOutput().get("hash").toString();
    int expectedRefsSize =
        Integer.parseInt(testCase.getExpectedOutput().get("sizeOfRefs").toString());

    // hash does not coincide with the fift result, needs to be fixed
    assertThat(Utils.bytesToHex(cell.getHash()).toUpperCase()).isEqualTo(expectedHash);
    assertThat(cell.getRefs().size()).isEqualTo(expectedRefsSize);
  }

  @Test
  public void testCellDeserialization9() {

    String testId = "cell-deserialization-9";
    TonSdkTestCases.TestCase testCase = tonSdkTestCases.getTestCases().get(testId);

    String description = testCase.getDescription();

    log.info("testId: {}", testId);
    log.info("description: {}", description);

    String bocAsHexWithCrc = testCase.getInput().get("bocAsHexWithCrc").toString();
    Cell cell = CellBuilder.beginCell().fromBoc(bocAsHexWithCrc).endCell();

    int expectedCellBitLength =
        Integer.parseInt(testCase.getExpectedOutput().get("cellBitLength").toString());

    assertThat(cell.getBitLength()).isEqualTo(expectedCellBitLength);
  }
}
