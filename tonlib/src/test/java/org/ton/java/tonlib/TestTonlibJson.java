package org.ton.java.tonlib;

import static java.util.Objects.nonNull;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.iwebpp.crypto.TweetNaclFast;
import com.sun.jna.*;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.ton.java.address.Address;
import org.ton.java.cell.Cell;
import org.ton.java.cell.CellBuilder;
import org.ton.java.cell.CellSlice;
import org.ton.java.mnemonic.Mnemonic;
import org.ton.java.tlb.types.ConfigParams8;
import org.ton.java.tlb.types.Transaction;
import org.ton.java.tonlib.types.*;
import org.ton.java.tonlib.types.globalconfig.*;
import org.ton.java.utils.Utils;

@Slf4j
@RunWith(JUnit4.class)
public class TestTonlibJson {

  public static final String TON_FOUNDATION = "EQCD39VS5jcptHL8vMjEXrzGaRcCVYto7HUn4bpAOg8xqB2N";
  public static final String ELECTOR_ADDRESSS =
      "-1:3333333333333333333333333333333333333333333333333333333333333333";

  Gson gs = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

  static Tonlib tonlib;

  static String tonlibPath = Utils.getArtifactGithubUrl("tonlibjson", "latest", "neodix42", "ton");

  @BeforeClass
  public static void setUpBeforeClass() {

    tonlib =
        Tonlib.builder()
            .pathToTonlibSharedLib(tonlibPath)
            .receiveTimeout(5)
            .ignoreCache(false)
            .build();
  }

  @Test
  public void testIssue13() {
    Tonlib tonlib =
        Tonlib.builder()
            .pathToTonlibSharedLib(tonlibPath)
            .ignoreCache(false)
            .liteServerIndex(0)
            .testnet(true)
            .build();

    BlockIdExt block = tonlib.getLast().getLast();
    log.info("block {}", block);
  }

  @Test
  public void testGetLiteServerVersion() {
    LiteServerVersion liteServerVersion = tonlib.getLiteServerVersion();
    log.info("liteServerVersion {}", liteServerVersion);
  }

  @Test
  public void testGlobalConfigDownload() {
    Tonlib tonlib =
        Tonlib.builder()
            .pathToTonlibSharedLib(tonlibPath)
            .pathToGlobalConfig("https://ton-blockchain.github.io/testnet-global.config.json")
            .ignoreCache(false)
            .build();

    BlockIdExt block = tonlib.getLast().getLast();
    log.info("block {}", block);
  }

  @Test
  public void testGlobalTonlibAndConfigDownload() {
    Tonlib tonlib =
        Tonlib.builder()
            .pathToTonlibSharedLib(
                "https://github.com/ton-blockchain/ton/releases/download/v2024.12-1/tonlibjson.dll")
            .pathToGlobalConfig("https://ton-blockchain.github.io/testnet-global.config.json")
            .ignoreCache(false)
            .build();

    BlockIdExt block = tonlib.getLast().getLast();
    log.info("block {}", block);
  }

  @Test
  public void testInitTonlibJson() throws IOException {

    TonlibJsonI tonlibJson = Native.load("tonlibjson.dll", TonlibJsonI.class);

    Pointer tonlib = tonlibJson.tonlib_client_json_create();

    tonlibJson.tonlib_client_set_verbosity_level(4);

    InputStream testConfig =
        TestTonlibJson.class.getClassLoader().getResourceAsStream("testnet-global.config.json");

    assert nonNull(testConfig);

    String globalConfigJson = Utils.streamToString(testConfig);
    testConfig.close();

    assert nonNull(globalConfigJson);

    TonlibSetup tonlibSetup =
        TonlibSetup.builder()
            .type("init")
            .options(
                TonlibOptions.builder()
                    .type("options")
                    .config(
                        TonlibConfig.builder()
                            .type("config")
                            .config(globalConfigJson)
                            .use_callbacks_for_network(false)
                            .blockchain_name("")
                            .ignore_cache(true)
                            .build())
                    .keystore_type(
                        KeyStoreTypeDirectory.builder()
                            .type("keyStoreTypeDirectory")
                            .directory(".")
                            .build())
                    .build())
            .build();

    tonlibJson.tonlib_client_set_verbosity_level(4);

    log.info("init:{}", gs.toJson(tonlibSetup));

    tonlibJson.tonlib_client_json_send(tonlib, gs.toJson(tonlibSetup));
    String result = tonlibJson.tonlib_client_json_receive(tonlib, 10.0);
    assertThat(result).isNotBlank();

    String q =
        "{\"@type\":\"blocks.lookupBlock\",\"mode\":1,\"id\":{\"@type\":\"ton.blockId\",\"workchain\":-1,\"shard\":-9223372036854775808,\"seqno\":42555714},\"lt\":0,\"utime\":0,\"@extra\":\"94e27988-cf7a-4205-9f89-35dab8037c16\"}";

    tonlibJson.tonlib_client_json_send(tonlib, q);
    result = tonlibJson.tonlib_client_json_receive(tonlib, 10.0);
    log.info("result {}", result);
    tonlibJson.tonlib_client_json_destroy(tonlib);
  }

  @Test
  public void testGlobalConfigJsonParser() throws IOException {

    InputStream testConfig =
        TestTonlibJson.class.getClassLoader().getResourceAsStream("testnet-global.config.json");
    String configStr = Utils.streamToString(testConfig);
    assert testConfig != null;
    testConfig.close();
    TonGlobalConfig globalConfig = gs.fromJson(configStr, TonGlobalConfig.class);
    log.info("config object: {}", globalConfig);

    log.info("lite-servers found {}", globalConfig.getLiteservers().length);
    log.info("dht-servers found {}", globalConfig.getDht().getStatic_nodes().getNodes().length);
    log.info("hard-forks found {}", globalConfig.getValidator().getHardforks().length);

    log.info("parsed config object back to json {}", gs.toJson(globalConfig));

    Tonlib tonlib1 =
        Tonlib.builder()
            .pathToTonlibSharedLib("G:\\libs\\testnet-tonlibjson.dll")
            .globalConfigAsString(gs.toJson(globalConfig))
            .ignoreCache(false)
            .build();

    log.info("last {}", tonlib1.getLast());
  }

  @Test
  public void testManualGlobalConfig() {

    TonGlobalConfig testnetGlobalConfig =
        TonGlobalConfig.builder()
            .liteservers(
                ArrayUtils.toArray(
                    LiteServers.builder()
                        .ip(1495755568)
                        .port(4695)
                        .id(
                            LiteServerId.builder()
                                .type("pub.ed25519")
                                .key("cZpMFqy6n0Lsu8x/z2Jq0wh/OdM1WAVJJKSb2CvDECQ=")
                                .build())
                        .build(),
                    LiteServers.builder()
                        .ip(1468571697)
                        .port(27787)
                        .id(
                            LiteServerId.builder()
                                .type("pub.ed25519")
                                .key("Y/QVf6G5VDiKTZOKitbFVm067WsuocTN8Vg036A4zGk=")
                                .build())
                        .build()))
            .validator(
                Validator.builder()
                    .type("validator.config.global")
                    .zero_state(
                        BlockInfo.builder()
                            .file_hash("Z+IKwYS54DmmJmesw/nAD5DzWadnOCMzee+kdgSYDOg=")
                            .root_hash("gj+B8wb/AmlPk1z1AhVI484rhrUpgSr2oSFIh56VoSg=")
                            .workchain(-1)
                            .seqno(0)
                            .shard(-9223372036854775808L)
                            .build())
                    .hardforks(
                        ArrayUtils.toArray(
                            BlockInfo.builder()
                                .file_hash("jF3RTD+OyOoP+OI9oIjdV6M8EaOh9E+8+c3m5JkPYdg=")
                                .root_hash("6JSqIYIkW7y8IorxfbQBoXiuY3kXjcoYgQOxTJpjXXA=")
                                .workchain(-1)
                                .seqno(5141579)
                                .shard(-9223372036854775808L)
                                .build(),
                            BlockInfo.builder()
                                .file_hash("WrNoMrn5UIVPDV/ug/VPjYatvde8TPvz5v1VYHCLPh8=")
                                .root_hash("054VCNNtUEwYGoRe1zjH+9b1q21/MeM+3fOo76Vcjes=")
                                .workchain(-1)
                                .seqno(5172980)
                                .shard(-9223372036854775808L)
                                .build(),
                            BlockInfo.builder()
                                .file_hash("xRaxgUwgTXYFb16YnR+Q+VVsczLl6jmYwvzhQ/ncrh4=")
                                .root_hash("SoPLqMe9Dz26YJPOGDOHApTSe5i0kXFtRmRh/zPMGuI=")
                                .workchain(-1)
                                .seqno(5176527)
                                .shard(-9223372036854775808L)
                                .build()))
                    .build())
            .build();
    log.info("config object: {}", testnetGlobalConfig);

    log.info("lite-servers found {}", testnetGlobalConfig.getLiteservers().length);

    log.info("parsed config object back to json {}", gs.toJson(testnetGlobalConfig));

    Tonlib tonlib1 =
        Tonlib.builder()
            .pathToTonlibSharedLib(tonlibPath)
            .globalConfig(testnetGlobalConfig)
            .ignoreCache(false)
            .build();

    log.info("last {}", tonlib1.getLast());
  }

  @Test
  public void testTonlibUsingGlobalConfigLiteServerByIndex() {

    Tonlib tonlib1 =
        Tonlib.builder()
            .ignoreCache(false)
            .pathToTonlibSharedLib(tonlibPath)
            .testnet(true)
            .liteServerIndex(3)
            .build();

    log.info("last {}", tonlib1.getLast());
  }

  @Test
  public void testTonlib() {
    Tonlib tonlib =
        Tonlib.builder()
            .pathToTonlibSharedLib(tonlibPath)
            .receiveTimeout(5)
            .ignoreCache(false)
            .testnet(true)
            .build();

    BlockIdExt fullblock = tonlib.lookupBlock(23512606, -1, -9223372036854775808L, 0, 0);

    log.info(fullblock.toString());

    MasterChainInfo masterChainInfo = tonlib.getLast();
    log.info(masterChainInfo.toString());

    BlockHeader header = tonlib.getBlockHeader(masterChainInfo.getLast());
    log.info(header.toString());

    Shards shards = tonlib.getShards(masterChainInfo.getLast()); // only seqno also ok?
    log.info(shards.toString());
    assertThat(shards.getShards()).isNotNull();
  }

  @Test
  public void testTonlibGetLast() {
    Tonlib tonlib =
        Tonlib.builder()
            .testnet(true)
            .pathToTonlibSharedLib(tonlibPath)
            .keystoreInMemory(true)
            .build();
    BlockIdExt fullblock = tonlib.getLast().getLast();
    log.info("last {}", fullblock);
    assertThat(fullblock).isNotNull();
  }

  @Test
  public void testTonlibGetAllBlockTransactions() {
    Tonlib tonlib =
        Tonlib.builder()
            .pathToTonlibSharedLib(tonlibPath)
            .ignoreCache(false)
            .liteServerIndex(0)
            .build();
    BlockIdExt fullblock = tonlib.getLast().getLast();
    assertThat(fullblock).isNotNull();

    log.info(fullblock.toString());

    Map<String, RawTransactions> txs = tonlib.getAllBlockTransactions(fullblock, 100, null);
    for (Map.Entry<String, RawTransactions> entry : txs.entrySet()) {
      for (RawTransaction tx : entry.getValue().getTransactions()) {
        if (nonNull(tx.getIn_msg())
            && (!tx.getIn_msg().getSource().getAccount_address().equals(""))) {
          log.info(
              "{} <<<<< {} : {} ",
              tx.getIn_msg().getSource().getAccount_address(),
              tx.getIn_msg().getDestination().getAccount_address(),
              Utils.formatNanoValue(tx.getIn_msg().getValue(), 9));
        }
        if (nonNull(tx.getOut_msgs())) {
          for (RawMessage msg : tx.getOut_msgs()) {
            log.info(
                "{} >>>>> {} : {} ",
                msg.getSource().getAccount_address(),
                msg.getDestination().getAccount_address(),
                Utils.formatNanoValue(msg.getValue()));
          }
        }
      }
    }
    assertThat(txs.size()).isNotEqualTo(0);
  }

  @Test
  public void testTonlibGetBlockTransactions() {
    for (int i = 0; i < 2; i++) {

      MasterChainInfo lastBlock = tonlib.getLast();
      log.info(lastBlock.toString());

      BlockTransactions blockTransactions = tonlib.getBlockTransactions(lastBlock.getLast(), 100);
      log.info(gs.toJson(blockTransactions));

      for (ShortTxId shortTxId : blockTransactions.getTransactions()) {
        Address acccount = Address.of("-1:" + Utils.base64ToHexString(shortTxId.getAccount()));
        log.info(
            "lt {}, hash {}, account {}",
            shortTxId.getLt(),
            shortTxId.getHash(),
            acccount.toString(false));
        RawTransactions rawTransactions =
            tonlib.getRawTransactions(
                acccount.toString(false),
                BigInteger.valueOf(shortTxId.getLt()),
                shortTxId.getHash());
        for (RawTransaction tx : rawTransactions.getTransactions()) {
          if (nonNull(tx.getIn_msg())
              && (!tx.getIn_msg().getSource().getAccount_address().equals(""))) {
            log.info(
                "{}, {} <<<<< {} : {} ",
                Utils.toUTC(tx.getUtime()),
                tx.getIn_msg().getSource().getAccount_address(),
                tx.getIn_msg().getDestination().getAccount_address(),
                Utils.formatNanoValue(tx.getIn_msg().getValue()));
          }
          if (nonNull(tx.getOut_msgs())) {
            for (RawMessage msg : tx.getOut_msgs()) {
              log.info(
                  "{}, {} >>>>> {} : {} ",
                  Utils.toUTC(tx.getUtime()),
                  msg.getSource().getAccount_address(),
                  msg.getDestination().getAccount_address(),
                  Utils.formatNanoValue(msg.getValue()));
            }
          }
        }
      }
      Utils.sleep(10, "wait for next block");
    }
  }

  @Test
  public void testTonlibGetTxsByAddress() {
    Address address = Address.of(TON_FOUNDATION);

    log.info("address: " + address.toBounceable());

    RawTransactions rawTransactions = tonlib.getRawTransactions(address.toRaw(), null, null);

    log.info("total txs: {}", rawTransactions.getTransactions().size());

    for (RawTransaction tx : rawTransactions.getTransactions()) {
      if (nonNull(tx.getIn_msg())
          && (!tx.getIn_msg().getSource().getAccount_address().equals(""))) {
        log.info(
            "{}, {} <<<<< {} : {} ",
            Utils.toUTC(tx.getUtime()),
            tx.getIn_msg().getSource().getAccount_address(),
            tx.getIn_msg().getDestination().getAccount_address(),
            Utils.formatNanoValue(tx.getIn_msg().getValue()));
      }
      if (nonNull(tx.getOut_msgs())) {
        for (RawMessage msg : tx.getOut_msgs()) {
          log.info(
              "{}, {} >>>>> {} : {} ",
              Utils.toUTC(tx.getUtime()),
              msg.getSource().getAccount_address(),
              msg.getDestination().getAccount_address(),
              Utils.formatNanoValue(msg.getValue()));
        }
      }
    }

    assertThat(rawTransactions.getTransactions().size()).isLessThan(20);
  }

  @Test
  public void testTonlibGetTxsByAddressTestnet() {

    Tonlib tonlib =
        Tonlib.builder()
            .pathToTonlibSharedLib(tonlibPath)
            .receiveTimeout(5)
            .ignoreCache(false)
            .testnet(true)
            .build();
    Address address =
        Address.of("0:b52a16ba3735501df19997550e7ed4c41754ee501ded8a841088ce4278b66de4");

    log.info("address: " + address.toBounceable());

    RawTransactions rawTransactions = tonlib.getRawTransactions(address.toRaw(), null, null);

    log.info("total txs: {}", rawTransactions.getTransactions().size());

    for (RawTransaction tx : rawTransactions.getTransactions()) {
      if (nonNull(tx.getIn_msg())
          && (!tx.getIn_msg().getSource().getAccount_address().equals(""))) {
        log.info("rawTx {}", tx);
        log.info(
            "{}, {} <<<<< {} : {} ",
            Utils.toUTC(tx.getUtime()),
            tx.getIn_msg().getSource().getAccount_address(),
            tx.getIn_msg().getDestination().getAccount_address(),
            Utils.formatNanoValue(tx.getIn_msg().getValue()));
      }
      if (nonNull(tx.getOut_msgs())) {
        for (RawMessage msg : tx.getOut_msgs()) {
          log.info(
              "{}, {} >>>>> {} : {} ",
              Utils.toUTC(tx.getUtime()),
              msg.getSource().getAccount_address(),
              msg.getDestination().getAccount_address(),
              Utils.formatNanoValue(msg.getValue()));
        }
      }
    }
  }

  @Test
  public void testTonlibGetTxsV2ByAddressTestnet() {

    Address address =
        Address.of("0:b52a16ba3735501df19997550e7ed4c41754ee501ded8a841088ce4278b66de4");

    log.info("address: " + address.toBounceable());

    RawTransactions rawTransactions =
        tonlib.getRawTransactionsV2(address.toRaw(), null, null, 10, true);

    log.info("total txs: {}", rawTransactions.getTransactions().size());

    for (RawTransaction tx : rawTransactions.getTransactions()) {
      if (nonNull(tx.getIn_msg())
          && (!tx.getIn_msg().getSource().getAccount_address().equals(""))) {
        log.info("rawTx {}", tx);
        log.info(
            "{}, {} <<<<< {} : {} ",
            Utils.toUTC(tx.getUtime()),
            tx.getIn_msg().getSource().getAccount_address(),
            tx.getIn_msg().getDestination().getAccount_address(),
            Utils.formatNanoValue(tx.getIn_msg().getValue()));
      }
      if (nonNull(tx.getOut_msgs())) {
        for (RawMessage msg : tx.getOut_msgs()) {
          log.info(
              "{}, {} >>>>> {} : {} ",
              Utils.toUTC(tx.getUtime()),
              msg.getSource().getAccount_address(),
              msg.getDestination().getAccount_address(),
              Utils.formatNanoValue(msg.getValue()));
        }
      }
    }

    assertThat(rawTransactions.getTransactions().size()).isLessThan(20);
  }

  @Test
  public void testTonlibGetTxsWithLimitByAddress() {
    Address address = Address.of(TON_FOUNDATION);

    log.info("address: " + address.toBounceable());

    RawTransactions rawTransactions = tonlib.getRawTransactions(address.toRaw(), null, null, 3);

    for (RawTransaction tx : rawTransactions.getTransactions()) {
      Transaction transaction =
          Transaction.deserialize(
              CellSlice.beginParse(CellBuilder.beginCell().fromBocBase64(tx.getData()).endCell()));
      log.info("transaction {}", transaction);
      if (nonNull(tx.getIn_msg())
          && (!tx.getIn_msg().getSource().getAccount_address().equals(""))) {
        log.info(
            "{}, {} <<<<< {} : {} ",
            Utils.toUTC(tx.getUtime()),
            tx.getIn_msg().getSource().getAccount_address(),
            tx.getIn_msg().getDestination().getAccount_address(),
            Utils.formatNanoValue(tx.getIn_msg().getValue()));
      }
      if (nonNull(tx.getOut_msgs())) {
        for (RawMessage msg : tx.getOut_msgs()) {
          log.info(
              "{}, {} >>>>> {} : {} ",
              Utils.toUTC(tx.getUtime()),
              msg.getSource().getAccount_address(),
              msg.getDestination().getAccount_address(),
              Utils.formatNanoValue(msg.getValue()));
        }
      }
    }

    log.info("total txs: {}", rawTransactions.getTransactions().size());
    assertThat(rawTransactions.getTransactions().size()).isLessThan(4);
  }

  @Test
  public void testTonlibGetAllTxsByAddress() {
    Address address = Address.of("EQAL66-DGwFvP046ysD_o18wvwt-0A6_aJoVmQpVNIqV_ZvK");

    log.info("address: " + address.toBounceable());

    RawTransactions rawTransactions = tonlib.getAllRawTransactions(address.toRaw(), null, null, 51);

    log.info("total txs: {}", rawTransactions.getTransactions().size());

    for (RawTransaction tx : rawTransactions.getTransactions()) {
      if (nonNull(tx.getIn_msg())
          && (!tx.getIn_msg().getSource().getAccount_address().equals(""))) {
        log.info(
            "<<<<< {} - {} : {} ",
            tx.getIn_msg().getSource().getAccount_address(),
            tx.getIn_msg().getDestination().getAccount_address(),
            Utils.formatNanoValue(tx.getIn_msg().getValue()));
      }
      if (nonNull(tx.getOut_msgs())) {
        for (RawMessage msg : tx.getOut_msgs()) {
          log.info(
              ">>>>> {} - {} : {} ",
              msg.getSource().getAccount_address(),
              msg.getDestination().getAccount_address(),
              Utils.formatNanoValue(msg.getValue()));
        }
      }
    }

    assertThat(rawTransactions.getTransactions().size()).isLessThan(10);
  }

  @Test
  public void testTonlibGetAllTxsByAddressWithMemo() {
    Address address = Address.of("EQCQxq9F4-RSaO-ya7q4CF26yyCaQNY98zgD5ys3ZbbiZdUy");

    log.info("address: " + address.toBounceable());

    RawTransactions rawTransactions = tonlib.getAllRawTransactions(address.toRaw(), null, null, 10);

    log.info("total txs: {}", rawTransactions.getTransactions().size());

    for (RawTransaction tx : rawTransactions.getTransactions()) {
      if (nonNull(tx.getIn_msg())
          && (!tx.getIn_msg().getSource().getAccount_address().equals(""))) {

        String msgBodyText;
        if (nonNull(tx.getIn_msg().getMsg_data().getBody())) {

          Cell c =
              CellBuilder.beginCell()
                  .fromBoc(Utils.base64ToSignedBytes(tx.getIn_msg().getMsg_data().getBody()))
                  .endCell();
          msgBodyText = c.print();
        } else {
          msgBodyText = Utils.base64ToString(tx.getIn_msg().getMsg_data().getText());
        }
        log.info(
            "<<<<< {} - {} : {}, msgBody cell/text {}, memo {}, memoBytes {}",
            tx.getIn_msg().getSource().getAccount_address(),
            tx.getIn_msg().getDestination().getAccount_address(),
            Utils.formatNanoValue(tx.getIn_msg().getValue()),
            StringUtils.normalizeSpace(msgBodyText),
            tx.getIn_msg().getMessage(),
            Utils.bytesToHex(tx.getIn_msg().getMessageBytes()));
      }
      if (nonNull(tx.getOut_msgs())) {
        for (RawMessage msg : tx.getOut_msgs()) {
          String msgBodyText;
          if (nonNull(msg.getMsg_data().getBody())) {
            Cell c =
                CellBuilder.beginCell()
                    .fromBoc(Utils.base64ToSignedBytes(msg.getMsg_data().getBody()))
                    .endCell();
            msgBodyText = c.print();
          } else {
            //                        msgBodyText = Utils.base64ToString(msg.getMessage());
            msgBodyText = msg.getMessage();
          }
          log.info(
              ">>>>> {} - {} : {}, msgBody cell/text {}, memo {}, memoHex {}",
              msg.getSource().getAccount_address(),
              msg.getDestination().getAccount_address(),
              Utils.formatNanoValue(msg.getValue()),
              StringUtils.normalizeSpace(msgBodyText),
              msg.getMessage(),
              msg.getMessageHex());
        }
      }
    }

    assertThat(rawTransactions.getTransactions().size()).isLessThan(11);
  }

  @Test
  public void testTonlibGetAllTxsByAddressSmallHistoryLimit() {

    Address address = Address.of(TON_FOUNDATION);

    log.info("address: " + address.toString(true));

    RawTransactions rawTransactions = tonlib.getAllRawTransactions(address.toRaw(), null, null, 3);

    log.info("total txs: {}", rawTransactions.getTransactions().size());

    for (RawTransaction tx : rawTransactions.getTransactions()) {
      if (nonNull(tx.getIn_msg())
          && (StringUtils.isNoneEmpty(tx.getIn_msg().getSource().getAccount_address()))) {
        log.info(
            "<<<<< {} - {} : {} ",
            tx.getIn_msg().getSource().getAccount_address(),
            tx.getIn_msg().getDestination().getAccount_address(),
            Utils.formatNanoValue(tx.getIn_msg().getValue()));
      }
      if (nonNull(tx.getOut_msgs())) {
        for (RawMessage msg : tx.getOut_msgs()) {
          log.info(
              ">>>>> {} - {} : {} ",
              msg.getSource().getAccount_address(),
              msg.getDestination().getAccount_address(),
              Utils.formatNanoValue(msg.getValue()));
        }
      }
    }

    assertThat(rawTransactions.getTransactions().size()).isLessThan(4);
  }

  /** Create new key pair and sign data using Tonlib library */
  @Test
  public void testTonlibNewKey() {
    Key key = tonlib.createNewKey();
    log.info(key.toString());
    String pubKey = Utils.base64UrlSafeToHexString(key.getPublic_key());
    byte[] secKey = Utils.base64ToBytes(key.getSecret());

    log.info(pubKey);
    log.info(Utils.bytesToHex(secKey));

    TweetNaclFast.Signature.KeyPair keyPair = Utils.generateSignatureKeyPairFromSeed(secKey);
    byte[] secKey2 = keyPair.getSecretKey();
    log.info(Utils.bytesToHex(secKey2));
    assertThat(Utils.bytesToHex(secKey2).contains(Utils.bytesToHex(secKey))).isTrue();
  }

  /** Encrypt/Decrypt using key */
  @Test
  public void testTonlibEncryptDecryptKey() {
    String secret = "Q3i3Paa45H/F/Is+RW97lxW0eikF0dPClSME6nbogm0=";
    String dataToEncrypt = Utils.stringToBase64("ABC");
    Data encrypted = tonlib.encrypt(dataToEncrypt, secret);
    log.info("encrypted {}", encrypted.getBytes());

    Data decrypted = tonlib.decrypt(encrypted.getBytes(), secret);
    String dataDecrypted = Utils.base64ToString(decrypted.getBytes());
    log.info("decrypted {}", dataDecrypted);

    assertThat("ABC").isEqualTo(dataDecrypted);
  }

  /** Encrypt/Decrypt with using mnemonic */
  @Test
  public void testTonlibEncryptDecryptMnemonic() {
    String base64mnemonic =
        Utils.stringToBase64(
            "centring moist twopenny bursary could carbarn abide flirt ground shoelace songster isomeric pis strake jittery penguin gab guileful lierne salivary songbird shore verbal measures");
    String dataToEncrypt = Utils.stringToBase64("ABC");
    Data encrypted = tonlib.encrypt(dataToEncrypt, base64mnemonic);
    log.info("encrypted {}", encrypted.getBytes());

    Data decrypted = tonlib.decrypt(encrypted.getBytes(), base64mnemonic);
    String dataDecrypted = Utils.base64ToString(decrypted.getBytes());

    assertThat("ABC").isEqualTo(dataDecrypted);
  }

  @Test
  public void testTonlibEncryptDecryptMnemonicModule()
      throws NoSuchAlgorithmException, InvalidKeyException {
    String base64mnemonic = Utils.stringToBase64(Mnemonic.generateString(24));

    String dataToEncrypt = Utils.stringToBase64("ABC");
    Data encrypted = tonlib.encrypt(dataToEncrypt, base64mnemonic);
    log.info("encrypted {}", encrypted.getBytes());

    Data decrypted = tonlib.decrypt(encrypted.getBytes(), base64mnemonic);
    String dataDecrypted = Utils.base64ToString(decrypted.getBytes());

    assertThat("ABC").isEqualTo(dataDecrypted);
  }

  @Test
  public void testTonlibRawAccountState() {
    Address addr = Address.of("Ef8-sf_0CQDgwW6kNuNY8mUvRW-MGQ34Evffj8O0Z9Ly1tZ4");
    log.info("address: " + addr.toBounceable());

    AccountAddressOnly accountAddressOnly =
        AccountAddressOnly.builder().account_address(addr.toBounceable()).build();

    RawAccountState accountState = tonlib.getRawAccountState(accountAddressOnly);
    log.info(accountState.toString());
    log.info("balance: {}", accountState.getBalance());
    assertThat(accountState.getCode()).isNotBlank();
  }

  @Test
  public void testTonlibAccountState() {
    Tonlib tonlib =
        Tonlib.builder()
            .pathToGlobalConfig("g:/libs/global-config-archive.json")
            .pathToTonlibSharedLib(tonlibPath)
            .receiveTimeout(5)
            .ignoreCache(false)
            .build();

    Address addr = Address.of("Ef8-sf_0CQDgwW6kNuNY8mUvRW-MGQ34Evffj8O0Z9Ly1tZ4");
    log.info("address: " + addr.toBounceable());

    AccountAddressOnly accountAddressOnly =
        AccountAddressOnly.builder().account_address(addr.toBounceable()).build();

    RawAccountState accountState = tonlib.getRawAccountState(accountAddressOnly);
    log.info(accountState.toString());
    log.info("balance: {}", accountState.getBalance());
    assertThat(accountState.getLast_transaction_id().getHash()).isNotBlank();
    log.info("last {}", tonlib.getLast());
  }

  @Test
  public void testTonlibAccountStateAtSeqno() {
    Tonlib tonlib =
        Tonlib.builder()
            .pathToGlobalConfig("g:/libs/global-config-archive.json")
            .pathToTonlibSharedLib(tonlibPath)
            .receiveTimeout(5)
            .ignoreCache(false)
            .build();

    Address addr = Address.of("Ef8-sf_0CQDgwW6kNuNY8mUvRW-MGQ34Evffj8O0Z9Ly1tZ4");
    log.info("address: " + addr.toBounceable());

    BlockIdExt blockId = tonlib.lookupBlock(39047069, -1, -9223372036854775808L, 0, 0);
    RawAccountState accountState = tonlib.getRawAccountState(addr, blockId);
    log.info(accountState.toString());
    log.info("balance: {}", accountState.getBalance());
    assertThat(accountState.getLast_transaction_id().getHash()).isNotBlank();
    log.info("last {}", tonlib.getLast());
  }

  @Test
  public void testTonlibKeystorePath() {
    Tonlib tonlib =
        Tonlib.builder()
            .keystoreInMemory(false)
            .keystorePath("D:/")
            .verbosityLevel(VerbosityLevel.INFO)
            .pathToTonlibSharedLib(tonlibPath)
            .build();
    Address address = Address.of("Ef8-sf_0CQDgwW6kNuNY8mUvRW-MGQ34Evffj8O0Z9Ly1tZ4");
    RunResult result = tonlib.runMethod(address, "seqno");
    log.info("gas_used {}, exit_code {} ", result.getGas_used(), result.getExit_code());
    TvmStackEntryNumber seqno = (TvmStackEntryNumber) result.getStack().get(0);
    log.info("seqno: {}", seqno.getNumber());
    assertThat(result.getExit_code()).isZero();
  }

  @Test
  public void testTonlibRunMethodSeqno() {
    Address address = Address.of(TON_FOUNDATION);
    RunResult result = tonlib.runMethod(address, "seqno");
    log.info("gas_used {}, exit_code {} ", result.getGas_used(), result.getExit_code());
    TvmStackEntryNumber seqno = (TvmStackEntryNumber) result.getStack().get(0);
    log.info("seqno: {}", seqno.getNumber());
    assertThat(result.getExit_code()).isZero();
  }

  @Test
  public void testTonlibRunMethodSeqnoAtBlockId() {
    Tonlib tonlib =
        Tonlib.builder()
            .pathToGlobalConfig("g:/libs/global-config-archive.json")
            .receiveTimeout(5)
            .ignoreCache(false)
            .build();
    Address address = Address.of(TON_FOUNDATION);
    RunResult result = tonlib.runMethod(address, "seqno", 39047069);
    log.info("gas_used {}, exit_code {} ", result.getGas_used(), result.getExit_code());
    TvmStackEntryNumber seqno = (TvmStackEntryNumber) result.getStack().get(0);
    log.info("seqno: {}", seqno.getNumber());
    assertThat(result.getExit_code()).isZero();
  }

  @Test
  public void testTonlibRunMethodGetJetton() {
    Address address = Address.of("EQBYzFXx0QTPW5Lo63ArbNasI_GWRj7NwcAcJR2IWo7_3nTp");
    RunResult result = tonlib.runMethod(address, "get_jetton_data");
    log.info("gas_used {}, exit_code {} ", result.getGas_used(), result.getExit_code());
    log.info("result: {}", result);
    assertThat(result.getExit_code()).isZero();
  }

  @Test
  public void testTonlibRunMethodParticipantsList() {
    Address address =
        Address.of("-1:3333333333333333333333333333333333333333333333333333333333333333");

    RunResult result = tonlib.runMethod(address, "participant_list");
    log.info(result.toString());
    TvmStackEntryList listResult = (TvmStackEntryList) result.getStack().get(0);
    for (Object o : listResult.getList().getElements()) {
      TvmStackEntryTuple t = (TvmStackEntryTuple) o;
      TvmTuple tuple = t.getTuple();
      TvmStackEntryNumber addr = (TvmStackEntryNumber) tuple.getElements().get(0);
      TvmStackEntryNumber stake = (TvmStackEntryNumber) tuple.getElements().get(1);
      log.info("{}, {}", addr.getNumber(), stake.getNumber());
    }
    assertThat(result.getExit_code()).isZero();
  }

  @Test
  public void testTonlibRunMethodParticipantsListInThePast() {
    Tonlib tonlib =
        Tonlib.builder()
            .pathToTonlibSharedLib(tonlibPath)
            .pathToGlobalConfig("g:/libs/global-config-archive.json")
            .receiveTimeout(5)
            .ignoreCache(false)
            .build();
    Address address =
        Address.of("-1:3333333333333333333333333333333333333333333333333333333333333333");

    RunResult result = tonlib.runMethod(address, "participant_list", 39047069);
    log.info(result.toString());
    TvmStackEntryList listResult = (TvmStackEntryList) result.getStack().get(0);
    for (Object o : listResult.getList().getElements()) {
      TvmStackEntryTuple t = (TvmStackEntryTuple) o;
      TvmTuple tuple = t.getTuple();
      TvmStackEntryNumber addr = (TvmStackEntryNumber) tuple.getElements().get(0);
      TvmStackEntryNumber stake = (TvmStackEntryNumber) tuple.getElements().get(1);
      log.info("{}, {}", addr.getNumber(), stake.getNumber());
    }
    assertThat(result.getExit_code()).isZero();
  }

  @Test
  public void testTonlibRunMethodActiveElectionId() {
    Address address =
        Address.of("-1:3333333333333333333333333333333333333333333333333333333333333333");
    RunResult result = tonlib.runMethod(address, "active_election_id");
    TvmStackEntryNumber electionId = (TvmStackEntryNumber) result.getStack().get(0);
    log.info("electionId: {}", electionId.getNumber());
    assertThat(result.getExit_code()).isZero();
  }

  @Test
  public void testTonlibRunMethodActiveElectionIdAtSeqno() {
    Tonlib tonlib =
        Tonlib.builder()
            .pathToTonlibSharedLib(tonlibPath)
            .pathToGlobalConfig("g:/libs/global-config-archive.json")
            .receiveTimeout(5)
            .ignoreCache(false)
            .build();
    Address address =
        Address.of("-1:3333333333333333333333333333333333333333333333333333333333333333");
    RunResult result = tonlib.runMethod(address, "active_election_id", 39047069);
    TvmStackEntryNumber electionId = (TvmStackEntryNumber) result.getStack().get(0);
    log.info("electionId: {}", electionId.getNumber());
    assertThat(result.getExit_code()).isZero();
  }

  @Test
  public void testTonlibRunMethodPastElectionsId() {
    Address address =
        Address.of("-1:3333333333333333333333333333333333333333333333333333333333333333");
    RunResult result = tonlib.runMethod(address, "past_election_ids");
    TvmStackEntryList listResult = (TvmStackEntryList) result.getStack().get(0);
    for (Object o : listResult.getList().getElements()) {
      TvmStackEntryNumber electionId = (TvmStackEntryNumber) o;
      log.info(electionId.getNumber().toString());
    }
    assertThat(result.getExit_code()).isZero();
  }

  @Test
  public void testTonlibRunMethodPastElections() {
    Address address =
        Address.of("-1:3333333333333333333333333333333333333333333333333333333333333333");
    RunResult result = tonlib.runMethod(address, "past_elections");
    TvmStackEntryList listResult = (TvmStackEntryList) result.getStack().get(0);
    log.info("pastElections: {}", listResult);

    assertThat(result.getExit_code()).isZero();
  }

  @Test
  public void testTonlibGetConfig() {
    MasterChainInfo mc = tonlib.getLast();
    Cell c = tonlib.getConfigParam(mc.getLast(), 22);
    log.info(c.print());
  }

  @Test
  public void testTonlibGetConfigAll() {
    Cell c = tonlib.getConfigAll(128);
    log.info(c.print());
  }

  @Test
  public void testTonlibLoadContract() {
    AccountAddressOnly address =
        AccountAddressOnly.builder()
            .account_address("EQAPZ3Trml6zO403fnA6fiqbjPw9JcOCSk0OVY6dVdyM2fEM")
            .build();
    long result = tonlib.loadContract(address);
    log.info("result {}", result);
  }

  @Test
  public void testTonlibLoadContractSeqno() {
    AccountAddressOnly address =
        AccountAddressOnly.builder()
            .account_address("EQAPZ3Trml6zO403fnA6fiqbjPw9JcOCSk0OVY6dVdyM2fEM")
            .build();
    long result = tonlib.loadContract(address, 36661567);
    log.info("result {}", result);
  }

  @Test
  public void testTonlibRunMethodComputeReturnedStake() {
    Address elector = Address.of(ELECTOR_ADDRESSS);
    RunResult result = tonlib.runMethod(elector, "compute_returned_stake", new ArrayDeque<>());
    log.info("result: {}", result);
    assertThat(result.getExit_code())
        .isEqualTo(2); // error since compute_returned_stake requires an argument

    Deque<String> stack = new ArrayDeque<>();
    Address validatorAddress = Address.of("Ef_sR2c8U-tNfCU5klvd60I5VMXUd_U9-22uERrxrrt3uzYi");
    stack.offer("[num," + validatorAddress.toDecimal() + "]");

    result = tonlib.runMethod(elector, "compute_returned_stake", stack);
    BigInteger returnStake = ((TvmStackEntryNumber) result.getStack().get(0)).getNumber();
    log.info("return stake: {} ", Utils.formatNanoValue(returnStake.longValue()));
  }

  @Test
  @Ignore
  public void testTonlibMyLocalTon() {
    Tonlib tonlib =
        Tonlib.builder()
            .verbosityLevel(VerbosityLevel.DEBUG)
            .pathToGlobalConfig(
                "G:/Git_Projects/MyLocalTon/myLocalTon/genesis/db/my-ton-global.config.json")
            .ignoreCache(true)
            .build();

    BlockIdExt blockIdExt = tonlib.getMasterChainInfo().getLast();
    Cell cellConfig8 = tonlib.getConfigParam(blockIdExt, 8);
    ConfigParams8 config8 = ConfigParams8.deserialize(CellSlice.beginParse(cellConfig8));
    log.info("config 8: {}", config8);
    RunResult seqno =
        tonlib.runMethod(
            Address.of("-1:CF624357217E2C9D2F4F5CA65F82FCBD16949FA00F46CA51358607BEF6D2CB53"),
            "seqno");
    log.info("seqno RunResult {}", seqno);
    FullAccountState accountState1 =
        tonlib.getAccountState(
            Address.of("-1:85cda44e9838bf5a8c6d1de95c3e22b92884ae70ee1b550723a92a8ca0df3321"));
    RawAccountState accountState2 =
        tonlib.getRawAccountState(
            Address.of("-1:85cda44e9838bf5a8c6d1de95c3e22b92884ae70ee1b550723a92a8ca0df3321"));

    log.info("full accountState {}", accountState1);
    log.info("raw  accountState {}", accountState2);
  }

  @Test
  public void testTonlibLookupBlock() {
    try {
      Tonlib tonlib =
          Tonlib.builder()
              .pathToTonlibSharedLib(tonlibPath)
              .receiveTimeout(5)
              .ignoreCache(false)
              .liteServerIndex(0)
              .verbosityLevel(VerbosityLevel.DEBUG)
              .testnet(true)
              .build();
      MasterChainInfo mcInfo = tonlib.getLast();

      Shards shards = tonlib.getShards(mcInfo.getLast().getSeqno(), 0, 0);
      log.info("shards-- {}", shards.getShards());

      BlockIdExt shard = shards.getShards().get(0);

      BlockIdExt fullblock =
          tonlib.lookupBlock(shard.getSeqno(), shard.getWorkchain(), shard.getShard(), 0, 0);
      log.info("fullBlock-- {}", fullblock);
      assertThat(fullblock).isNotNull();
    } catch (Throwable e) {
      e.printStackTrace();
    }
  }

  @Test
  public void testTonlibTryLocateTxByIncomingMessage() {
    RawTransaction tx =
        tonlib.tryLocateTxByIncomingMessage(
            Address.of("EQAuMjwyuQBaaxM6ooRJWbuUacQvBgVEWQOSSlbMERG0ljRD"),
            Address.of("EQDEruSI2frAF-GdzpjDLWWBKnwREDAJmu7eIEFG6zdUlXVE"),
            26521292000002L);

    log.info("found tx {}", tx);

    assertThat(tx.getIn_msg()).isNotNull();
  }

  @Test
  public void testTonlibTryLocateTxByOutgoingMessage() {
    RawTransaction tx =
        tonlib.tryLocateTxByOutcomingMessage(
            Address.of("EQAuMjwyuQBaaxM6ooRJWbuUacQvBgVEWQOSSlbMERG0ljRD"),
            Address.of("EQDEruSI2frAF-GdzpjDLWWBKnwREDAJmu7eIEFG6zdUlXVE"),
            26521292000002L);

    log.info("found tx {}", tx);

    assertThat(tx.getIn_msg()).isNotNull();
    assertThat(tx.getOut_msgs()).isNotNull();
  }

  @Test
  public void testTonlibStateAndStatus() {
    FullAccountState accountState1 =
        tonlib.getAccountState(Address.of("EQCtPHFrtkIw3UC2rNfSgVWYT1MiMLDUtgMy2M7j1P_eNMDq"));
    log.info("FullAccountState {}", accountState1);

    RawAccountState accountState2 =
        tonlib.getRawAccountState(Address.of("EQCtPHFrtkIw3UC2rNfSgVWYT1MiMLDUtgMy2M7j1P_eNMDq"));
    log.info("RawAccountState {}", accountState2);

    BlockIdExt blockId =
        BlockIdExt.builder()
            .workchain(-1)
            .shard(-9223372036854775808L)
            .seqno(27894542)
            .root_hash("akLZC86Ve0IPDW2HGhSCKKz7RkJk1yAgl34qMpo1RlE=")
            .file_hash("KjJvOENNPc39inKO2cOce0s/fUX9Nv8/qdS1VFj2yw0=")
            .build();

    log.info("input blockId {}", blockId);

    FullAccountState accountState1AtBlock =
        tonlib.getAccountState(
            Address.of("EQCtPHFrtkIw3UC2rNfSgVWYT1MiMLDUtgMy2M7j1P_eNMDq"), blockId);
    log.info("accountState1AtBlock {}", accountState1AtBlock);

    RawAccountState accountState2AtBlock =
        tonlib.getRawAccountState(
            Address.of("EQCtPHFrtkIw3UC2rNfSgVWYT1MiMLDUtgMy2M7j1P_eNMDq"), blockId);
    log.info("RawAccountStateAtBlock {}", accountState2AtBlock);

    String accountState1Status =
        tonlib.getRawAccountStatus(Address.of("EQCtPHFrtkIw3UC2rNfSgVWYT1MiMLDUtgMy2M7j1P_eNMDq"));
    log.info("==========================================");

    log.info("rawAccountState2 {}", accountState2);
  }

  @Test
  public void testTonlibGetLibraries() {
    SmcLibraryResult result =
        tonlib.getLibraries(
            Collections.singletonList("wkUmK4wrzl6fzSPKM04dVfqW1M5pqigX3tcXzvy6P3M="));
    log.info("result: {}", result);

    assertThat(result.getResult().get(0).getHash())
        .isEqualTo("wkUmK4wrzl6fzSPKM04dVfqW1M5pqigX3tcXzvy6P3M=");
  }

  @Test
  public void testTonlibGetLibrariesExt() {
    BigInteger arg1 =
        new BigInteger("6aaf20fed58ba5e6db692909e78e5c5c6525e28d1cfa8bd22dc216729b4841b3", 16);

    SmcLibraryQueryExt smcLibraryQueryExt =
        SmcLibraryQueryExt.builder()
            .boc(
                "b5ee9c72410101010024000043801482bf04d1769cf0b59f5ffd4cbf659b5e1d9ddd2eccc47901ec29242b3fd76fb07111e8d8")
            .max_libs(10)
            .hash(arg1.longValue())
            .build();
    List<SmcLibraryQueryExt> list = new ArrayList<>();
    list.add(smcLibraryQueryExt);

    SmcLibraryResult result = tonlib.getLibrariesExt(list);
    log.info("result: {}", result);

    assertThat(result.getResult().get(0).getHash())
        .isEqualTo("wkUmK4wrzl6fzSPKM04dVfqW1M5pqigX3tcXzvy6P3M=");
  }

  @Test
  public void testTonlibGetConfigs() {
    log.info("config0 {}", tonlib.getConfigParam0());
    log.info("config1 {}", tonlib.getConfigParam1());
    log.info("config2 {}", tonlib.getConfigParam2());
    //    log.info("config3 {}", tonlib.getConfigParam3());
    log.info("config4 {}", tonlib.getConfigParam4());
    log.info("config5 {}", tonlib.getConfigParam5());
    //    log.info("config6 {}", tonlib.getConfigParam6());
  }

  @Test
  public void testTonlibRunMethodGetSrcAddr() {
    Address smc = Address.of("EQD-BJSVUJviud_Qv7Ymfd3qzXdrmV525e3YDzWQoHIAiInL");

    Deque<String> stack = new ArrayDeque<>();
    BigInteger arg1 =
        new BigInteger("6aaf20fed58ba5e6db692909e78e5c5c6525e28d1cfa8bd22dc216729b4841b3", 16);
    BigInteger arg2 =
        new BigInteger("e4cf3b2f4c6d6a61ea0f2b5447d266785b26af3637db2deee6bcd1aa826f3412", 16);
    stack.offer("[num," + arg1 + "]");
    stack.offer("[num," + arg2 + "]");

    RunResult result = tonlib.runMethod(smc, "get_source_item_address", stack);
    log.info("result {}", result);
    TvmStackEntrySlice slice = ((TvmStackEntrySlice) result.getStack().get(0));
    String b64 = Utils.base64ToHexString(slice.getSlice().getBytes());
    log.info("b64 {}", b64);

    // @formatter:off
    // tonlib
    //         4          4    desc    slice
    //            crc
    // b5ee9c72410101010024 00 ----
    // 0043801482bf04d1769cf0b59f5ffd4cbf659b5e1d9ddd2eccc47901ec29242b3fd76fb0 7111e8d8
    Cell c = CellBuilder.beginCell().fromBoc(b64).endCell();
    log.info("c {}", c.toHex(true));

    // b5ee9c72010101010024 00 00
    // 43801f0111d15a69c7d42a36433173c773ce24bf905542d3d38dd073462b4a5b136c30 subbotin-1
    // b5ee9c72010101010024 00 00
    // 43800f2034101a96276a66c408417b1d38f5fcb3d6187226930600e8273b43c92a6970 subbotin-2

    // slice returned by lite-client
    // "0043801482bf04d1769cf0b59f5ffd4cbf659b5e1d9ddd2eccc47901ec29242b3fd76fb0"

    // ton4j to boc       6    6
    // b5ee9c72410101010024 00
    // 0043801482bf04d1769cf0b59f5ffd4cbf659b5e1d9ddd2eccc47901ec29242b3fd76fb0 7111e8d8 tonlib
    //
    //         4
    // b5ee9c72410101010024 00 0048
    // 0043801482bf04d1769cf0b59f5ffd4cbf659b5e1d9ddd2eccc47901ec29242b3fd76fb0 cad13166 with crc +
    // store all bytes
    // b5ee9c72410101010026 00 0048
    // 0043801482bf04d1769cf0b59f5ffd4cbf659b5e1d9ddd2eccc47901ec29242b3fd76fb0 cad13166 crc + store
    // 267 bits
    // b5ee9c72c10101010026 00 2600
    // 480043801482bf04d1769cf0b59f5ffd4cbf659b5e1d9ddd2eccc47901ec29242b3fd76fb0 fe87f4b3 crc+index
    // b5ee9c72010101010026 00 6048
    // 0043801482bf04d1769cf0b59f5ffd4cbf659b5e1d9ddd2eccc47901ec29242b3fd76fb0 pruned
    // b5ee9c72010101010026 00 0048
    // 0043801482bf04d1769cf0b59f5ffd4cbf659b5e1d9ddd2eccc47901ec29242b3fd76fb0 ordinary
    // b5ee9c72010101010026 00 0048
    // 0043801482bf04d1769cf0b59f5ffd4cbf659b5e1d9ddd2eccc47901ec29242b3fd76fb0 library
    // go lang
    // b5ee9c72410101010024 00 0043
    // 0043801482bf04d1769cf0b59f5ffd4cbf659b5e1d9ddd2eccc47901ec29242b3    fd0 d197c327 store 267
    // bits
    // b5ee9c72410101010026 00 0048
    // 0043801482bf04d1769cf0b59f5ffd4cbf659b5e1d9ddd2eccc47901ec29242b3fd76fb0 cad13166
    // pytoniq-core
    //
    // slice returned by lite-client and trimmed front
    // "801482bf04d1769cf0b59f5ffd4cbf659b5e1d9ddd2eccc47901ec29242b3fd76fb0"
    //         4          4
    // b5ee9c72410101010024 00 0044
    // 801482bf04d1769cf0b59f5ffd4cbf659b5e1d9ddd2eccc47901ec29242b3fd76fb0 98f3b93d

    // @formatter:on
  }
}
