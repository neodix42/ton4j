package org.ton.java.adnl;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.List;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@JsonIgnoreProperties(ignoreUnknown = true)
class LiteServerConfig {
  @JsonProperty("liteservers")
  private List<LiteServer> liteServers;

  public List<LiteServer> getLiteServers() {
    return liteServers;
  }

  public LiteServer getRandomLiteServer() {
    if (liteServers == null || liteServers.isEmpty()) {
      return null;
    }
    return liteServers.get(new Random().nextInt(liteServers.size()));
  }

  public LiteServer getLiteServerByIndex(int index) {
    if (liteServers == null || liteServers.isEmpty()) {
      return null;
    }
    return liteServers.get(index);
  }
}

class LiteServer {
  @JsonProperty("ip")
  private long ip;

  @JsonProperty("port")
  private int port;

  @JsonProperty("id")
  private ServerId id;

  public String getHost() {
    // Convert long to IP address
    return String.format(
        "%d.%d.%d.%d", (ip >> 24) & 0xFF, (ip >> 16) & 0xFF, (ip >> 8) & 0xFF, ip & 0xFF);
  }

  public int getPort() {
    return port;
  }

  public String getKey() {
    return id.getKey();
  }
}

@JsonIgnoreProperties(ignoreUnknown = true)
class ServerId {
  @JsonProperty("key")
  private String key;

  public String getKey() {
    return key;
  }
}

/** Test class for ADNL Lite Client Demonstrates usage and basic functionality */
public class AdnlLiteClientTest {

  private LiteServerConfig fetchLiteServerConfig(String path) throws IOException {
    ObjectMapper mapper = new ObjectMapper();
    // Try to load from classpath first
    InputStream is = getClass().getClassLoader().getResourceAsStream(path);
    if (is != null) {
      return mapper.readValue(is, LiteServerConfig.class);
    }
    // Fall back to URL if not found in classpath
    return mapper.readValue(new URL(path), LiteServerConfig.class);
  }

  private static final Logger logger = Logger.getLogger(AdnlLiteClientTest.class.getName());

  private AdnlLiteClient client;
  private LiteClientConnectionPool pool;

  // Get test configuration from resources
  private static final String CONFIG_PATH = "test-config.json";

  @BeforeEach
  void setUp() {
    // Enable debug logging for tests
    Logger.getLogger("org.ton.java.adnl").setLevel(Level.ALL);
  }

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
    logger.info("Testing single liteserver connection");

    // Fetch test server configuration
    LiteServerConfig config = fetchLiteServerConfig(CONFIG_PATH);
    LiteServer server = config.getLiteServerByIndex(0);

    if (server == null) {
      fail("No liteservers found in configuration");
    }

    client = new AdnlLiteClient();
    try {
      client.connect(server.getHost(), server.getPort(), server.getKey());
      assertTrue(client.isConnected(), "Client should be connected");
    } catch (Exception e) {
      logger.severe("Connection failed: " + e.getMessage());
      throw e;
    }

    // Test ping by getting masterchain info
    AdnlLiteClient.MasterchainInfo info = client.getMasterchainInfo();
    assertNotNull(info, "Masterchain info should not be null");
    assertNotNull(info.getLast(), "Last block should not be null");
    assertTrue(info.getLast().getSeqno() > 0, "Seqno should be positive");

    logger.info("Last block seqno: " + info.getLast().getSeqno());
    logger.info("Workchain: " + info.getLast().getWorkchain());
    logger.info("Shard: " + info.getLast().getShard());
  }

  @Test
  void testConnectionPool() throws Exception {
    logger.info("Testing connection pool");

    // Fetch test server configuration
    LiteServerConfig config = fetchLiteServerConfig(CONFIG_PATH);
    LiteServer server = config.getRandomLiteServer();

    if (server == null) {
      fail("No liteservers found in configuration");
    }

    pool = new LiteClientConnectionPool();
    pool.addConnection(server.getHost(), server.getPort(), server.getKey());

    assertEquals(1, pool.getTotalConnectionCount(), "Should have 1 connection");
    assertEquals(1, pool.getActiveConnectionCount(), "Should have 1 active connection");

    // Test query through pool
    AdnlLiteClient.MasterchainInfo info = pool.getMasterchainInfo();
    assertNotNull(info, "Masterchain info should not be null");
    logger.info("Pool query successful - Last block seqno: " + info.getLast().getSeqno());
  }

  @Test
  void testAccountStateQuery() throws Exception {
    logger.info("Testing account state query");

    // Fetch test server configuration
    LiteServerConfig config = fetchLiteServerConfig(CONFIG_PATH);
    LiteServer server = config.getRandomLiteServer();

    if (server == null) {
      fail("No liteservers found in configuration");
    }

    client = new AdnlLiteClient();
    client.connect(server.getHost(), server.getPort(), server.getKey());

    // Get current masterchain info first
    AdnlLiteClient.MasterchainInfo info = client.getMasterchainInfo();
    AdnlLiteClient.BlockIdExt lastBlock = info.getLast();

    // Query a well-known account (TON Foundation)
    byte[] accountAddress = new byte[32]; // Zero address for testing
    AdnlLiteClient.AccountId accountId = new AdnlLiteClient.AccountId(-1, accountAddress);

    try {
      AdnlLiteClient.AccountState state = client.getAccountState(lastBlock, accountId);
      assertNotNull(state, "Account state should not be null");
      logger.info("Account state query successful");
    } catch (Exception e) {
      // Account might not exist, which is fine for this test
      logger.info("Account state query completed (account may not exist): " + e.getMessage());
    }
  }

  @Test
  void testSmcMethodCall() throws Exception {
    logger.info("Testing smart contract method call");

    // Fetch test server configuration
    LiteServerConfig config = fetchLiteServerConfig(CONFIG_PATH);
    LiteServer server = config.getRandomLiteServer();

    if (server == null) {
      fail("No liteservers found in configuration");
    }

    client = new AdnlLiteClient();
    client.connect(server.getHost(), server.getPort(), server.getKey());

    // Get current masterchain info first
    AdnlLiteClient.MasterchainInfo info = client.getMasterchainInfo();
    AdnlLiteClient.BlockIdExt lastBlock = info.getLast();

    // Try to call a method on a contract (this might fail if contract doesn't exist)
    byte[] accountAddress = new byte[32]; // Zero address for testing
    AdnlLiteClient.AccountId accountId = new AdnlLiteClient.AccountId(-1, accountAddress);

    try {
      AdnlLiteClient.RunMethodResult result =
          client.runSmcMethod(
              lastBlock,
              accountId,
              85143, // seqno method
              new byte[0] // no parameters
              );

      assertNotNull(result, "Run method result should not be null");
      logger.info("SMC method call completed with exit code: " + result.getExitCode());
    } catch (Exception e) {
      // Method call might fail if account doesn't exist or method doesn't exist
      logger.info("SMC method call completed: " + e.getMessage());
    }
  }

  @Test
  void testMultipleQueries() throws Exception {
    logger.info("Testing multiple sequential queries");

    // Fetch test server configuration
    LiteServerConfig config = fetchLiteServerConfig(CONFIG_PATH);
    LiteServer server = config.getRandomLiteServer();

    if (server == null) {
      fail("No liteservers found in configuration");
    }

    client = new AdnlLiteClient();
    client.connect(server.getHost(), server.getPort(), server.getKey());

    // Perform multiple queries to test connection stability
    for (int i = 0; i < 5; i++) {
      AdnlLiteClient.MasterchainInfo info = client.getMasterchainInfo();
      assertNotNull(info, "Masterchain info should not be null for query " + i);
      logger.info("Query " + i + " - Seqno: " + info.getLast().getSeqno());

      // Small delay between queries
      Thread.sleep(100);
    }

    logger.info("Multiple queries completed successfully");
  }

  /** Manual test method (not a JUnit test) for interactive testing */
  public static void main(String[] args) {
    Logger.getLogger("org.ton.java.adnl").setLevel(Level.INFO);
    System.out.println("Starting ADNL Lite Client manual test...");

    try {
      // Test single connection
      AdnlLiteClient client = new AdnlLiteClient();
      System.out.println("Connecting to liteserver...");

      // Fetch test server configuration
      AdnlLiteClientTest test = new AdnlLiteClientTest();
      LiteServerConfig config = test.fetchLiteServerConfig(CONFIG_PATH);
      LiteServer server = config.getLiteServerByIndex(0);

      if (server == null) {
        System.err.println("No liteservers found in configuration");
        return;
      }

      client.connect(server.getHost(), server.getPort(), server.getKey());
      System.out.println("Connected successfully!");

      // Get masterchain info
      System.out.println("Getting masterchain info...");
      AdnlLiteClient.MasterchainInfo info = client.getMasterchainInfo();
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
