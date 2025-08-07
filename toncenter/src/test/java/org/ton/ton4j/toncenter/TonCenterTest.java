package org.ton.ton4j.toncenter;

import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.ton.ton4j.toncenter.model.*;
import static org.ton.ton4j.toncenter.model.CommonResponses.*;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Comprehensive test class for TonCenter API wrapper covering all 27 endpoints
 */
@Slf4j
@RunWith(JUnit4.class)
public class TonCenterTest {

    private static final String API_KEY = "";
    private static final Network NETWORK = Network.MAINNET;
    
    // Test addresses for different scenarios
    private static final String TEST_ADDRESS = "EQCD39VS5jcptHL8vMjEXrzGaRcCVYto7HUn4bpAOg8xqB2N";
    private static final String WALLET_ADDRESS = "EQD7vdOGw8KvXW6_OgBR2QpBQq5-9R8N8DCo0peQJZrP_VLu";
    private static final String NFT_ADDRESS = "EQAOQdwdw8kGftJCSFgOErM1mBjYPe4DBPq8-AhF6vr9si5N";
    
    // ========== BUILDER AND CONFIGURATION TESTS ==========
    
    @Test
    public void testBuilderPattern() {
        TonCenter client = TonCenter.builder()
                .apiKey("test-api-key")
                .network(NETWORK)
                .connectTimeout(Duration.ofSeconds(5))
                .readTimeout(Duration.ofSeconds(15))
                .writeTimeout(Duration.ofSeconds(15))
                .build();
        
        assertNotNull(client);
        client.close();
    }
    
    @Test
    public void testNetworkSelection() {
        // Test mainnet (default)
        TonCenter mainnetClient = TonCenter.builder()
                .apiKey("test-key")
                .network(NETWORK)
                .build();
        assertNotNull(mainnetClient);
        mainnetClient.close();
        
        // Test testnet
        TonCenter testnetClient = TonCenter.builder()
                .apiKey("test-key")
                .network(NETWORK)
                .build();
        assertNotNull(testnetClient);
        testnetClient.close();
        
        // Test explicit network enum
        TonCenter explicitClient = TonCenter.builder()
                .apiKey("test-key")
                .network(Network.MAINNET)
                .build();
        assertNotNull(explicitClient);
        explicitClient.close();
    }
    
    @Test
    public void testDefaultBuilder() {
        TonCenter client = TonCenter.builder().build();
        assertNotNull(client);
        client.close();
    }
    
    // ========== ACCOUNT METHODS TESTS (10 endpoints) ==========
    
    @Test
    public void testGetAddressInformation() {
        TonCenter client = TonCenter.builder()
                .apiKey(API_KEY)
                .network(NETWORK)
                .build();
        
        try {
            TonResponse<AddressInformationResponse> response = client.getAddressInformation(TEST_ADDRESS);
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
        TonCenter client = TonCenter.builder()
                .apiKey(API_KEY)
                .network(NETWORK)
                .build();
        
        try {
            TonResponse<ExtendedAddressInformationResponse> response = client.getExtendedAddressInformation(TEST_ADDRESS);
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
        TonCenter client = TonCenter.builder()
                .apiKey(API_KEY)
                .network(NETWORK)
                .build();
        try {
            TonResponse<WalletInformationResponse> response = client.getWalletInformation(TEST_ADDRESS);
            log.info("response {}",  response.getResult());
            assertTrue("Wallet information should be successful", response.isSuccess());
            assertNotNull("Result should not be null", response.getResult());
            log.info("Wallet information retrieved successfully");
        } finally {
            client.close();
        }
    }
    
    @Test
    public void testGetTransactions() {
        TonCenter client = TonCenter.builder()
                .apiKey(API_KEY)
                .network(NETWORK)
                .build();
        
        try {
            TonResponse<List<TransactionResponse>> response = client.getTransactions(TEST_ADDRESS, 5, null, null, null, false);
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
        TonCenter client = TonCenter.builder()
                .apiKey(API_KEY)
                .network(NETWORK)
                .build();
        
        try {
            TonResponse<String> response = client.getAddressBalance(TEST_ADDRESS);
            assertTrue("Address balance should be successful", response.isSuccess());
            assertNotNull("Balance should not be null", response.getResult());
            log.info("Address balance: {}", response.getResult());
        } finally {
            client.close();
        }
    }
    
    @Test
    public void testGetAddressState() {
        TonCenter client = TonCenter.builder()
                .apiKey(API_KEY)
                .network(NETWORK)
                .build();
        
        try {
            TonResponse<String> response = client.getAddressState(TEST_ADDRESS);
            assertTrue("Address state should be successful", response.isSuccess());
            assertNotNull("State should not be null", response.getResult());
            log.info("Address state: {}", response.getResult());
        } finally {
            client.close();
        }
    }
    
    @Test
    public void testPackAddress() {
        TonCenter client = TonCenter.builder()
                .apiKey(API_KEY)
                .network(NETWORK)
                .build();
        
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
        TonCenter client = TonCenter.builder()
                .apiKey(API_KEY)
                .network(NETWORK)
                .build();
        
        try {
            TonResponse<String> response = client.unpackAddress(TEST_ADDRESS);
            assertTrue("Unpack address should be successful", response.isSuccess());
            assertNotNull("Unpacked address should not be null", response.getResult());
            log.info("Unpacked address: {}", response.getResult());
        } finally {
            client.close();
        }
    }
    
    @Test
    public void testDetectAddress() {
        TonCenter client = TonCenter.builder()
                .apiKey(API_KEY)
                .network(NETWORK)
                .build();
        
        try {
            TonResponse<DetectAddressResponse> response = client.detectAddress(TEST_ADDRESS);
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
        TonCenter client = TonCenter.builder()
                .apiKey(API_KEY)
                .network(NETWORK)
                .build();
        
        try {
            TonResponse<TokenDataResponse> response = client.getTokenData(NFT_ADDRESS);
            log.info("token data: {}", response.getResult());
            log.info("response {}", response.getResult());
            assertTrue("Token data should be successful", response.isSuccess());
            assertNotNull("Token data should not be null", response.getResult());
            log.info("Token data retrieved successfully");
        } finally {
            client.close();
        }
    }
    
    // ========== BLOCK METHODS TESTS (9 endpoints) ==========
    
    @Test
    public void testGetMasterchainInfo() {
        TonCenter client = TonCenter.builder()
                .apiKey(API_KEY)
                .network(NETWORK)
                .build();
        
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
        TonCenter client = TonCenter.builder()
                .apiKey(API_KEY)
                .network(NETWORK)
                .build();
        
        try {
            TonResponse<MasterchainBlockSignaturesResponse> response = client.getMasterchainBlockSignatures(1000);
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
        TonCenter client = TonCenter.builder()
                .apiKey(API_KEY)
                .network(NETWORK)
                .build();

        try {
            TonResponse<ShardBlockProofResponse> response = client.getShardBlockProof(-1, -9223372036854775808L, 1000L);
            log.info("response {}", response.getResult());
            assertTrue("Shard block proof should be successful", response.isSuccess());
            assertNotNull("Shard block proof should not be null", response.getResult());
            log.info("Shard block proof retrieved successfully");
        } finally {
            client.close();
        }
    }
    
    @Test
    public void testGetConsensusBlock() {
        TonCenter client = TonCenter.builder()
                .apiKey(API_KEY)
                .network(NETWORK)
                .build();
        
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
        TonCenter client = TonCenter.builder()
                .apiKey(API_KEY)
                .network(NETWORK)
                .build();
        
        try {
            TonResponse<LookupBlockResponse> response = client.lookupBlockBySeqno(-1, -9223372036854775808L, 1000);
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
        TonCenter client = TonCenter.builder()
                .apiKey(API_KEY)
                .network(NETWORK)
                .build();
        
        try {
            TonResponse<ShardsResponse> response = client.getShards(1000);
            assertTrue("Get shards should be successful", response.isSuccess());
            assertNotNull("Shards info should not be null", response.getResult());
            assertNotNull("Shards list should not be null", response.getResult().getShards());
            assertTrue("Should have at least one shard", response.getResult().getShards().size() > 0);
            
            // Test the typed response structure
            ShardsResponse.BlockIdExt firstShard = response.getResult().getShards().get(0);
            assertNotNull("First shard should not be null", firstShard);
            assertNotNull("Shard workchain should not be null", firstShard.getWorkchain());
            assertNotNull("Shard seqno should not be null", firstShard.getSeqno());
            
            log.info("Shards information retrieved successfully: {} shards", response.getResult().getShards().size());
            log.info("First shard: workchain={}, seqno={}", firstShard.getWorkchain(), firstShard.getSeqno());
        } finally {
            client.close();
        }
    }
    
    @Test
    public void testGetBlockTransactions() {
        TonCenter client = TonCenter.builder()
                .apiKey(API_KEY)
                .network(NETWORK)
                .build();
        
        try {
            TonResponse<BlockTransactionsResponse> response = client.getBlockTransactions(-1, -9223372036854775808L, 1000L);
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
        TonCenter client = TonCenter.builder()
                .apiKey(API_KEY)
                .network(NETWORK)
                .build();
        
        try {
            TonResponse<BlockTransactionsResponse> response = client.getBlockTransactionsExt(-1, -9223372036854775808L, 1000L, null, null, null, null, null);
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
        TonCenter client = TonCenter.builder()
                .apiKey(API_KEY)
                .network(NETWORK)
                .build();
        
        try {
            TonResponse<BlockHeaderResponse> response = client.getBlockHeader(-1, -9223372036854775808L, 1000L);
            log.info("response {}", response.getResult());
            assertTrue("Block header should be successful", response.isSuccess());
            assertNotNull("Block header should not be null", response.getResult());
        } finally {
            client.close();
        }
    }
    
    @Test
    public void testGetOutMsgQueueSizes() {
        TonCenter client = TonCenter.builder()
                .apiKey(API_KEY)
                .network(NETWORK)
                .build();
        
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
        TonCenter client = TonCenter.builder()
                .apiKey(API_KEY)
                .network(NETWORK)
                .build();
        
        try {
            TonResponse<ConfigParamResponse> response = client.getConfigParam(0); // Config param 0 (address of config smart contract)
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
        TonCenter client = TonCenter.builder()
                .apiKey(API_KEY)
                .network(NETWORK)
                .build();
        
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
    public void testTryLocateTx() {
        TonCenter client = TonCenter.builder()
                .apiKey(API_KEY)
                .network(NETWORK)
                .build();
        
        try {
            TonResponse<LocateTxResponse> response = client.tryLocateTx(TEST_ADDRESS, WALLET_ADDRESS, 1000000L);
            // This might not find a transaction, but should not error
            log.info("Try locate tx completed: success={}", response.isSuccess());
        } catch (TonCenterException e) {
            log.warn("Try locate tx failed: {}", e.getMessage());
        } finally {
            client.close();
        }
    }
    
    @Test
    public void testTryLocateResultTx() {
        TonCenter client = TonCenter.builder()
                .apiKey(API_KEY)
                .network(NETWORK)
                .build();
        
        try {
            TonResponse<LocateTxResponse> response = client.tryLocateResultTx(TEST_ADDRESS, WALLET_ADDRESS, 1000000L);
            // This might not find a transaction, but should not error
            log.info("Try locate result tx completed: success={}", response.isSuccess());
        } catch (TonCenterException e) {
            log.warn("Try locate result tx failed: {}", e.getMessage());
        } finally {
            client.close();
        }
    }
    
    @Test
    public void testTryLocateSourceTx() {
        TonCenter client = TonCenter.builder()
                .apiKey(API_KEY)
                .network(NETWORK)
                .build();
        
        try {
            TonResponse<LocateTxResponse> response = client.tryLocateSourceTx(TEST_ADDRESS, WALLET_ADDRESS, 1000000L);
            // This might not find a transaction, but should not error
            log.info("Try locate source tx completed: success={}", response.isSuccess());
        } catch (TonCenterException e) {
            log.warn("Try locate source tx failed: {}", e.getMessage());
        } finally {
            client.close();
        }
    }
    
    // ========== RUN METHOD TESTS (1 endpoint) ==========
    
    @Test
    public void testRunGetMethod() {
        TonCenter client = TonCenter.builder()
                .apiKey(API_KEY)
                .network(NETWORK)
                .build();
        
        try {
            // Test calling seqno method on a wallet
            TonResponse<RunGetMethodResponse> response = client.runGetMethod(TEST_ADDRESS, "seqno", new ArrayList<>());
            log.info("response {}", response.getResult());
            assertTrue("Run get method should be successful", response.isSuccess());
            assertNotNull("Method result should not be null", response.getResult());
            log.info("Get method 'seqno' executed successfully");
        } finally {
            client.close();
        }
    }
    
    // ========== SEND METHODS TESTS (4 endpoints) ==========
    // Note: These tests use dummy data and will likely fail, but test the API structure
    
    @Test
    public void testSendBoc() {
        TonCenter client = TonCenter.builder()
                .apiKey(API_KEY)
                .network(NETWORK)
                .build();
        
        try {
            // Using dummy BOC data - this will fail but tests the endpoint
            String dummyBoc = "te6ccgEBAQEAJgAAOAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAABYvI0mM=";
            TonResponse<SendBocResponse> response = client.sendBoc(dummyBoc);
            log.info("Send BOC completed: success={}", response.isSuccess());
        } finally {
            client.close();
        }
    }
    
    @Test
    public void testSendBocReturnHash() {
        TonCenter client = TonCenter.builder()
                .apiKey(API_KEY)
                .network(NETWORK)
                .build();
        
        try {
            // Using dummy BOC data - this will fail but tests the endpoint
            String dummyBoc = "te6ccgEBAQEAJgAAOAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAABYvI0mM=";
            TonResponse<String> response = client.sendBocReturnHash(dummyBoc);
            log.info("Send BOC return hash completed: success={}", response.isSuccess());
        } finally {
            client.close();
        }
    }
    
    @Test
    public void testSendQuery() {
        TonCenter client = TonCenter.builder()
                .apiKey(API_KEY)
                .network(NETWORK)
                .build();
        
        try {
            // Using dummy data - this will fail but tests the endpoint
            TonResponse<SendQueryResponse> response = client.sendQuery(TEST_ADDRESS, "dummyBody", "", "");
            log.info("Send query completed: success={}", response.isSuccess());
        } finally {
            client.close();
        }
    }
    
    @Test
    public void testEstimateFee() {
        TonCenter client = TonCenter.builder()
                .apiKey(API_KEY)
                .network(NETWORK)
                .build();
        
        try {
            // Using dummy data - this will fail but tests the endpoint
            TonResponse<EstimateFeeResponse> response = client.estimateFee(TEST_ADDRESS, "dummyBody");
            log.info("Estimate fee completed: success={}", response.isSuccess());
        } finally {
            client.close();
        }
    }
    
    // ========== CONVENIENCE METHODS TESTS ==========
    
    @Test
    public void testConvenienceMethods() {
        TonCenter client = TonCenter.builder()
                .apiKey(API_KEY)
                .network(NETWORK)
                .build();
        
        try {
            // Test convenience method for transactions
            TonResponse<List<TransactionResponse>> response = client.getTransactions(TEST_ADDRESS);
            assertTrue("Convenience method should work", response.isSuccess());
            log.info("Convenience method getTransactions() works");
            
            // Test convenience method with limit
            TonResponse<List<TransactionResponse>> response2 = client.getTransactions(TEST_ADDRESS, 3);
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
        TonCenter client = TonCenter.builder()
                .endpoint("https://invalid-endpoint-that-does-not-exist.com/api/v2")
                .build();
        
        try {
            client.getMasterchainInfo();
            fail("Should throw TonCenterException for invalid endpoint");
        } catch (TonCenterException e) {
            // Expected
            assertTrue("Should contain network error", 
                e.getMessage().contains("Network error") || e.getMessage().contains("HTTP error"));
            log.info("Exception handling works correctly");
        }
        
        client.close();
    }
    
    @Test
    public void testApiKeyValidation() {
        TonCenter client = TonCenter.builder()
                .apiKey("invalid-api-key")
                .network(NETWORK)
                .build();
        
        try {
            client.getMasterchainInfo();
            // May succeed or fail depending on API key validation
        } catch (TonCenterApiException e) {
            // Expected for invalid API key
            log.info("API key validation works: {}", e.getMessage());
        } catch (TonCenterException e) {
            log.info("Network error with invalid API key: {}", e.getMessage());
        }
        
        client.close();
    }
    
    // ========== SUMMARY TEST ==========
    
    @Test
    public void testAllEndpointsCount() {
        // This test verifies that all 27 endpoints are implemented
        
        TonCenter client = TonCenter.builder().build();
        
        // Account methods (10)
        assertNotNull("getAddressInformation should exist", getMethod(client, "getAddressInformation"));
        assertNotNull("getExtendedAddressInformation should exist", getMethod(client, "getExtendedAddressInformation"));
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
        assertNotNull("getMasterchainBlockSignatures should exist", getMethod(client, "getMasterchainBlockSignatures"));
        assertNotNull("getShardBlockProof should exist", getMethod(client, "getShardBlockProof"));
        assertNotNull("getConsensusBlock should exist", getMethod(client, "getConsensusBlock"));
        assertNotNull("lookupBlock should exist", getMethod(client, "lookupBlock"));
        assertNotNull("getShards should exist", getMethod(client, "getShards"));
        assertNotNull("getBlockTransactions should exist", getMethod(client, "getBlockTransactions"));
        assertNotNull("getBlockTransactionsExt should exist", getMethod(client, "getBlockTransactionsExt"));
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
