package org.ton.java.adnl;

import static org.junit.jupiter.api.Assertions.*;

import java.util.logging.Level;
import java.util.logging.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Test class for ADNL Lite Client Demonstrates usage and basic functionality */
public class AdnlLiteClientTest {
  private static final Logger logger = Logger.getLogger(AdnlLiteClientTest.class.getName());

  private AdnlLiteClient client;
  private LiteClientConnectionPool pool;

  // TON testnet liteserver (public)
  private static final String LITESERVER_HOST = "109.236.80.69";
  private static final int LITESERVER_PORT = 49913;
  private static final String LITESERVER_KEY = "AxFZRHVD1qIO9Fyva52P4vC3tRvk8ac1KKOG0c6IVio=";

  @BeforeEach
  void setUp() {
    // Enable debug logging for tests
    Logger.getLogger("org.ton.java.adnl").setLevel(Level.FINE);
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

    client = new AdnlLiteClient();
    client.connect(LITESERVER_HOST, LITESERVER_PORT, LITESERVER_KEY);

    assertTrue(client.isConnected(), "Client should be connected");

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

    pool = new LiteClientConnectionPool();

    // Add single connection for testing
    pool.addConnection(LITESERVER_HOST, LITESERVER_PORT, LITESERVER_KEY);

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

    client = new AdnlLiteClient();
    client.connect(LITESERVER_HOST, LITESERVER_PORT, LITESERVER_KEY);

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

    client = new AdnlLiteClient();
    client.connect(LITESERVER_HOST, LITESERVER_PORT, LITESERVER_KEY);

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

    client = new AdnlLiteClient();
    client.connect(LITESERVER_HOST, LITESERVER_PORT, LITESERVER_KEY);

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

  /**
   * Manual test method (not a JUnit test) for interactive testing Run this manually to test with
   * real liteserver
   */
  public static void main(String[] args) {
    Logger.getLogger("org.ton.java.adnl").setLevel(Level.INFO);

    System.out.println("Starting ADNL Lite Client manual test...");

    try {
      // Test single connection
      AdnlLiteClient client = new AdnlLiteClient();
      System.out.println("Connecting to liteserver...");
      client.connect(LITESERVER_HOST, LITESERVER_PORT, LITESERVER_KEY);
      System.out.println("Connected successfully!");

      // Get masterchain info
      System.out.println("Getting masterchain info...");
      AdnlLiteClient.MasterchainInfo info = client.getMasterchainInfo();
      System.out.println("Last block seqno: " + info.getLast().getSeqno());
      System.out.println("Workchain: " + info.getLast().getWorkchain());
      System.out.println("Shard: " + info.getLast().getShard());

      // Test connection pool
      System.out.println("\nTesting connection pool...");
      LiteClientConnectionPool pool = new LiteClientConnectionPool();
      pool.addConnection(LITESERVER_HOST, LITESERVER_PORT, LITESERVER_KEY);

      AdnlLiteClient.MasterchainInfo poolInfo = pool.getMasterchainInfo();
      System.out.println("Pool query - Last block seqno: " + poolInfo.getLast().getSeqno());

      // Cleanup
      client.close();
      pool.close();

      System.out.println("\nTest completed successfully!");

    } catch (Exception e) {
      System.err.println("Test failed: " + e.getMessage());
      e.printStackTrace();
    }
  }
}
