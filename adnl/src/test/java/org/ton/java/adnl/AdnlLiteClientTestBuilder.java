package org.ton.java.adnl;

import static org.junit.jupiter.api.Assertions.*;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.ton.java.adnl.globalconfig.TonGlobalConfig;
import org.ton.ton4j.tl.liteserver.responses.*;

@Slf4j
public class AdnlLiteClientTestBuilder {

  public static final String TESTNET_ADDRESS = "0QAyni3YDAhs7c-7imWvPyEbMEeVPMX8eWDLQ5GUe-B-Bl9Z";
  public static final String MAINNET_ADDRESS = "EQCRGnccIFznQqxm_oBm8PHz95iOe89Oe6hRAhSlAaMctuo6";
  public static final String ELECTOR_ADDRESS =
      "-1:3333333333333333333333333333333333333333333333333333333333333333";

  private static AdnlLiteClient client;
  private LiteClientConnectionPool pool;
  private static final String TESTNET_CONFIG_PATH = "testnet-global.config.json";
  private static final String MAINNET_CONFIG_PATH = "global.config.json";
  private static final boolean mainnet = true;

  @Test
  void testBuilderWithUrl() throws Exception {
    AdnlLiteClient client =
        AdnlLiteClient.builder()
            .configUrl("https://ton.org/global-config.json")
            .maxRetries(3)
            .build();

    assertTrue(client.isConnected(), "Client should be connected");

    MasterchainInfo info = client.getMasterchainInfo();
    assertNotNull(info.getLast(), "Last block should not be null");
    assertTrue(info.getLast().getSeqno() > 0, "Seqno should be positive");
  }

  @Test
  void testBuilderWithPath() throws Exception {
    AdnlLiteClient client =
        AdnlLiteClient.builder()
            .configPath("/home/neodix/gitProjects/ton4j/adnl/testnet-global.config.json")
            .maxRetries(3)
            .build();

    assertTrue(client.isConnected(), "Client should be connected");

    MasterchainInfo info = client.getMasterchainInfo();
    assertNotNull(info.getLast(), "Last block should not be null");
    assertTrue(info.getLast().getSeqno() > 0, "Seqno should be positive");
  }

  @Test
  void testBuilderWithObject() throws Exception {
    TonGlobalConfig tonGlobalConfig =
        TonGlobalConfig.loadFromPath(
            "/home/neodix/gitProjects/ton4j/adnl/testnet-global.config.json");
    AdnlLiteClient client =
        AdnlLiteClient.builder()
            .globalConfig(tonGlobalConfig)
            .liteServerIndex(1)
            .maxRetries(3)
            .build();

    assertTrue(client.isConnected(), "Client should be connected");

    MasterchainInfo info = client.getMasterchainInfo();
    assertNotNull(info.getLast(), "Last block should not be null");
    assertTrue(info.getLast().getSeqno() > 0, "Seqno should be positive");
  }

  @Test
  void testBuilderWithIndex() throws Exception {
    AdnlLiteClient client =
        AdnlLiteClient.builder()
            .configPath("/home/neodix/gitProjects/ton4j/adnl/testnet-global.config.json")
            .liteServerIndex(1)
            .maxRetries(3)
            .build();

    assertTrue(client.isConnected(), "Client should be connected");

    MasterchainInfo info = client.getMasterchainInfo();
    assertNotNull(info.getLast(), "Last block should not be null");
    assertTrue(info.getLast().getSeqno() > 0, "Seqno should be positive");
  }
}
