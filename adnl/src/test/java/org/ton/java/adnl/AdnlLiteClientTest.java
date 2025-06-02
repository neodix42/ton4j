package org.ton.java.adnl;

import static org.junit.jupiter.api.Assertions.*;

import java.util.logging.Level;
import java.util.logging.Logger;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.ton.java.adnl.globalconfig.TonGlobalConfig;
import org.ton.ton4j.tl.types.CurrentTime;
import org.ton.ton4j.tl.types.MasterchainInfo;
import org.ton.ton4j.tl.types.Version;
import org.ton.ton4j.utils.Utils;

// global config
// slf4j

/** Test class for ADNL Lite Client Demonstrates usage and basic functionality */
@Slf4j
public class AdnlLiteClientTest {

  private AdnlLiteClient client;
  private LiteClientConnectionPool pool;

  // Get test configuration from resources
  private static final String CONFIG_PATH = "test-config.json";

  @AfterEach
  void tearDown() {
    if (client != null) {
      client.close();
    }
    if (pool != null) {
      pool.close();
    }
  }

  @Test
  void testSingleConnection() throws Exception {
    log.info("Testing single lite-server connection");

    // TonGlobalConfig tonGlobalConfig = TonGlobalConfig.loadFromPath(CONFIG_PATH);
    TonGlobalConfig tonGlobalConfig =
        TonGlobalConfig.loadFromUrl(Utils.getGlobalConfigUrlTestnetGithub());

    if (tonGlobalConfig.getLiteservers().length == 0) {
      fail("No lite-servers found in configuration");
    }

    client = new AdnlLiteClient();
    try {
      client.connect(
          Utils.int2ip(tonGlobalConfig.getLiteservers()[0].getIp()),
          (int) tonGlobalConfig.getLiteservers()[0].getPort(),
          tonGlobalConfig.getLiteservers()[0].getId().getKey());
      assertTrue(client.isConnected(), "Client should be connected");
    } catch (Exception e) {
      log.error("Connection failed: {}", e.getMessage());
      throw e;
    }

    // Test ping by getting masterchain info
    Utils.sleep(6);
    MasterchainInfo info = client.getMasterchainInfo();
    assertNotNull(info, "Masterchain info should not be null");
    assertNotNull(info.getLast(), "Last block should not be null");

    log.info("Last block seqno: {} ", info.getLast().getSeqno());
    log.info("Workchain: {}", info.getLast().getWorkchain());
    log.info("Shard: {}", info.getLast().getShard());
    log.info("init.wc: {}", info.getInit().getWorkchain());

    assertTrue(info.getLast().getSeqno() > 0, "Seqno should be positive");
  }

  @Test
  void testConnectionPool() throws Exception {
    log.info("Testing connection pool");

    TonGlobalConfig tonGlobalConfig = TonGlobalConfig.loadFromPath(CONFIG_PATH);

    if (tonGlobalConfig.getLiteservers().length == 0) {
      fail("No lite-servers found in configuration");
    }

    pool = new LiteClientConnectionPool();
    pool.addConnection(
        Utils.int2ip(tonGlobalConfig.getLiteservers()[0].getIp()),
        (int) tonGlobalConfig.getLiteservers()[0].getPort(),
        tonGlobalConfig.getLiteservers()[0].getId().getKey());

    assertEquals(1, pool.getTotalConnectionCount(), "Should have 1 connection");
    assertEquals(1, pool.getActiveConnectionCount(), "Should have 1 active connection");

    // Test query through pool
    //    MasterchainInfo info = pool.getMasterchainInfo();
    //    assertNotNull(info, "Masterchain info should not be null");
    //    log.info("Pool query successful - Last block seqno: " + info.getLast().getSeqno());
  }

  @Test
  void testGetTime() throws Exception {
    log.info("Testing getTime query");

    TonGlobalConfig tonGlobalConfig = TonGlobalConfig.loadFromUrl(Utils.getGlobalConfigUrlTestnetGithub());

    if (tonGlobalConfig.getLiteservers().length == 0) {
      fail("No lite-servers found in configuration");
    }

    client = new AdnlLiteClient();
    client.connect(
        Utils.int2ip(tonGlobalConfig.getLiteservers()[0].getIp()),
        (int) tonGlobalConfig.getLiteservers()[0].getPort(),
        tonGlobalConfig.getLiteservers()[0].getId().getKey());
    assertTrue(client.isConnected(), "Client should be connected");

    CurrentTime time = client.getTime();
    assertNotNull(time, "CurrentTime should not be null");
    assertTrue(time.getNow() > 0, "Now timestamp should be positive");
    
    log.info("Current time: {}", time.getNow());
  }

  @Test
  void testGetVersion() throws Exception {
    log.info("Testing getVersion query");

    TonGlobalConfig tonGlobalConfig = TonGlobalConfig.loadFromUrl(Utils.getGlobalConfigUrlTestnetGithub());

    if (tonGlobalConfig.getLiteservers().length == 0) {
      fail("No lite-servers found in configuration");
    }

    client = new AdnlLiteClient();
    client.connect(
        Utils.int2ip(tonGlobalConfig.getLiteservers()[0].getIp()),
        (int) tonGlobalConfig.getLiteservers()[0].getPort(),
        tonGlobalConfig.getLiteservers()[0].getId().getKey());
    assertTrue(client.isConnected(), "Client should be connected");

    Version version = client.getVersion();
    assertNotNull(version, "Version should not be null");
    assertTrue(version.getVersion() > 0, "Version number should be positive");
    assertTrue(version.getNow() > 0, "Now timestamp should be positive");
    
    log.info("Lite server version: {}", version.getVersion());
    log.info("Mode: {}", version.getMode());
    log.info("Capabilities: {}", version.getCapabilities());
    log.info("Now: {}", version.getNow());
  }

  //  @Test
  //  void testAccountStateQuery() throws Exception {
  //    log.info("Testing account state query");
  //
  //    // Fetch test server configuration
  //    LiteServerConfig config = fetchLiteServerConfig(CONFIG_PATH);
  //    LiteServer server = config.getRandomLiteServer();
  //
  //    if (server == null) {
  //      fail("No liteservers found in configuration");
  //    }
  //
  //    client = new AdnlLiteClient();
  //    client.connect(server.getHost(), server.getPort(), server.getKey());
  //
  //    // Get current masterchain info first
  //    MasterchainInfo info = client.getMasterchainInfo();
  //    BlockIdExt lastBlock = info.getLast();
  //
  //    // Query a well-known account (TON Foundation)
  //    byte[] accountAddress = new byte[32]; // Zero address for testing
  //    AccountId accountId = new AccountId(-1, accountAddress);
  //
  //    try {
  //      AccountState state = client.getAccountState(lastBlock, accountId);
  //      assertNotNull(state, "Account state should not be null");
  //      log.info("Account state query successful");
  //    } catch (Exception e) {
  //      // Account might not exist, which is fine for this test
  //      log.info("Account state query completed (account may not exist): " + e.getMessage());
  //    }
  //  }

  //  @Test
  //  void testSmcMethodCall() throws Exception {
  //    log.info("Testing smart contract method call");
  //
  //    // Fetch test server configuration
  //    LiteServerConfig config = fetchLiteServerConfig(CONFIG_PATH);
  //    LiteServer server = config.getRandomLiteServer();
  //
  //    if (server == null) {
  //      fail("No liteservers found in configuration");
  //    }
  //
  //    client = new AdnlLiteClient();
  //    client.connect(server.getHost(), server.getPort(), server.getKey());
  //
  //    // Get current masterchain info first
  //    MasterchainInfo info = client.getMasterchainInfo();
  //    BlockIdExt lastBlock = info.getLast();
  //
  //    // Try to call a method on a contract (this might fail if contract doesn't exist)
  //    byte[] accountAddress = new byte[32]; // Zero address for testing
  //    AccountId accountId = new AccountId(-1, accountAddress);
  //
  //    try {
  //      RunMethodResult result =
  //          client.runSmcMethod(
  //              lastBlock,
  //              accountId,
  //              85143, // seqno method
  //              new byte[0] // no parameters
  //              );
  //
  //      assertNotNull(result, "Run method result should not be null");
  //      log.info("SMC method call completed with exit code: " + result.getExitCode());
  //    } catch (Exception e) {
  //      // Method call might fail if account doesn't exist or method doesn't exist
  //      log.info("SMC method call completed: " + e.getMessage());
  //    }
  //  }
  //
  //  @Test
  //  void testMultipleQueries() throws Exception {
  //    log.info("Testing multiple sequential queries");
  //
  //    // Fetch test server configuration
  //    LiteServerConfig config = fetchLiteServerConfig(CONFIG_PATH);
  //    LiteServer server = config.getRandomLiteServer();
  //
  //    if (server == null) {
  //      fail("No liteservers found in configuration");
  //    }
  //
  //    client = new AdnlLiteClient();
  //    client.connect(server.getHost(), server.getPort(), server.getKey());
  //
  //    // Perform multiple queries to test connection stability
  //    for (int i = 0; i < 5; i++) {
  //      AdnlLiteClient.MasterchainInfo info = client.getMasterchainInfo();
  //      assertNotNull(info, "Masterchain info should not be null for query " + i);
  //      log.info("Query " + i + " - Seqno: " + info.getLast().getSeqno());
  //
  //      // Small delay between queries
  //      Thread.sleep(100);
  //    }
  //
  //    log.info("Multiple queries completed successfully");
  //  }

  /** Manual test method (not a JUnit test) for interactive testing */
  public static void main(String[] args) {
    Logger.getLogger("org.ton.java.adnl").setLevel(Level.INFO);
    System.out.println("Starting ADNL Lite Client manual test...");

    try {
      // Test single connection
      AdnlLiteClient client = new AdnlLiteClient();
      System.out.println("Connecting to lite-server...");

      // Fetch test server configuration
      AdnlLiteClientTest test = new AdnlLiteClientTest();
      TonGlobalConfig tonGlobalConfig = TonGlobalConfig.loadFromPath(CONFIG_PATH);

      if (tonGlobalConfig.getLiteservers().length == 0) {
        fail("No lite-servers found in configuration");
      }

      client.connect(
          Utils.int2ip(tonGlobalConfig.getLiteservers()[0].getIp()),
          (int) tonGlobalConfig.getLiteservers()[0].getPort(),
          tonGlobalConfig.getLiteservers()[0].getId().getKey());
      System.out.println("Connected successfully!");

      // Get masterchain info
      System.out.println("Getting masterchain info...");
      MasterchainInfo info = client.getMasterchainInfo();
      System.out.println("Last block seqno: " + info.getLast().getSeqno());
      System.out.println("Workchain: " + info.getLast().getWorkchain());
      System.out.println("Shard: " + info.getLast().getShard());

      System.out.println("\nTest completed successfully!");
      client.close();
    } catch (Exception e) {
      System.err.println("Test failed: " + e.getMessage());
      e.printStackTrace();
    }
  }
}
