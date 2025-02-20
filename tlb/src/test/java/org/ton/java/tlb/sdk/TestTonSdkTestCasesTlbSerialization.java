package org.ton.java.tlb.sdk;

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
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.ton.java.tlb.*;
import org.ton.java.utils.Utils;

@Slf4j
@RunWith(JUnit4.class)
public class TestTonSdkTestCasesTlbSerialization {
  public static final String numbersTestFileUrl =
      "https://raw.githubusercontent.com/neodix42/ton-sdk-test-cases/main/tlb-serialization.json";
  Gson gson = new GsonBuilder().setObjectToNumberStrategy(ToNumberPolicy.LONG_OR_DOUBLE).create();
  String fileContentWithUseCases =
      IOUtils.toString(new URL(numbersTestFileUrl), Charset.defaultCharset());
  TonSdkTestCases tonSdkTestCases = gson.fromJson(fileContentWithUseCases, TonSdkTestCases.class);

  public TestTonSdkTestCasesTlbSerialization() throws IOException {}

  @Test
  public void testTlbSerialization1() {

    String testId = "tlb-serialization-1";
    TonSdkTestCases.TestCase testCase = tonSdkTestCases.getTestCases().get(testId);

    String description = testCase.getDescription();

    log.info("testId: {}", testId);
    log.info("description: {}", description);

    String addr1_none = testCase.getInput().get("addr1_none").toString();
    BigInteger addr2_external_len =
        new BigInteger(testCase.getInput().get("addr2_external_len").toString());
    String addr2_external_bits = testCase.getInput().get("addr2_external_bits").toString();
    BigInteger addr3_std_wc = new BigInteger(testCase.getInput().get("addr3_std_wc").toString());
    String addr3_std_address_bits256 =
        testCase.getInput().get("addr3_std_address_bits256").toString();
    BigInteger addr4_var_wc = new BigInteger(testCase.getInput().get("addr4_var_wc").toString());
    BigInteger addr4_var_len = new BigInteger(testCase.getInput().get("addr4_var_len").toString());
    String addr4_var_bits = testCase.getInput().get("addr4_var_bits").toString();
    BigInteger addr5_anycast_depth =
        new BigInteger(testCase.getInput().get("addr5_anycast_depth").toString());
    String addr5_anycast_pfx_depth = testCase.getInput().get("addr5_anycast_pfx_depth").toString();
    BigInteger addr5_std_wc = new BigInteger(testCase.getInput().get("addr5_std_wc").toString());
    String addr5_std_address_bits256 =
        testCase.getInput().get("addr5_std_address_bits256").toString();
    boolean bocWithCrc = Boolean.parseBoolean(testCase.getInput().get("bocWithCrc").toString());

    MsgAddress addr1 = MsgAddressExtNone.builder().build();

    MsgAddress addr2 =
        MsgAddressExternal.builder()
            .len(addr2_external_len.intValue())
            .externalAddress(new BigInteger(Utils.bitStringToHex(addr2_external_bits), 16))
            .build();

    MsgAddress addr3 =
        MsgAddressIntStd.builder()
            .workchainId(addr3_std_wc.byteValue())
            .address(new BigInteger(addr3_std_address_bits256, 16))
            .build();

    MsgAddress addr4 =
        MsgAddressIntVar.builder()
            .workchainId(addr4_var_wc.byteValue())
            .addrLen(addr4_var_len.intValue())
            .address(new BigInteger(addr4_var_bits, 2))
            .build();

    MsgAddress addr5 =
        MsgAddressIntStd.builder()
            .anycast(
                Anycast.builder()
                    .depth(addr5_anycast_depth.intValue())
                    .rewritePfx(new BigInteger(addr5_anycast_pfx_depth, 2).byteValueExact())
                    .build())
            .workchainId(addr5_std_wc.byteValue())
            .address(new BigInteger(addr5_std_address_bits256, 16))
            .build();

    String bocAsHexAddr1 = testCase.getExpectedOutput().get("bocAsHexAddr1").toString();
    String bocAsHexAddr2 = testCase.getExpectedOutput().get("bocAsHexAddr2").toString();
    String bocAsHexAddr3 = testCase.getExpectedOutput().get("bocAsHexAddr3").toString();
    String bocAsHexAddr4 = testCase.getExpectedOutput().get("bocAsHexAddr4").toString();
    String bocAsHexAddr5 = testCase.getExpectedOutput().get("bocAsHexAddr5").toString();

    assertThat(addr1.toCell().toHex(bocWithCrc)).isEqualTo(bocAsHexAddr1);
    assertThat(addr2.toCell().toHex(bocWithCrc)).isEqualTo(bocAsHexAddr2);
    assertThat(addr3.toCell().toHex(bocWithCrc)).isEqualTo(bocAsHexAddr3);
    assertThat(addr4.toCell().toHex(bocWithCrc)).isEqualTo(bocAsHexAddr4);
    assertThat(addr5.toCell().toHex(bocWithCrc)).isEqualTo(bocAsHexAddr5);
  }

  @Test
  public void testTlbSerialization2() {

    String testId = "tlb-serialization-2";
    TonSdkTestCases.TestCase testCase = tonSdkTestCases.getTestCases().get(testId);

    String description = testCase.getDescription();

    log.info("testId: {}", testId);
    log.info("description: {}", description);

    // input
    BigInteger shard1Prefix = new BigInteger(testCase.getInput().get("shard1_prefix").toString());
    BigInteger shard1PrefixBits =
        new BigInteger(testCase.getInput().get("shard1_prefix_bits").toString());
    BigInteger shard2Prefix = new BigInteger(testCase.getInput().get("shard2_prefix").toString());
    BigInteger shard2PrefixBits =
        new BigInteger(testCase.getInput().get("shard2_prefix_bits").toString());
    BigInteger shard3Prefix = new BigInteger(testCase.getInput().get("shard3_prefix").toString());
    BigInteger shard3PrefixBits =
        new BigInteger(testCase.getInput().get("shard3_prefix_bits").toString());
    String shard1PrefixAsHex = testCase.getInput().get("shard1_prefix_as_hex").toString();
    String shard4PrefixAsHex = testCase.getInput().get("shard4_prefix_as_hex").toString();

    // expected output
    String expectedShard1Hex = testCase.getExpectedOutput().get("shard1_hex").toString();
    String expectedShard2Hex = testCase.getExpectedOutput().get("shard2_hex").toString();
    String expectedShard3Hex = testCase.getExpectedOutput().get("shard3_hex").toString();
    String expectedShard1ChildLeft =
        testCase.getExpectedOutput().get("shard1_child_left").toString();
    String expectedShard1ChildRight =
        testCase.getExpectedOutput().get("shard1_child_right").toString();
    BigInteger expectedShard1Parent =
        new BigInteger(testCase.getExpectedOutput().get("shard1_parent").toString());
    String expectedShard2Parent = testCase.getExpectedOutput().get("shard2_parent").toString();
    String expectedShard3Parent = testCase.getExpectedOutput().get("shard3_parent").toString();
    String expectedShard4ChildLeft =
        testCase.getExpectedOutput().get("shard4_child_left").toString();
    String expectedShard4ChildRight =
        testCase.getExpectedOutput().get("shard4_child_right").toString();
    String expectedShard4Parent = testCase.getExpectedOutput().get("shard4_parent").toString();

    // testing
    ShardIdent shard2_4000000000000000 =
        ShardIdent.builder()
            .shardPrefix(shard2Prefix)
            .prefixBits(shard2PrefixBits.intValue())
            .build();
    assertThat(shard2_4000000000000000.convertShardIdentToShard().toString(16))
        .isEqualTo(expectedShard2Hex);
    assertThat(shard2_4000000000000000.getParent().toString(16)).isEqualTo(expectedShard2Parent);

    ShardIdent shard3_c000000000000000 =
        ShardIdent.builder()
            .shardPrefix(shard3Prefix)
            .prefixBits(shard3PrefixBits.intValue())
            .build();
    assertThat(shard3_c000000000000000.convertShardIdentToShard().toString(16))
        .isEqualTo(expectedShard3Hex);
    assertThat(shard3_c000000000000000.getParent().toString(16)).isEqualTo(expectedShard3Parent);

    // root
    ShardIdent shard1_root =
        ShardIdent.builder()
            .shardPrefix(shard1Prefix)
            .prefixBits(shard1PrefixBits.intValue())
            .build();
    assertThat(shard1_root.convertShardIdentToShard().toString(16)).isEqualTo(expectedShard1Hex);
    assertThat(shard1_root.getParent()).isEqualTo(expectedShard1Parent);

    ShardIdent shard1_root2 = ShardIdent.convertShardToShardIdent(shard1PrefixAsHex, 0);
    assertThat(shard1_root2.getChildLeft().toString(16)).isEqualTo(expectedShard1ChildLeft);
    assertThat(shard1_root2.getChildRight().toString(16)).isEqualTo(expectedShard1ChildRight);
    assertThat(shard1_root2.getParent()).isEqualTo(expectedShard1Parent);

    // child left/right
    assertThat(shard2_4000000000000000.convertShardIdentToShard().toString(16))
        .isEqualTo(shard1_root.getChildLeft().toString(16));
    assertThat(shard3_c000000000000000.convertShardIdentToShard().toString(16))
        .isEqualTo(shard1_root.getChildRight().toString(16));

    ShardIdent shard4_6000000000000000 = ShardIdent.convertShardToShardIdent(shard4PrefixAsHex, 0);
    assertThat(shard4_6000000000000000.getChildLeft().toString(16))
        .isEqualTo(expectedShard4ChildLeft);
    assertThat(shard4_6000000000000000.getChildRight().toString(16))
        .isEqualTo(expectedShard4ChildRight);
    assertThat(shard4_6000000000000000.getParent().toString(16)).isEqualTo(expectedShard4Parent);
  }
}
