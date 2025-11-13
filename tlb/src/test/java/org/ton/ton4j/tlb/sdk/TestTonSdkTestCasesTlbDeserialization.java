package org.ton.ton4j.tlb.sdk;

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
import org.ton.ton4j.cell.Cell;
import org.ton.ton4j.cell.CellBuilder;
import org.ton.ton4j.cell.CellSlice;
import org.ton.ton4j.tlb.*;

@Slf4j
@RunWith(JUnit4.class)
public class TestTonSdkTestCasesTlbDeserialization {
  public static final String numbersTestFileUrl =
      "https://raw.githubusercontent.com/neodix42/ton-sdk-test-cases/main/tlb-deserialization.json";
  Gson gson = new GsonBuilder().setObjectToNumberStrategy(ToNumberPolicy.LONG_OR_DOUBLE).create();
  String fileContentWithUseCases =
      IOUtils.toString(new URL(numbersTestFileUrl), Charset.defaultCharset());
  TonSdkTestCases tonSdkTestCases = gson.fromJson(fileContentWithUseCases, TonSdkTestCases.class);

  public TestTonSdkTestCasesTlbDeserialization() throws IOException {}

  @Test
  public void testTlbDeserialization1() {

    String testId = "tlb-deserialization-1";
    TonSdkTestCases.TestCase testCase = tonSdkTestCases.getTestCases().get(testId);

    String description = testCase.getDescription();

    log.info("testId: {}", testId);
    log.info("description: {}", description);

    String bocAsHex = testCase.getInput().get("bocAsHex").toString();

    Cell c = CellBuilder.beginCell().fromBoc(bocAsHex).endCell();
    InternalMessageInfo internalMessageInfo =
        InternalMessageInfo.deserialize(CellSlice.beginParse(c));
    log.info("internalMessage {}", internalMessageInfo);

    Boolean ihrDisabled =
        Boolean.valueOf(testCase.getExpectedOutput().get("ihrDisabled").toString());
    Boolean bounce = Boolean.valueOf(testCase.getExpectedOutput().get("bounce").toString());
    Boolean bounced = Boolean.valueOf(testCase.getExpectedOutput().get("bounced").toString());
    String sourceAddress = testCase.getExpectedOutput().get("sourceAddress").toString();
    String destinationAddress = testCase.getExpectedOutput().get("destinationAddress").toString();
    BigInteger valueCoins =
        new BigInteger(testCase.getExpectedOutput().get("valueCoins").toString());
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
    assertThat(internalMessageInfo.getExtraFlags()).isEqualTo(ihrFee);
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
    ExternalMessageInInfo externalMessageInInfo =
        ExternalMessageInInfo.deserialize(CellSlice.beginParse(c));
    log.info("ExternalMessageInfo {}", externalMessageInInfo);

    String expectedSourceAddress = testCase.getExpectedOutput().get("sourceAddress").toString();
    String expectedDestinationAddress =
        testCase.getExpectedOutput().get("destinationAddress").toString();
    Long expectedImportFee =
        Long.parseLong(testCase.getExpectedOutput().get("importFee").toString());

    assertThat(externalMessageInInfo.getSrcAddr().toString()).isEqualTo(expectedSourceAddress);
    assertThat(externalMessageInInfo.getDstAddr().toAddress().toRaw())
        .isEqualTo(expectedDestinationAddress);
    assertThat(externalMessageInInfo.getImportFee().longValue()).isEqualTo(expectedImportFee);
  }

  @Test
  public void testTlbDeserialization3() {

    String testId = "tlb-deserialization-3";
    TonSdkTestCases.TestCase testCase = tonSdkTestCases.getTestCases().get(testId);

    String description = testCase.getDescription();

    log.info("testId: {}", testId);
    log.info("description: {}", description);

    String bocAsHex = testCase.getInput().get("bocAsHex").toString();

    Cell c = CellBuilder.beginCell().fromBoc(bocAsHex).endCell();
    Block block = Block.deserialize(CellSlice.beginParse(c));
    log.info("block {}", block);

    Boolean notMaster = Boolean.valueOf(testCase.getExpectedOutput().get("notMaster").toString());
    Long seqno = Long.parseLong(testCase.getExpectedOutput().get("seqno").toString());
    Integer prevRef_prev1_seqno =
        Integer.parseInt(testCase.getExpectedOutput().get("prevRef_prev1_seqno").toString());

    assertThat(block.getBlockInfo().isNotMaster()).isEqualTo(notMaster);
    assertThat(block.getBlockInfo().getSeqno()).isEqualTo(seqno);
    assertThat(block.getBlockInfo().getPrevRef().getPrev1().getSeqno())
        .isEqualTo(prevRef_prev1_seqno);
  }

  @Test
  public void testTlbDeserialization4() {

    String testId = "tlb-deserialization-4";
    TonSdkTestCases.TestCase testCase = tonSdkTestCases.getTestCases().get(testId);

    String description = testCase.getDescription();

    log.info("testId: {}", testId);
    log.info("description: {}", description);

    String bocAsHex = testCase.getInput().get("bocAsHex").toString();

    Cell c = CellBuilder.beginCell().fromBoc(bocAsHex).endCell();
    Block block = Block.deserialize(CellSlice.beginParse(c));
    log.info("block {}", block);

    Boolean notMaster = Boolean.valueOf(testCase.getExpectedOutput().get("notMaster").toString());
    Long genuTime = Long.parseLong(testCase.getExpectedOutput().get("genuTime").toString());
    Long startLt = Long.parseLong(testCase.getExpectedOutput().get("startLt").toString());
    Long endLt = Long.parseLong(testCase.getExpectedOutput().get("endLt").toString());
    Long genCatchainSeqno =
        Long.parseLong(testCase.getExpectedOutput().get("genCatchainSeqno").toString());

    assertThat(block.getBlockInfo().isNotMaster()).isEqualTo(notMaster);
    assertThat(block.getBlockInfo().getGenuTime()).isEqualTo(genuTime);
    assertThat(block.getBlockInfo().getStartLt().longValue()).isEqualTo(startLt);
    assertThat(block.getBlockInfo().getEndLt().longValue()).isEqualTo(endLt);
    assertThat(block.getBlockInfo().getGenCatchainSeqno()).isEqualTo(genCatchainSeqno);
  }

  @Ignore("input was generated using wrong sdk")
  @Test
  public void testTlbDeserialization5() {

    String testId = "tlb-deserialization-5";
    TonSdkTestCases.TestCase testCase = tonSdkTestCases.getTestCases().get(testId);

    String description = testCase.getDescription();

    log.info("testId: {}", testId);
    log.info("description: {}", description);

    String bocAsHex = testCase.getInput().get("bocAsHex").toString();

    Cell c = CellBuilder.beginCell().fromBoc(bocAsHex).endCell();
    Block block = Block.deserialize(CellSlice.beginParse(c));
    log.info("block {}", block);

    BigInteger valueFlow_fromPrevBlk_Coins =
        new BigInteger(testCase.getExpectedOutput().get("valueFlow_fromPrevBlk_Coins").toString());
    assertThat(block.getValueFlow().getFromPrevBlk().getCoins())
        .isEqualTo(valueFlow_fromPrevBlk_Coins);
  }
}
