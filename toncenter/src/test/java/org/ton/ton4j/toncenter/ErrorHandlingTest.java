package org.ton.ton4j.toncenter;

import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import static org.junit.Assert.*;

/**
 * Test class for error handling scenarios
 */
@Slf4j
@RunWith(JUnit4.class)
public class ErrorHandlingTest {

    private static final String TEST_ADDRESS = "EQCD39VS5jcptHL8vMjEXrzGaRcCVYto7HUn4bpAOg8xqB2N";
    private static final String INVALID_API_KEY = "asdf";
    
    @Test
    public void testInvalidApiKeyErrorMessage() {
        TonCenter client = TonCenter.builder()
                .apiKey(INVALID_API_KEY)
                .testnet()
                .build();
        
        try {
            client.getAddressInformation(TEST_ADDRESS);
            fail("Should throw TonCenterApiException for invalid API key");
        } catch (TonCenterApiException e) {
            // Expected - should contain detailed error message
            log.info("TonCenterApiException caught: {}", e.getMessage());
            log.info("Error code: {}", e.getErrorCode());
            assertTrue("Should contain API key error message", 
                e.getMessage().contains("API key does not exist") || 
                e.getMessage().contains("API key"));
            assertEquals("Should have 401 error code", Integer.valueOf(401), e.getErrorCode());
        } catch (TonCenterException e) {
            // Fallback case - should at least contain response body
            log.info("TonCenterException caught: {}", e.getMessage());
            assertTrue("Should contain error details", 
                e.getMessage().contains("401") || 
                e.getMessage().contains("API key"));
        } finally {
            client.close();
        }
    }
    
    @Test
    public void testMainnetWithInvalidApiKey() {
        TonCenter client = TonCenter.builder()
                .apiKey(INVALID_API_KEY)
                .mainnet()  // Test mainnet endpoint too
                .build();
        
        try {
            client.getMasterchainInfo();
            fail("Should throw exception for invalid API key");
        } catch (TonCenterApiException e) {
            log.info("Mainnet TonCenterApiException: {}", e.getMessage());
            assertTrue("Should contain API key error", 
                e.getMessage().contains("API key") || 
                e.getMessage().contains("401"));
        } catch (TonCenterException e) {
            log.info("Mainnet TonCenterException: {}", e.getMessage());
            assertTrue("Should contain error details", 
                e.getMessage().contains("401") || 
                e.getMessage().contains("API key"));
        } finally {
            client.close();
        }
    }
}
