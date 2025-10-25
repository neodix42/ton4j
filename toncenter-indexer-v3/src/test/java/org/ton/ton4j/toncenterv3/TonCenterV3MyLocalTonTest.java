package org.ton.ton4j.toncenterv3;

import static org.junit.Assert.*;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.ton.ton4j.toncenterv3.model.CommonModels;
import org.ton.ton4j.toncenterv3.model.ResponseModels.*;

/**
 * Test class for TonCenterV3 API client Note: These tests require a valid API key and network
 * connection Set TONCENTER_API_KEY environment variable to run tests
 *
 * <p>No rate limit in locally deployed TonCenter
 */
@Slf4j
public class TonCenterV3MyLocalTonTest {

  private static final String TEST_ADDRESS =
      "-1:6744e92c6f71c776fbbcef299e31bf76f39c245cd56f2075b89c6a22026b4131";
  private static final String API_KEY = System.getenv("TONCENTER_API_KEY");

  /**
   * Ensures rate limiting by waiting if necessary before executing a test. This is especially
   * important when running without an API key (1 req/sec limit).
   */
  private void enforceRateLimit() {
    // no rate limit for local host
  }

  private TonCenterV3 createClient() {
    TonCenterV3.TonCenterV3Builder builder =
        TonCenterV3.builder()
            .network(Network.MY_LOCAL_TON)
            .connectTimeout(Duration.ofSeconds(15))
            .readTimeout(Duration.ofSeconds(30));

    if (API_KEY != null && !API_KEY.isEmpty()) {
      builder.apiKey(API_KEY);
    }

    return builder.build();
  }

  @Test
  public void testBuilderPattern() {
    enforceRateLimit();
    TonCenterV3 client =
        TonCenterV3.builder()
            .mainnet()
            .apiKey("test-key")
            .connectTimeout(Duration.ofSeconds(10))
            .readTimeout(Duration.ofSeconds(30))
            .writeTimeout(Duration.ofSeconds(30))
            .debug()
            .uniqueRequests()
            .build();

    assertNotNull(client);
    client.close();
  }

  @Test
  public void testGetMasterchainInfo() {
    enforceRateLimit();
    TonCenterV3 client = createClient();
    try {
      MasterchainInfo info = client.getMasterchainInfo();
      assertNotNull(info);
      assertNotNull(info.getFirst());
      assertNotNull(info.getLast());
      log.info("First block seqno: {}", info.getFirst().getSeqno());
      log.info("Last block seqno: {}", info.getLast().getSeqno());
      log.info(info.toString());
    } finally {
      client.close();
    }
  }

  @Test
  public void testGetAccountStates() {
    enforceRateLimit();
    TonCenterV3 client = createClient();
    try {
      List<String> addresses = Collections.singletonList(TEST_ADDRESS);
      AccountStatesResponse response = client.getAccountStates(addresses, true);
      assertNotNull(response);
      assertNotNull(response.getAccounts());
      log.info("Retrieved {}", response.getAccounts().size() + " account states");
      log.info(response.toString());
    } finally {
      client.close();
    }
  }

  @Test
  public void testGetWalletStates() {
    enforceRateLimit();
    TonCenterV3 client = createClient();
    try {
      List<String> addresses = Collections.singletonList(TEST_ADDRESS);
      WalletStatesResponse response = client.getWalletStates(addresses);
      assertNotNull(response);
      assertNotNull(response.getWallets());
      log.info("Retrieved {}", response.getWallets().size() + " wallet states");
      log.info(response.toString());
    } finally {
      client.close();
    }
  }

  @Test
  public void testGetBlocks() {
    enforceRateLimit();
    TonCenterV3 client = createClient();
    try {
      BlocksResponse response =
          client.getBlocks(-1, null, null, null, null, null, null, null, null, null, 10, 0, "desc");
      assertNotNull(response);
      assertNotNull(response.getBlocks());
      log.info("Retrieved {}", response.getBlocks().size() + " blocks");
      log.info(response.toString());
    } finally {
      client.close();
    }
  }

  @Test
  public void testGetTransactions() {
    enforceRateLimit();
    TonCenterV3 client = createClient();
    try {
      TransactionsResponse response =
          client.getTransactions(
              null,
              null,
              null,
              null,
              Collections.singletonList(TEST_ADDRESS),
              null,
              null,
              null,
              null,
              null,
              null,
              null,
              10,
              0,
              "desc");
      assertNotNull(response);
      assertNotNull(response.getTransactions());
      log.info("Retrieved {}", response.getTransactions().size() + " transactions");
      log.info(response.toString());
    } finally {
      client.close();
    }
  }

  @Test
  public void testGetMessages() {
    enforceRateLimit();
    TonCenterV3 client = createClient();
    try {
      MessagesResponse response =
          client.getMessages(
              null,
              null,
              null,
              TEST_ADDRESS,
              null,
              null,
              null,
              null,
              null,
              null,
              null,
              null,
              10,
              0,
              "desc");
      assertNotNull(response);
      assertNotNull(response.getMessages());
      log.info("Retrieved {}", response.getMessages().size() + " messages");
      for (CommonModels.Message message : response.getMessages()) {
        log.info("Message: {}", message);
      }
    } finally {
      client.close();
    }
  }

  @Test
  public void testGetJettonMasters() {
    enforceRateLimit();
    TonCenterV3 client = createClient();
    try {
      JettonMastersResponse response = client.getJettonMasters(null, null, 10, 0);
      assertNotNull(response);
      assertNotNull(response.getJettonMasters());
      log.info("Retrieved {}", response.getJettonMasters().size() + " jetton masters");
      log.info(response.toString());
    } finally {
      client.close();
    }
  }

  @Test
  public void testGetNFTCollections() {
    enforceRateLimit();
    TonCenterV3 client = createClient();
    try {
      NFTCollectionsResponse response = client.getNFTCollections(null, null, 10, 0);
      assertNotNull(response);
      assertNotNull(response.getNftCollections());
      log.info("Retrieved {}", response.getNftCollections().size() + " NFT collections");
      log.info(response.toString());
    } finally {
      client.close();
    }
  }

  @Test
  public void testGetTopAccountsByBalance() {
    enforceRateLimit();
    TonCenterV3 client = createClient();
    try {
      List<AccountBalance> response = client.getTopAccountsByBalance(10, 0);
      assertNotNull(response);
      log.info("Retrieved {}", response.size() + " top accounts");
      if (!response.isEmpty()) {
        log.info("Top account balance: {}", response.get(0).getBalance());
      }
      log.info(response.toString());
    } finally {
      client.close();
    }
  }

  @Test
  public void testThreadSafety() throws InterruptedException {
    enforceRateLimit();
    TonCenterV3 client = createClient();

    Thread[] threads = new Thread[5];
    for (int i = 0; i < threads.length; i++) {
      threads[i] =
          new Thread(
              () -> {
                MasterchainInfo info = client.getMasterchainInfo();
                assertNotNull(info);
                log.info(Thread.currentThread().getName() + " - Success");
              });
      threads[i].start();
    }

    for (Thread thread : threads) {
      thread.join();
    }

    client.close();
  }

  @Test
  public void testErrorHandling() {
    enforceRateLimit();
    TonCenterV3 client = TonCenterV3.builder().testnet().apiKey("invalid-key").build();

    try {
      client.getMasterchainInfo();
      fail("Should have thrown an exception");
    } catch (TonCenterException e) {
      log.info("Expected error: {}", e.getMessage());
      // Accept various error messages (API key errors, HTTP errors, etc.)
      assertTrue(
          "Error message should indicate a problem",
          e.getMessage() != null && !e.getMessage().isEmpty());
    } finally {
      client.close();
    }
  }

  // ========== ADDITIONAL ACCOUNT METHOD TESTS ==========

  @Test
  public void testGetAddressBook() {
    enforceRateLimit();
    TonCenterV3 client = createClient();
    try {
      List<String> addresses = Collections.singletonList(TEST_ADDRESS);
      Map<String, Map<String, Object>> response = client.getAddressBook(addresses);
      assertNotNull(response);
      log.info("Retrieved address book for {}", addresses.size() + " addresses");
      log.info(response.toString());
    } finally {
      client.close();
    }
  }

  @Test
  public void testGetAddressInformation() {
    enforceRateLimit();
    TonCenterV3 client = createClient();
    try {
      V2AddressInformation response = client.getAddressInformation(TEST_ADDRESS);
      assertNotNull(response);
      log.info("Retrieved address information");
      log.info(response.toString());
    } finally {
      client.close();
    }
  }

  @Test
  public void testGetMetadata() {
    enforceRateLimit();
    TonCenterV3 client = createClient();
    try {
      List<String> addresses = Collections.singletonList(TEST_ADDRESS);
      Map<String, Map<String, Object>> response = client.getMetadata(addresses);
      assertNotNull(response);
      log.info("Retrieved metadata for {}", addresses.size() + " addresses");
      log.info(response.toString());
    } finally {
      client.close();
    }
  }

  @Test
  public void testGetWalletInformation() {
    enforceRateLimit();
    TonCenterV3 client = createClient();
    try {
      V2WalletInformation response = client.getWalletInformation(TEST_ADDRESS);
      assertNotNull(response);
      log.info("Retrieved wallet information");
      log.info(response.toString());
    } finally {
      client.close();
    }
  }

  // ========== ACTION METHOD TESTS ==========

  @Test
  public void testGetActions() {
    enforceRateLimit();
    TonCenterV3 client = createClient();
    try {
      ActionsResponse response =
          client.getActions(
              TEST_ADDRESS,
              null,
              null,
              null,
              null,
              null,
              null,
              null,
              null,
              null,
              null,
              null,
              null,
              null,
              null,
              10,
              0,
              "desc");
      assertNotNull(response);
      log.info("Retrieved actions");
      log.info(response.toString());
    } finally {
      client.close();
    }
  }

  @Test
  public void testGetTraces() {
    enforceRateLimit();
    TonCenterV3 client = createClient();
    try {
      TracesResponse response =
          client.getTraces(
              TEST_ADDRESS,
              null,
              null,
              null,
              null,
              null,
              null,
              null,
              null,
              null,
              null,
              10,
              0,
              "desc");
      assertNotNull(response);
      log.info("Retrieved traces");
      log.info(response.toString());
    } finally {
      client.close();
    }
  }

  // ========== ADDITIONAL BLOCKCHAIN METHOD TESTS ==========

  @Test
  public void testGetMasterchainBlockShards() {
    enforceRateLimit();
    TonCenterV3 client = createClient();
    try {
      // Get current masterchain info first
      MasterchainInfo info = client.getMasterchainInfo();
      Integer seqno = info.getLast().getSeqno();

      TransactionsResponse response = client.getMasterchainBlockShards(seqno, 10, 0);
      assertNotNull(response);
      log.info("Retrieved masterchain block shards");
      log.info(response.toString());
    } finally {
      client.close();
    }
  }

  @Test
  public void testGetTransactionsByMasterchainBlock() {
    enforceRateLimit();
    TonCenterV3 client = createClient();
    try {
      // Get current masterchain info first
      MasterchainInfo info = client.getMasterchainInfo();
      Integer seqno = info.getLast().getSeqno();

      TransactionsResponse response =
          client.getTransactionsByMasterchainBlock(seqno, 10, 0, "desc");
      assertNotNull(response);
      log.info("Retrieved transactions by masterchain block");
      log.info(response.toString());
    } finally {
      client.close();
    }
  }

  // ========== JETTON METHOD TESTS ==========

  @Test
  public void testGetJettonWallets() {
    enforceRateLimit();
    TonCenterV3 client = createClient();
    try {
      JettonWalletsResponse response =
          client.getJettonWallets(
              null, Collections.singletonList(TEST_ADDRESS), null, null, 10, 0, "desc");
      assertNotNull(response);
      log.info("Retrieved jetton wallets");
      log.info(response.toString());
    } finally {
      client.close();
    }
  }

  @Test
  public void testGetJettonTransfers() {
    enforceRateLimit();
    TonCenterV3 client = createClient();
    try {
      JettonTransfersResponse response =
          client.getJettonTransfers(
              Collections.singletonList(TEST_ADDRESS),
              null,
              null,
              null,
              null,
              null,
              null,
              null,
              10,
              0,
              "desc");
      assertNotNull(response);
      log.info("Retrieved jetton transfers");
      log.info(response.toString());
    } finally {
      client.close();
    }
  }

  @Test
  public void testGetJettonBurns() {
    enforceRateLimit();
    TonCenterV3 client = createClient();
    try {
      JettonBurnsResponse response =
          client.getJettonBurns(
              Collections.singletonList(TEST_ADDRESS),
              null,
              null,
              null,
              null,
              null,
              null,
              10,
              0,
              "desc");
      assertNotNull(response);
      log.info("Retrieved jetton burns");
      log.info(response.toString());
    } finally {
      client.close();
    }
  }

  // ========== NFT METHOD TESTS ==========

  @Test
  public void testGetNFTItems() {
    enforceRateLimit();
    TonCenterV3 client = createClient();
    try {
      NFTItemsResponse response =
          client.getNFTItems(
              null, Collections.singletonList(TEST_ADDRESS), null, null, false, 10, 0);
      assertNotNull(response);
      log.info("Retrieved NFT items");
      log.info(response.toString());
    } finally {
      client.close();
    }
  }

  @Test
  public void testGetNFTTransfers() {
    enforceRateLimit();
    TonCenterV3 client = createClient();
    try {
      NFTTransfersResponse response =
          client.getNFTTransfers(
              Collections.singletonList(TEST_ADDRESS),
              null,
              null,
              null,
              null,
              null,
              null,
              null,
              10,
              0,
              "desc");
      assertNotNull(response);
      log.info("Retrieved NFT transfers");
      log.info(response.toString());
    } finally {
      client.close();
    }
  }

  // ========== DNS METHOD TESTS ==========

  @Test
  public void testGetDNSRecords() {
    enforceRateLimit();
    TonCenterV3 client = createClient();
    try {
      DNSRecordsResponse response = client.getDNSRecords(TEST_ADDRESS, null, 10, 0);
      assertNotNull(response);
      log.info("Retrieved DNS records");
      log.info(response.toString());
    } finally {
      client.close();
    }
  }

  // ========== MULTISIG METHOD TESTS ==========

  @Test
  public void testGetMultisigWallets() {
    enforceRateLimit();
    TonCenterV3 client = createClient();
    try {
      MultisigResponse response =
          client.getMultisigWallets(
              null, Collections.singletonList(TEST_ADDRESS), 10, 0, "desc", false);
      assertNotNull(response);
      log.info("Retrieved multisig wallets");
      log.info(response.toString());
    } finally {
      client.close();
    }
  }

  @Test
  public void testGetMultisigOrders() {
    enforceRateLimit();
    TonCenterV3 client = createClient();
    try {
      MultisigOrderResponse response = client.getMultisigOrders(null, null, false, 10, 0, "desc");
      assertNotNull(response);
      log.info("Retrieved multisig orders");
      log.info(response.toString());
    } finally {
      client.close();
    }
  }

  // ========== VESTING METHOD TESTS ==========

  @Test
  public void testGetVestingContracts() {
    enforceRateLimit();
    TonCenterV3 client = createClient();
    try {
      VestingContractsResponse response =
          client.getVestingContracts(null, Collections.singletonList(TEST_ADDRESS), false, 10, 0);
      assertNotNull(response);
      log.info("Retrieved vesting contracts");
      log.info(response.toString());
    } finally {
      client.close();
    }
  }
}
