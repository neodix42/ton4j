package org.ton.ton4j.toncenter;

import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.ton.ton4j.toncenter.model.*;
import static org.ton.ton4j.toncenter.model.CommonResponses.*;

import java.math.BigInteger;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Comprehensive test class for TonCenter API wrapper covering all 27 endpoints Rate limited to
 * execute no more than one test per second to respect API limits Tests run against both MAINNET and
 * TESTNET networks
 */
@Slf4j
@RunWith(Parameterized.class)
public class TonCenterTest {

  public static final String MAINNET_API_KEY =
      "65126352a1859d70f3dd8846213075fa030de9c0e1a3f0dcab2b9c76cb9d2a88";
  public static final String TESTNET_API_KEY =
      "188b29e2b477d8bb95af5041f75c57b62653add1170634f148ac71d7751d0c71";

  // Test addresses for different scenarios
  public static final String MAINNET_TON_FOUNDATION_WALLET =
      "EQCD39VS5jcptHL8vMjEXrzGaRcCVYto7HUn4bpAOg8xqB2N";
  public static final String TESTNET_TON_FOUNDATION_WALLET =
      "0QCsMm47egxSofgw5Y-l34ZeMw6vPYUUyTIjYT3HTafpmH9O";

  private static final String MAINNET_NFT_ADDRESS =
      "EQAOQdwdw8kGftJCSFgOErM1mBjYPe4DBPq8-AhF6vr9si5N";
  private static final String TESTNET_NFT_ADDRESS =
      "kQBpqkbPrhSjleAQ8W9TJpZBj6K3GKijCH-Uz_6H7UnaqVTI";

  // Parameterized test fields
  private final Network network;
  private final String apiKey;
  private final String tonFoundationWallet;
  private final String nftAddress;

  @Parameterized.Parameters(name = "Network: {0}")
  public static Collection<Object[]> data() {
    return Arrays.asList(
        new Object[][] {
          {Network.MAINNET, MAINNET_API_KEY, MAINNET_TON_FOUNDATION_WALLET, MAINNET_NFT_ADDRESS},
          {Network.TESTNET, TESTNET_API_KEY, TESTNET_TON_FOUNDATION_WALLET, TESTNET_NFT_ADDRESS}
        });
  }

  public TonCenterTest(
      Network network, String apiKey, String tonFoundationWallet, String nftAddress) {
    this.network = network;
    this.apiKey = apiKey;
    this.tonFoundationWallet = tonFoundationWallet;
    this.nftAddress = nftAddress;
  }

  // Rate limiting mechanism - ensures no more than 1 test per second
  private static volatile long lastTestExecutionTime = 0;
  private static final long MIN_TEST_INTERVAL_MS = 1200;

  /** Ensures rate limiting by waiting if necessary before executing a test */
  private void enforceRateLimit() {
    synchronized (TonCenterTest.class) {
      long currentTime = System.currentTimeMillis();
      long timeSinceLastTest = currentTime - lastTestExecutionTime;

      if (timeSinceLastTest < MIN_TEST_INTERVAL_MS) {
        long waitTime = MIN_TEST_INTERVAL_MS - timeSinceLastTest;
        log.info("Rate limiting: waiting {}ms before executing test", waitTime);
        try {
          Thread.sleep(waitTime);
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
          log.warn("Rate limiting sleep interrupted", e);
        }
      }

      lastTestExecutionTime = System.currentTimeMillis();
    }
  }

  // ========== BUILDER AND CONFIGURATION TESTS ==========

  @Test
  public void testBuilderPattern() {
    enforceRateLimit();
    TonCenter client =
        TonCenter.builder()
            .apiKey("test-api-key")
            .network(network)
            .connectTimeout(Duration.ofSeconds(5))
            .readTimeout(Duration.ofSeconds(15))
            .writeTimeout(Duration.ofSeconds(15))
            .build();

    assertNotNull(client);
    client.close();
  }

  @Test
  public void testNetworkSelection() {
    enforceRateLimit();
    // Test current network
    TonCenter networkClient = TonCenter.builder().apiKey("test-key").network(network).build();
    assertNotNull(networkClient);
    networkClient.close();

    // Test with current network again
    TonCenter testnetClient = TonCenter.builder().apiKey("test-key").network(network).build();
    assertNotNull(testnetClient);
    testnetClient.close();

    // Test explicit network enum
    TonCenter explicitClient = TonCenter.builder().apiKey("test-key").network(network).build();
    assertNotNull(explicitClient);
    explicitClient.close();
  }

  @Test
  public void testDefaultBuilder() {
    enforceRateLimit();
    TonCenter client = TonCenter.builder().build();
    assertNotNull(client);
    client.close();
  }

  // ========== ACCOUNT METHODS TESTS (10 endpoints) ==========

  @Test
  public void testGetAddressInformation() {
    enforceRateLimit();
    TonCenter client = TonCenter.builder().apiKey(apiKey).network(network).build();

    try {
      TonResponse<AddressInformationResponse> response =
          client.getAddressInformation(tonFoundationWallet);
      log.info("response {}", response);
      assertTrue("Address information should be successful", response.isSuccess());
      assertNotNull("Result should not be null", response.getResult());
      assertNotNull("Balance should not be null", response.getResult().getBalance());
      log.info("Address info: balance={}", response.getResult().getBalance());
    } finally {
      client.close();
    }
  }

  @Test
  public void testGetExtendedAddressInformation() {
    enforceRateLimit();
    TonCenter client = TonCenter.builder().apiKey(apiKey).network(network).build();

    try {
      TonResponse<ExtendedAddressInformationResponse> response =
          client.getExtendedAddressInformation(tonFoundationWallet);
      log.info("response {}", response.getResult());
      assertTrue("Extended address info should be successful", response.isSuccess());
      assertNotNull("Result should not be null", response.getResult());
      log.info("Extended address info retrieved successfully");
    } finally {
      client.close();
    }
  }

  @Test
  public void testGetWalletInformation() {
    enforceRateLimit();
    TonCenter client = TonCenter.builder().apiKey(apiKey).network(network).build();
    try {
      TonResponse<WalletInformationResponse> response =
          client.getWalletInformation(tonFoundationWallet);
      log.info("response {}", response.getResult());
      assertTrue("Wallet information should be successful", response.isSuccess());
      assertNotNull("Result should not be null", response.getResult());
      log.info("Wallet information retrieved successfully");
    } finally {
      client.close();
    }
  }

  @Test
  public void testGetTransactions() {
    enforceRateLimit();
    TonCenter client = TonCenter.builder().apiKey(apiKey).network(network).build();

    try {
      TonResponse<List<TransactionResponse>> response =
          client.getTransactions(tonFoundationWallet, 5, null, null, null, true);
      log.info("response {}", response.getResult());
      assertTrue("Transactions should be successful", response.isSuccess());
      assertNotNull("Result should not be null", response.getResult());
      log.info("Retrieved {} transactions", response.getResult().size());
    } finally {
      client.close();
    }
  }

  @Test
  public void testGetAddressBalance() {
    enforceRateLimit();
    TonCenter client = TonCenter.builder().apiKey(apiKey).network(network).build();

    try {
      TonResponse<String> response = client.getAddressBalance(tonFoundationWallet);
      assertTrue("Address balance should be successful", response.isSuccess());
      assertNotNull("Balance should not be null", response.getResult());
      log.info("Address balance: {}", response.getResult());
    } finally {
      client.close();
    }
  }

  @Test
  public void testGetBalance() {
    enforceRateLimit();
    TonCenter client = TonCenter.builder().apiKey(apiKey).network(network).build();

    try {
      BigInteger balance = client.getBalance(tonFoundationWallet);
      assertNotNull(balance);
      log.info("Address balance: {}", balance);
    } finally {
      client.close();
    }
  }

  @Test
  public void testGetAddressState() {
    enforceRateLimit();
    TonCenter client = TonCenter.builder().apiKey(apiKey).network(network).build();

    try {
      TonResponse<String> response = client.getAddressState(tonFoundationWallet);
      assertTrue("Address state should be successful", response.isSuccess());
      assertNotNull("State should not be null", response.getResult());
      log.info("Address state: {}", response.getResult());
    } finally {
      client.close();
    }
  }


  @Test
  public void testGetState() {
    enforceRateLimit();
    TonCenter client = TonCenter.builder().apiKey(apiKey).network(network).build();

    try {
      String state = client.getState(tonFoundationWallet);
      assertNotNull(state);
      log.info("Address state: {}", state);
    } finally {
      client.close();
    }
  }

  @Test
  public void testPackAddress() {
    enforceRateLimit();
    TonCenter client = TonCenter.builder().apiKey(apiKey).network(network).build();

    try {
      String rawAddress = "0:83DFD552E63729B472FCBCC8C45EBCC6691702558B68EC7527E1BA403A0F31A8";
      TonResponse<String> response = client.packAddress(rawAddress);
      assertTrue("Pack address should be successful", response.isSuccess());
      assertNotNull("Packed address should not be null", response.getResult());
      log.info("Packed address: {}", response.getResult());
    } finally {
      client.close();
    }
  }

  @Test
  public void testUnpackAddress() {
    enforceRateLimit();
    TonCenter client = TonCenter.builder().apiKey(apiKey).network(network).build();

    try {
      TonResponse<String> response = client.unpackAddress(tonFoundationWallet);
      assertTrue("Unpack address should be successful", response.isSuccess());
      assertNotNull("Unpacked address should not be null", response.getResult());
      log.info("Unpacked address: {}", response.getResult());
    } finally {
      client.close();
    }
  }

  @Test
  public void testDetectAddress() {
    enforceRateLimit();
    TonCenter client = TonCenter.builder().apiKey(apiKey).network(network).build();

    try {
      TonResponse<DetectAddressResponse> response = client.detectAddress(tonFoundationWallet);
      log.info("response {}", response.getResult());
      assertTrue("Detect address should be successful", response.isSuccess());
      assertNotNull("Detected address forms should not be null", response.getResult());
      log.info("Address detection completed successfully");
    } finally {
      client.close();
    }
  }

  @Test
  public void testGetTokenData() {
    enforceRateLimit();
    TonCenter client = TonCenter.builder().apiKey(apiKey).network(network).build();

    try {
      TonResponse<TokenDataResponse> response = client.getTokenData(nftAddress);
      log.info("token data: {}", response.getResult());
      log.info("response {}", response.getResult());
      assertTrue("Token data should be successful", response.isSuccess());
      assertNotNull("Token data should not be null", response.getResult());

      // For NFT collection, check collection content and contract type
      if ("nft_collection".equals(response.getResult().getContractType())) {
        assertNotNull(
            "Collection content should not be null", response.getResult().getCollectionContent());
        assertEquals(
            "Contract type should be nft_collection",
            "nft_collection",
            response.getResult().getContractType());
        log.info("NFT collection data retrieved successfully");
      } else if (response.getResult().getJettonWallet() != null) {
        assertNotNull("Jetton wallet should not be null", response.getResult().getJettonWallet());
        log.info("Jetton wallet data retrieved successfully");
      } else {
        assertNotNull("Some token data should be present", response.getResult().getContractType());
        log.info("Token data retrieved successfully");
      }
    } finally {
      client.close();
    }
  }

  // ========== BLOCK METHODS TESTS (9 endpoints) ==========

  @Test
  public void testGetMasterchainInfo() {
    enforceRateLimit();
    TonCenter client = TonCenter.builder().apiKey(apiKey).network(network).build();

    try {
      TonResponse<MasterchainInfoResponse> response = client.getMasterchainInfo();
      log.info("response {}", response.getResult());
      assertTrue("Masterchain info should be successful", response.isSuccess());
      assertNotNull("Masterchain info should not be null", response.getResult());
      log.info("Masterchain seqno: {}", response.getResult().getLast().getSeqno());
    } finally {
      client.close();
    }
  }

  @Test
  public void testGetMasterchainBlockSignatures() {
    enforceRateLimit();
    TonCenter client = TonCenter.builder().apiKey(apiKey).network(network).build();

    try {
      TonResponse<MasterchainBlockSignaturesResponse> response =
          client.getMasterchainBlockSignatures(network == Network.MAINNET ? 50706498L : 34098432L);
      log.info("response {}", response.getResult());
      assertTrue("Block signatures should be successful", response.isSuccess());
      assertNotNull("Block signatures should not be null", response.getResult());
      log.info("Block signatures retrieved successfully");
    } finally {
      client.close();
    }
  }

  @Test
  public void testGetShardBlockProof() {
    enforceRateLimit();
    TonCenter client = TonCenter.builder().apiKey(apiKey).network(network).build();

    try {
      TonResponse<ShardBlockProofResponse> response =
          client.getShardBlockProof(
              -1, -9223372036854775808L, network == Network.MAINNET ? 50706498L : 34098432L);
      log.info("response {}", response.getResult());
      assertTrue("Shard block proof should be successful", response.isSuccess());
      assertNotNull(
          "Shard block proof masterchainId should not be null",
          response.getResult().getMasterchainId());
      log.info("Shard block proof retrieved successfully");
    } finally {
      client.close();
    }
  }

  @Test
  public void testGetConsensusBlock() {
    enforceRateLimit();
    TonCenter client = TonCenter.builder().apiKey(apiKey).network(network).build();

    try {
      TonResponse<ConsensusBlockResponse> response = client.getConsensusBlock();
      log.info("response {}", response.getResult());
      assertTrue("Consensus block should be successful", response.isSuccess());
      assertNotNull("Consensus block should not be null", response.getResult());
      log.info("Consensus block retrieved successfully");
    } finally {
      client.close();
    }
  }

  @Test
  public void testLookupBlock() {
    enforceRateLimit();
    TonCenter client = TonCenter.builder().apiKey(apiKey).network(network).build();

    try {
      TonResponse<LookupBlockResponse> response =
          client.lookupBlockBySeqno(-1, -9223372036854775808L, 1000L);
      log.info("response {}", response.getResult());
      assertTrue("Lookup block should be successful", response.isSuccess());
      assertNotNull("Block lookup result should not be null", response.getResult());
      log.info("Block lookup completed successfully");
    } finally {
      client.close();
    }
  }

  @Test
  public void testGetShards() {
    enforceRateLimit();
    TonCenter client = TonCenter.builder().apiKey(apiKey).network(network).build();

    try {
      TonResponse<ShardsResponse> response = client.getShards(1000L);
      assertTrue("Get shards should be successful", response.isSuccess());
      assertNotNull("Shards list should not be null", response.getResult().getShards());
      assertTrue("Should have at least one shard", response.getResult().getShards().size() > 0);

      // Test the typed response structure
      ShardsResponse.BlockIdExt firstShard = response.getResult().getShards().get(0);
      assertNotNull("First shard should not be null", firstShard);
      assertNotNull("Shard workchain should not be null", firstShard.getWorkchain());
      assertNotNull("Shard seqno should not be null", firstShard.getSeqno());

      log.info(
          "Shards information retrieved successfully: {} shards",
          response.getResult().getShards().size());
      log.info(
          "First shard: workchain={}, seqno={}", firstShard.getWorkchain(), firstShard.getSeqno());
    } finally {
      client.close();
    }
  }

  @Test
  public void testGetBlockTransactions() {
    enforceRateLimit();
    TonCenter client = TonCenter.builder().apiKey(apiKey).network(network).build();

    try {
      TonResponse<BlockTransactionsResponse> response =
          client.getBlockTransactions(-1, -9223372036854775808L, 1000L);
      log.info("response {}", response.getResult());
      assertTrue("Block transactions should be successful", response.isSuccess());
      assertNotNull("Block transactions should not be null", response.getResult());
      log.info("Block transactions retrieved successfully");
    } finally {
      client.close();
    }
  }

  @Test
  public void testGetBlockTransactionsExt() {
    enforceRateLimit();
    TonCenter client = TonCenter.builder().apiKey(apiKey).network(network).build();

    try {
      TonResponse<BlockTransactionsResponse> response =
          client.getBlockTransactionsExt(
              -1, -9223372036854775808L, 1000L, null, null, null, null, null);
      log.info("response {}", response.getResult());
      assertTrue("Block transactions ext should be successful", response.isSuccess());
      assertNotNull("Block transactions ext should not be null", response.getResult());
      log.info("Block transactions ext retrieved successfully");
    } finally {
      client.close();
    }
  }

  @Test
  public void testGetBlockHeader() {
    enforceRateLimit();
    TonCenter client = TonCenter.builder().apiKey(apiKey).network(network).build();

    try {
      TonResponse<BlockHeaderResponse> response =
          client.getBlockHeader(-1, -9223372036854775808L, 1000L);
      log.info("response {}", response.getResult());
      assertTrue("Block header should be successful", response.isSuccess());
      assertNotNull("Block header should not be null", response.getResult());
    } finally {
      client.close();
    }
  }

  @Test
  public void testGetOutMsgQueueSizes() {
    enforceRateLimit();
    TonCenter client = TonCenter.builder().apiKey(apiKey).network(network).build();

    try {
      TonResponse<OutMsgQueueSizesResponse> response = client.getOutMsgQueueSizes();
      log.info("response {}", response.getResult());
      assertTrue("Out msg queue sizes should be successful", response.isSuccess());
      assertNotNull("Queue sizes should not be null", response.getResult());
      log.info("Out message queue sizes retrieved successfully");
    } finally {
      client.close();
    }
  }

  // ========== CONFIGURATION METHODS TESTS (2 endpoints) ==========

  @Test
  public void testGetConfigParam() {
    enforceRateLimit();
    TonCenter client = TonCenter.builder().apiKey(apiKey).network(network).build();

    try {
      TonResponse<ConfigParamResponse> response =
          client.getConfigParam(0); // Config param 0 (address of config smart contract)
      log.info("response {}", response.getResult());
      assertTrue("Config param should be successful", response.isSuccess());
      assertNotNull("Config param should not be null", response.getResult());
      log.info("Config param 0 retrieved successfully");
    } finally {
      client.close();
    }
  }

  @Test
  public void testGetConfigAll() {
    enforceRateLimit();
    TonCenter client = TonCenter.builder().apiKey(apiKey).network(network).build();

    try {
      TonResponse<ConfigAllResponse> response = client.getConfigAll();
      log.info("response {}", response.getResult());
      assertTrue("Config all should be successful", response.isSuccess());
      assertNotNull("Full config should not be null", response.getResult());
      log.info("Full config retrieved successfully");
    } finally {
      client.close();
    }
  }

  // ========== TRANSACTION METHODS TESTS (3 endpoints) ==========

  @Test
  public void testTryLocateTx() { // todo fix in toncenter
    enforceRateLimit();
    TonCenter client = TonCenter.builder().apiKey(apiKey).network(network).build();

    try {
      TonResponse<LocateTxResponse> response =
          client.tryLocateTx(
              network == Network.MAINNET
                  ? "EQAuMjwyuQBaaxM6ooRJWbuUacQvBgVEWQOSSlbMERG0ljRD"
                  : "0QCsMm47egxSofgw5Y-l34ZeMw6vPYUUyTIjYT3HTafpmH9O",
              network == Network.MAINNET
                  ? "EQDEruSI2frAF-GdzpjDLWWBKnwREDAJmu7eIEFG6zdUlXVE"
                  : "kQAykk_XYcWpo4kCzxwoDYrj4akTMqg25l03BZn1QZvncQen",
              network == Network.MAINNET ? 26521292000002L : 37560648000003L);
      log.info("response {}", response.getResult().getTransactionId());
      log.info("Try locate tx completed: success={}", response.isSuccess());
    } finally {
      client.close();
    }
  }

  @Test
  public void testTryLocateResultTx() {
    enforceRateLimit();
    TonCenter client = TonCenter.builder().apiKey(apiKey).network(network).build();

    try {
      TonResponse<LocateTxResponse> response =
          client.tryLocateResultTx(
              network == Network.MAINNET
                  ? "EQAuMjwyuQBaaxM6ooRJWbuUacQvBgVEWQOSSlbMERG0ljRD"
                  : "0QCsMm47egxSofgw5Y-l34ZeMw6vPYUUyTIjYT3HTafpmH9O",
              network == Network.MAINNET
                  ? "EQDEruSI2frAF-GdzpjDLWWBKnwREDAJmu7eIEFG6zdUlXVE"
                  : "kQAykk_XYcWpo4kCzxwoDYrj4akTMqg25l03BZn1QZvncQen",
              network == Network.MAINNET ? 26521292000002L : 37560648000003L);
      log.info("response {}", response.getResult().getTransactionId());
      log.info("Try locate result tx completed: success={}", response.isSuccess());
    } finally {
      client.close();
    }
  }

  @Test
  public void testTryLocateSourceTx() {
    enforceRateLimit();
    TonCenter client = TonCenter.builder().apiKey(apiKey).network(network).build();

    try {
      TonResponse<LocateTxResponse> response =
          client.tryLocateSourceTx(
              network == Network.MAINNET
                  ? "EQAuMjwyuQBaaxM6ooRJWbuUacQvBgVEWQOSSlbMERG0ljRD"
                  : "0QCsMm47egxSofgw5Y-l34ZeMw6vPYUUyTIjYT3HTafpmH9O",
              network == Network.MAINNET
                  ? "EQDEruSI2frAF-GdzpjDLWWBKnwREDAJmu7eIEFG6zdUlXVE"
                  : "kQAykk_XYcWpo4kCzxwoDYrj4akTMqg25l03BZn1QZvncQen",
              network == Network.MAINNET ? 26521292000002L : 37560648000003L);
      log.info("response {}", response.getResult().getTransactionId());
      log.info("Try locate source tx completed: success={}", response.isSuccess());
    } finally {
      client.close();
    }
  }

  // ========== RUN METHOD TESTS (1 endpoint) ==========

  @Test
  public void testRunGetMethod() {
    enforceRateLimit();
    TonCenter client = TonCenter.builder().apiKey(apiKey).network(network).build();

    try {
      // Test calling seqno method on a wallet
      TonResponse<RunGetMethodResponse> response =
          client.runGetMethod(tonFoundationWallet, "seqno", new ArrayList<>());
      log.info("response {}", response.getResult());
      assertTrue("Run get method should be successful", response.isSuccess());
      assertNotNull("Method result should not be null", response.getResult());
      log.info("Get method 'seqno' executed successfully");
    } finally {
      client.close();
    }
  }

  @Test
  public void testGetSeqno() {
    enforceRateLimit();
    TonCenter client = TonCenter.builder().apiKey(apiKey).network(network).build();

    try {
      // Test calling seqno method on a wallet
      long seqno =              client.getSeqno(tonFoundationWallet);
      log.info("seqno {}", seqno);
      assertTrue(seqno > 0);
      log.info("Get method 'seqno' executed successfully");
    } finally {
      client.close();
    }
  }

  @Test
  public void testGetSubWalletId() {
    enforceRateLimit();
    TonCenter client = TonCenter.builder().apiKey(apiKey).network(network).build();

    try {
      // Test calling seqno method on a wallet
      long subWalletId = client.getSubWalletId(tonFoundationWallet);
      log.info("subWalletId {}", subWalletId);
      assertTrue(subWalletId >= 0);
      log.info("Get method 'get_subwallet_id' executed successfully");
    } finally {
      client.close();
    }
  }

  // ========== SEND METHODS TESTS (4 endpoints) ==========
  // Note: These tests use dummy data and will likely fail, but test the API structure

  @Test
  public void testSendBoc() {
    enforceRateLimit();
    TonCenter client = TonCenter.builder().apiKey(apiKey).network(network).build();

    try {
      // Using dummy BOC data - this will fail but tests the endpoint
      String dummyBoc =
          "te6ccgEBAgEAqgAB4YgA2ZpktQsYby0n9cV5VWOFINBjScIU2HdondFsK3lDpEAFG8W4Jpf7AeOqfzL9vZ79mX3eM6UEBxZvN6+QmpYwXBq32QOBIrP4lF5ijGgQmZbC6KDeiiptxmTNwl5f59OAGU1NGLsixYlYAAAA2AAcAQBoYgBZQOG7qXmeA/2Tw1pLX2IkcQ5h5fxWzzcBskMJbVVRsKNaTpAAAAAAAAAAAAAAAAAAAA==";
      TonResponse<SendBocResponse> response = client.sendBoc(dummyBoc);
      log.info("Send BOC completed: success={}", response.isSuccess());
    } catch (TonCenterApiException e) {
      // expected
    } finally {
      client.close();
    }
  }

  @Test
  public void testSendBocReturnHash() {
    enforceRateLimit();
    TonCenter client = TonCenter.builder().apiKey(apiKey).network(network).build();

    try {
      // Using dummy BOC data - this will fail but tests the endpoint
      String dummyBoc =
          "te6ccgEBAgEAqgAB4YgA2ZpktQsYby0n9cV5VWOFINBjScIU2HdondFsK3lDpEAFG8W4Jpf7AeOqfzL9vZ79mX3eM6UEBxZvN6+QmpYwXBq32QOBIrP4lF5ijGgQmZbC6KDeiiptxmTNwl5f59OAGU1NGLsixYlYAAAA2AAcAQBoYgBZQOG7qXmeA/2Tw1pLX2IkcQ5h5fxWzzcBskMJbVVRsKNaTpAAAAAAAAAAAAAAAAAAAA==";

      TonResponse<String> response = client.sendBocReturnHash(dummyBoc);
      log.info("Send BOC return hash completed: success={}", response.isSuccess());
    } catch (TonCenterApiException e) {
      // expected
    } finally {
      client.close();
    }
  }

  @Test
  public void testSendQuery() {
    enforceRateLimit();
    TonCenter client = TonCenter.builder().apiKey(apiKey).network(network).build();

    try {
      // Using dummy data - this will fail but tests the endpoint
      TonResponse<SendQueryResponse> response =
          client.sendQuery(
              tonFoundationWallet,
              "te6ccgEBAgEAqgAB4YgA2ZpktQsYby0n9cV5VWOFINBjScIU2HdondFsK3lDpEAFG8W4Jpf7AeOqfzL9vZ79mX3eM6UEBxZvN6+QmpYwXBq32QOBIrP4lF5ijGgQmZbC6KDeiiptxmTNwl5f59OAGU1NGLsixYlYAAAA2AAcAQBoYgBZQOG7qXmeA/2Tw1pLX2IkcQ5h5fxWzzcBskMJbVVRsKNaTpAAAAAAAAAAAAAAAAAAAA==",
              "",
              "");
      log.info("Send query completed: success={}", response.isSuccess());
    } catch (TonCenterApiException e) {
      // expected
    } finally {
      client.close();
    }
  }

  @Test
  public void testEstimateFee() {
    enforceRateLimit();
    TonCenter client = TonCenter.builder().apiKey(apiKey).network(network).build();

    try {
      // Using dummy data - this will fail but tests the endpoint
      TonResponse<EstimateFeeResponse> response =
          client.estimateFee(
              tonFoundationWallet,
              "te6ccgEBAgEAqgAB4YgA2ZpktQsYby0n9cV5VWOFINBjScIU2HdondFsK3lDpEAFG8W4Jpf7AeOqfzL9vZ79mX3eM6UEBxZvN6+QmpYwXBq32QOBIrP4lF5ijGgQmZbC6KDeiiptxmTNwl5f59OAGU1NGLsixYlYAAAA2AAcAQBoYgBZQOG7qXmeA/2Tw1pLX2IkcQ5h5fxWzzcBskMJbVVRsKNaTpAAAAAAAAAAAAAAAAAAAA==");
      log.info("Estimate fee completed: success={}", response.isSuccess());
    } finally {
      client.close();
    }
  }

  // ========== CONVENIENCE METHODS TESTS ==========

  @Test
  public void testConvenienceMethods() {
    enforceRateLimit();
    TonCenter client = TonCenter.builder().apiKey(apiKey).network(network).build();

    try { // todo works with stage.toncenter
      // Test convenience method for transactions
      TonResponse<List<TransactionResponse>> response = client.getTransactions(tonFoundationWallet);
      assertTrue("Convenience method should work", response.isSuccess());
      log.info("Convenience method getTransactions() works");

      // Test convenience method with limit
      TonResponse<List<TransactionResponse>> response2 =
          client.getTransactions(tonFoundationWallet, 3);
      assertTrue("Convenience method with limit should work", response2.isSuccess());
      log.info("Convenience method getTransactions(limit) works");
      log.info("response {}", response2.getResult());
    } finally {
      client.close();
    }
  }

  // ========== ERROR HANDLING TESTS ==========

  @Test
  public void testExceptionHandling() {
    enforceRateLimit();
    TonCenter client =
        TonCenter.builder()
            .endpoint("https://invalid-endpoint-that-does-not-exist.com/api/v2")
            .build();

    try {
      client.getMasterchainInfo();
      fail("Should throw TonCenterException for invalid endpoint");
    } catch (TonCenterException e) {
      // Expected
      assertTrue(
          "Should contain network error",
          e.getMessage().contains("Network error") || e.getMessage().contains("HTTP error"));
      log.info("Exception handling works correctly");
    }

    client.close();
  }

  @Test
  public void testApiKeyValidation() {
    enforceRateLimit();
    TonCenter client = TonCenter.builder().apiKey("invalid-api-key").network(network).build();

    try {
      client.getMasterchainInfo();
      // May succeed or fail depending on API key validation
    } catch (TonCenterException e) {
      log.info("Network error with invalid API key: {}", e.getMessage());
    }

    client.close();
  }

  // ========== SUMMARY TEST ==========

  @Test
  public void testAllEndpointsCount() {
    enforceRateLimit();
    // This test verifies that all 27 endpoints are implemented

    TonCenter client = TonCenter.builder().build();

    // Account methods (10)
    assertNotNull("getAddressInformation should exist", getMethod(client, "getAddressInformation"));
    assertNotNull(
        "getExtendedAddressInformation should exist",
        getMethod(client, "getExtendedAddressInformation"));
    assertNotNull("getWalletInformation should exist", getMethod(client, "getWalletInformation"));
    assertNotNull("getTransactions should exist", getMethod(client, "getTransactions"));
    assertNotNull("getAddressBalance should exist", getMethod(client, "getAddressBalance"));
    assertNotNull("getAddressState should exist", getMethod(client, "getAddressState"));
    assertNotNull("packAddress should exist", getMethod(client, "packAddress"));
    assertNotNull("unpackAddress should exist", getMethod(client, "unpackAddress"));
    assertNotNull("detectAddress should exist", getMethod(client, "detectAddress"));
    assertNotNull("getTokenData should exist", getMethod(client, "getTokenData"));

    // Block methods (9)
    assertNotNull("getMasterchainInfo should exist", getMethod(client, "getMasterchainInfo"));
    assertNotNull(
        "getMasterchainBlockSignatures should exist",
        getMethod(client, "getMasterchainBlockSignatures"));
    assertNotNull("getShardBlockProof should exist", getMethod(client, "getShardBlockProof"));
    assertNotNull("getConsensusBlock should exist", getMethod(client, "getConsensusBlock"));
    assertNotNull("lookupBlock should exist", getMethod(client, "lookupBlock"));
    assertNotNull("getShards should exist", getMethod(client, "getShards"));
    assertNotNull("getBlockTransactions should exist", getMethod(client, "getBlockTransactions"));
    assertNotNull(
        "getBlockTransactionsExt should exist", getMethod(client, "getBlockTransactionsExt"));
    assertNotNull("getBlockHeader should exist", getMethod(client, "getBlockHeader"));
    assertNotNull("getOutMsgQueueSizes should exist", getMethod(client, "getOutMsgQueueSizes"));

    // Config methods (2)
    assertNotNull("getConfigParam should exist", getMethod(client, "getConfigParam"));
    assertNotNull("getConfigAll should exist", getMethod(client, "getConfigAll"));

    // Transaction methods (3)
    assertNotNull("tryLocateTx should exist", getMethod(client, "tryLocateTx"));
    assertNotNull("tryLocateResultTx should exist", getMethod(client, "tryLocateResultTx"));
    assertNotNull("tryLocateSourceTx should exist", getMethod(client, "tryLocateSourceTx"));

    // Run method (1)
    assertNotNull("runGetMethod should exist", getMethod(client, "runGetMethod"));

    // Send methods (4)
    assertNotNull("sendBoc should exist", getMethod(client, "sendBoc"));
    assertNotNull("sendBocReturnHash should exist", getMethod(client, "sendBocReturnHash"));
    assertNotNull("sendQuery should exist", getMethod(client, "sendQuery"));
    assertNotNull("estimateFee should exist", getMethod(client, "estimateFee"));

    log.info("✅ All 27 TonCenter API endpoints are implemented!");
    client.close();
  }

  private java.lang.reflect.Method getMethod(Object obj, String methodName) {
    try {
      for (java.lang.reflect.Method method : obj.getClass().getDeclaredMethods()) {
        if (method.getName().equals(methodName)) {
          return method;
        }
      }
      return null;
    } catch (Exception e) {
      return null;
    }
  }
}
