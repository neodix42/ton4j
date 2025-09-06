package org.ton.ton4j.sdk;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.ToNumberPolicy;
import java.io.IOException;
import java.math.BigInteger;
import java.net.URL;
import java.nio.charset.Charset;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.ton.ton4j.address.Address;
import org.ton.ton4j.bitstring.BitString;
import org.ton.ton4j.cell.*;

@Slf4j
@RunWith(JUnit4.class)
public class TestTonSdkTestCasesHashmapDeserialization {
  public static final String numbersTestFileUrl =
      "https://raw.githubusercontent.com/neodix42/ton-sdk-test-cases/main/hashmap-deserialization.json";
  Gson gson = new GsonBuilder().setObjectToNumberStrategy(ToNumberPolicy.LONG_OR_DOUBLE).create();
  String fileContentWithUseCases =
      IOUtils.toString(new URL(numbersTestFileUrl), Charset.defaultCharset());
  TonSdkTestCases tonSdkTestCases = gson.fromJson(fileContentWithUseCases, TonSdkTestCases.class);

  public TestTonSdkTestCasesHashmapDeserialization() throws IOException {}

  @Test
  public void testHashmapDeserialization1() {

    String testId = "hashmap-deserialization-1";
    TonSdkTestCases.TestCase testCase = tonSdkTestCases.getTestCases().get(testId);

    String description = testCase.getDescription();

    log.info("testId: {}", testId);
    log.info("description: {}", description);

    int keySizeBits = Integer.parseInt(testCase.getInput().get("keySizeBits").toString());
    int uintValueSizeBits =
        Integer.parseInt(testCase.getInput().get("uintValueSizeBits").toString());
    String bocHex = testCase.getInput().get("bocHex").toString();

    Cell cell = CellBuilder.beginCell().fromBoc(bocHex).endCell();
    CellSlice cs = CellSlice.beginParse(cell);
    TonHashMap x =
        cs.loadDict(
            keySizeBits,
            k -> k.readUint(keySizeBits),
            v -> CellSlice.beginParse(v).loadUint(uintValueSizeBits));

    int elementsCount =
        Integer.parseInt(testCase.getExpectedOutput().get("elementsCount").toString());
    int firstElementValue =
        Integer.parseInt(testCase.getExpectedOutput().get("firstElementValue").toString());

    assertThat(x.elements.size()).isEqualTo(elementsCount);
    assertThat((BigInteger) x.elements.get(BigInteger.ONE)).isEqualTo(firstElementValue);
  }

  @Test
  public void testHashmapDeserialization2() {

    String testId = "hashmap-deserialization-2";
    TonSdkTestCases.TestCase testCase = tonSdkTestCases.getTestCases().get(testId);

    String description = testCase.getDescription();

    log.info("testId: {}", testId);
    log.info("description: {}", description);

    int keySizeBits = Integer.parseInt(testCase.getInput().get("keySizeBits").toString());
    int uintValueSizeBits =
        Integer.parseInt(testCase.getInput().get("uintValueSizeBits").toString());
    String bocHex = testCase.getInput().get("bocHex").toString();

    Cell cell = CellBuilder.beginCell().fromBoc(bocHex).endCell();
    CellSlice cs = CellSlice.beginParse(cell);
    TonHashMap x =
        cs.loadDict(
            keySizeBits,
            k -> k.readUint(keySizeBits),
            v -> CellSlice.beginParse(v).loadUint(uintValueSizeBits));

    int elementsCount =
        Integer.parseInt(testCase.getExpectedOutput().get("elementsCount").toString());
    int thirdElementKey =
        Integer.parseInt(testCase.getExpectedOutput().get("thirdElementKey").toString());
    int thirdElementValue =
        Integer.parseInt(testCase.getExpectedOutput().get("thirdElementValue").toString());

    assertThat(x.elements.size()).isEqualTo(elementsCount);
    assertThat((BigInteger) x.elements.get(BigInteger.valueOf(thirdElementKey)))
        .isEqualTo(thirdElementValue);
  }

  @Ignore("might be wrong input, todo")
  @Test
  public void testHashmapDeserialization3() {

    String testId = "hashmap-deserialization-3";
    TonSdkTestCases.TestCase testCase = tonSdkTestCases.getTestCases().get(testId);

    String description = testCase.getDescription();

    log.info("testId: {}", testId);
    log.info("description: {}", description);

    int keySizeBits = Integer.parseInt(testCase.getInput().get("keySizeBits").toString());
    int intValueSizeBits = Integer.parseInt(testCase.getInput().get("intValueSizeBits").toString());
    String bocBase64 = testCase.getInput().get("bocBase64").toString();

    Cell cell = CellBuilder.beginCell().fromBocBase64(bocBase64).endCell();
    CellSlice cs = CellSlice.beginParse(cell);
    TonHashMap x = cs.loadDict(keySizeBits, k -> k.readUint(intValueSizeBits), v -> v);

    int elementsCount =
        Integer.parseInt(testCase.getExpectedOutput().get("elementsCount").toString());
    BigInteger thirdElementKey =
        new BigInteger(testCase.getExpectedOutput().get("thirdElementKey").toString());
    BigInteger lastElementKey =
        new BigInteger(testCase.getExpectedOutput().get("lastElementKey").toString());

    assertThat(x.elements.size()).isEqualTo(elementsCount);
    assertThat((BigInteger) x.getKeyByIndex(2)).isEqualTo(thirdElementKey);
    assertThat((BigInteger) x.getKeyByIndex(13)).isEqualTo(lastElementKey);
  }

  @Test
  public void testHashmapDeserialization4() {

    String testId = "hashmap-deserialization-4";
    TonSdkTestCases.TestCase testCase = tonSdkTestCases.getTestCases().get(testId);

    String description = testCase.getDescription();

    log.info("testId: {}", testId);
    log.info("description: {}", description);

    int keySizeBits = Integer.parseInt(testCase.getInput().get("keySizeBits").toString());
    int uintValueSizeBits =
        Integer.parseInt(testCase.getInput().get("uintValueSizeBits").toString());
    String bocHex = testCase.getInput().get("bocHex").toString();

    Cell cell = CellBuilder.beginCell().fromBoc(bocHex).endCell();
    CellSlice cs = CellSlice.beginParse(cell);
    TonHashMapE x =
        cs.loadDictE(
            keySizeBits,
            k -> k.readUint(keySizeBits),
            v -> CellSlice.beginParse(v).loadUint(uintValueSizeBits));

    int elementsCount =
        Integer.parseInt(testCase.getExpectedOutput().get("elementsCount").toString());
    BigInteger firstElementValue =
        new BigInteger(testCase.getExpectedOutput().get("firstElementValue").toString());

    assertThat(x.elements.size()).isEqualTo(elementsCount);
    assertThat((BigInteger) x.getValueByIndex(0)).isEqualTo(firstElementValue);
  }

  @Test
  public void testHashmapDeserialization5() {

    String testId = "hashmap-deserialization-5";
    TonSdkTestCases.TestCase testCase = tonSdkTestCases.getTestCases().get(testId);

    String description = testCase.getDescription();

    log.info("testId: {}", testId);
    log.info("description: {}", description);

    int keySizeBits = Integer.parseInt(testCase.getInput().get("keySizeBits").toString());
    int uintValueSizeBits =
        Integer.parseInt(testCase.getInput().get("booleanValueSizeBits").toString());
    String bocHex = testCase.getInput().get("bocHex").toString();

    Cell cell = CellBuilder.beginCell().fromBoc(bocHex).endCell();
    CellSlice cs = CellSlice.beginParse(cell);
    TonPfxHashMap x = cs.loadDictPfx(keySizeBits, BitString::readAddress, v -> true);

    int elementsCount =
        Integer.parseInt(testCase.getExpectedOutput().get("elementsCount").toString());
    String firstElementKey = testCase.getExpectedOutput().get("firstElementKey").toString();

    assertThat(x.elements.size()).isEqualTo(elementsCount);
    assertThat(((Address) x.getKeyByIndex(0)).toBounceable()).isEqualTo(firstElementKey);
  }

  @Test
  public void testHashmapDeserialization6() {

    String testId = "hashmap-deserialization-6";
    TonSdkTestCases.TestCase testCase = tonSdkTestCases.getTestCases().get(testId);

    String description = testCase.getDescription();

    log.info("testId: {}", testId);
    log.info("description: {}", description);

    int keySizeBits = Integer.parseInt(testCase.getInput().get("keySizeBits").toString());
    int uintValueSizeBits =
        Integer.parseInt(testCase.getInput().get("uintValueSizeBits").toString());
    int uintEdgeSizeBits = Integer.parseInt(testCase.getInput().get("uintEdgeSizeBits").toString());
    String bocHex = testCase.getInput().get("bocHex").toString();

    Cell cell = CellBuilder.beginCell().fromBoc(bocHex).endCell();
    CellSlice cs = CellSlice.beginParse(cell);
    TonHashMapAug x =
        cs.loadDictAug(
            keySizeBits,
            k -> k.readUint(32),
            v -> CellSlice.beginParse(v).loadUint(uintValueSizeBits),
            e -> CellSlice.beginParse(e).loadUint(uintEdgeSizeBits));

    int elementsCount =
        Integer.parseInt(testCase.getExpectedOutput().get("elementsCount").toString());
    BigInteger firstElementKey =
        new BigInteger(testCase.getExpectedOutput().get("firstElementKey").toString());
    BigInteger firstElementValue =
        new BigInteger(testCase.getExpectedOutput().get("firstElementValue").toString());
    BigInteger firstElementEdge =
        new BigInteger(testCase.getExpectedOutput().get("firstElementEdge").toString());

    assertThat(x.elements.size()).isEqualTo(elementsCount);
    assertThat((BigInteger) x.getKeyByIndex(0)).isEqualTo(firstElementKey);
    assertThat((BigInteger) x.getValueByIndex(0)).isEqualTo(firstElementValue);
    assertThat((BigInteger) x.getEdgeByIndex(0)).isEqualTo(firstElementEdge);
  }

  @Test
  public void testHashmapDeserialization7() {

    String testId = "hashmap-deserialization-7";
    TonSdkTestCases.TestCase testCase = tonSdkTestCases.getTestCases().get(testId);

    String description = testCase.getDescription();

    log.info("testId: {}", testId);
    log.info("description: {}", description);

    int keySizeBits = Integer.parseInt(testCase.getInput().get("keySizeBits").toString());
    int uintValueSizeBits =
        Integer.parseInt(testCase.getInput().get("uintValueSizeBits").toString());
    int uintEdgeSizeBits = Integer.parseInt(testCase.getInput().get("uintEdgeSizeBits").toString());
    String bocHex = testCase.getInput().get("bocHex").toString();

    Cell cell = CellBuilder.beginCell().fromBoc(bocHex).endCell();
    CellSlice cs = CellSlice.beginParse(cell);
    TonHashMapAugE x =
        cs.loadDictAugE(
            keySizeBits,
            k -> k.readUint(32),
            v -> CellSlice.beginParse(v).loadUint(uintValueSizeBits),
            e -> CellSlice.beginParse(e).loadUint(uintEdgeSizeBits));

    int elementsCount =
        Integer.parseInt(testCase.getExpectedOutput().get("elementsCount").toString());
    BigInteger firstElementKey =
        new BigInteger(testCase.getExpectedOutput().get("firstElementKey").toString());
    BigInteger firstElementValue =
        new BigInteger(testCase.getExpectedOutput().get("firstElementValue").toString());
    BigInteger firstElementEdge =
        new BigInteger(testCase.getExpectedOutput().get("firstElementEdge").toString());

    assertThat(x.elements.size()).isEqualTo(elementsCount);
    assertThat((BigInteger) x.getKeyByIndex(0)).isEqualTo(firstElementKey);
    assertThat((BigInteger) x.getValueByIndex(0)).isEqualTo(firstElementValue);
    assertThat((BigInteger) x.getEdgeByIndex(0)).isEqualTo(firstElementEdge);
  }
}
