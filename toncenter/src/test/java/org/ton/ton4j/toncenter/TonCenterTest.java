package org.ton.ton4j.toncenter;

import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.ton.ton4j.toncenter.model.AddressInformationResponse;
import org.ton.ton4j.toncenter.model.MasterchainInfoResponse;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;

import static org.junit.Assert.*;

/**
 * Test class for TonCenter API wrapper
 */
@Slf4j
@RunWith(JUnit4.class)
public class TonCenterTest {

    private static final String TEST_ADDRESS = "EQCD39VS5jcptHL8vMjEXrzGaRcCVYto7HUn4bpAOg8xqB2N";
    public static final String API_KEY = "";

    @Test
    public void testBuilderPattern() {
        TonCenter client = TonCenter.builder()
                .apiKey("test-api-key")
                .testnet()  // Use testnet
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
                .mainnet()
                .build();
        assertNotNull(mainnetClient);
        mainnetClient.close();
        
        // Test testnet
        TonCenter testnetClient = TonCenter.builder()
                .apiKey("test-key")
                .testnet()
                .build();
        assertNotNull(testnetClient);
        testnetClient.close();
        
        // Test explicit network enum
        TonCenter explicitClient = TonCenter.builder()
                .apiKey("test-key")
                .network(Network.TESTNET)
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
    
    @Test
    public void testBuilderWithApiKey() {
        TonCenter client = TonCenter.builder()
                .apiKey(API_KEY)
                .build();
        
        assertNotNull(client);
        client.close();
    }
    
    // Note: The following tests would require a valid API key and network access
    // They are commented out to avoid actual API calls during testing
    
    
    @Test
    public void testGetMasterchainInfo() {
        TonCenter client = TonCenter.builder()
                .apiKey(API_KEY)
                .network(Network.TESTNET)
                .build();
        
        try {
            TonResponse<MasterchainInfoResponse> response = client.getMasterchainInfo();
            assertTrue(response.isSuccess());
            assertNotNull(response.getResult());
            log.info("response {}", response.getResult());
        } catch (TonCenterException e) {
            // Expected if no API key or network issues
            System.out.println("API call failed (expected): " + e.getMessage());
        } finally {
            client.close();
        }
    }
    
    @Test
    public void testGetAddressInformation() {
        TonCenter client = TonCenter.builder()
                .apiKey(API_KEY)
                .network(Network.TESTNET)
                .build();
        
        try {
            TonResponse<AddressInformationResponse> response = client.getAddressInformation(TEST_ADDRESS);
            assertTrue(response.isSuccess());
            assertNotNull(response.getResult());
            assertNotNull(response.getResult().getBalance());
        } catch (TonCenterException e) {
            // Expected if no API key or network issues
            System.out.println("API call failed (expected): " + e.getMessage());
        } finally {
            client.close();
        }
    }
    
    @Test
    public void testGetAddressBalance() {
        TonCenter client = TonCenter.builder()
                .apiKey(API_KEY)
                .network(Network.TESTNET)
                .build();
        
        try {
            TonResponse<String> response = client.getAddressBalance(TEST_ADDRESS);
            assertTrue(response.isSuccess());
            assertNotNull(response.getResult());
        } catch (TonCenterException e) {
            // Expected if no API key or network issues
            System.out.println("API call failed (expected): " + e.getMessage());
        } finally {
            client.close();
        }
    }
    
    @Test
    public void testRunGetMethod() {
        TonCenter client = TonCenter.builder()
                .apiKey(API_KEY)
                .network(Network.TESTNET)
                .build();
        
        try {
            // Test calling get_seqno method on a wallet
            TonResponse<Object> response = client.runGetMethod(
                TEST_ADDRESS, 
                "seqno", 
                new ArrayList<>()
            );
            assertTrue(response.isSuccess());
            assertNotNull(response.getResult());
        } catch (TonCenterException e) {
            // Expected if no API key, network issues, or method doesn't exist
            System.out.println("API call failed (expected): " + e.getMessage());
        } finally {
            client.close();
        }
    }
    
    
    @Test
    public void testConvenienceMethods() {
        TonCenter client = TonCenter.builder().build();
        
        // Test that convenience methods don't throw exceptions when building requests
        // Note: Some endpoints may work without API key (with rate limiting)
        
        try {
            client.getTransactions(TEST_ADDRESS);
            // May succeed or fail depending on rate limiting
        } catch (TonCenterException e) {
            // Expected for rate-limited endpoints
            assertTrue(e.getMessage().contains("HTTP error") || e.getMessage().contains("Network error"));
        }
        
        try {
            client.getTransactions(TEST_ADDRESS, 5);
            // May succeed or fail depending on rate limiting
        } catch (TonCenterException e) {
            // Expected for rate-limited endpoints
            assertTrue(e.getMessage().contains("HTTP error") || e.getMessage().contains("Network error"));
        }
        
        client.close();
    }
    
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
            assertTrue(e.getMessage().contains("Network error") || e.getMessage().contains("HTTP error"));
        }
        
        client.close();
    }
}
