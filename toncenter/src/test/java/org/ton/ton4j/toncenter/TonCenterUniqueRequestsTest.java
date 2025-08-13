package org.ton.ton4j.toncenter;

import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.ton.ton4j.toncenter.model.MasterchainInfoResponse;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Test for the uniqueRequests feature in TonCenter.
 * This test demonstrates how to use the uniqueRequests feature
 * which adds a random parameter to each request to prevent caching.
 */
@Slf4j
@RunWith(JUnit4.class)
public class TonCenterUniqueRequestsTest {

    @Test
    public void testUniqueRequests() {
        // Create a TonCenter instance with uniqueRequests enabled
        TonCenter tonCenter = TonCenter.builder()
                .testnet()
                .debug()
                .uniqueRequests() // Enable unique requests
                .build();

        // Make a request - this will include a random t= parameter
        TonResponse<MasterchainInfoResponse> response = tonCenter.getMasterchainInfo();
        
        // Verify the response is successful
        assertTrue("Response should be successful", response.isSuccess());
        assertNotNull("Response result should not be null", response.getResult());
        
        // Note: In a real test, you might want to use a mock HTTP client to verify
        // that the URL actually contains the t= parameter with a random UUID
    }
    
    @Test
    public void testWithoutUniqueRequests() {
        // Create a TonCenter instance without uniqueRequests
        TonCenter tonCenter = TonCenter.builder()
                .testnet()
                .debug()
                // uniqueRequests() is not called
                .build();

        // Make a request - this will NOT include a random t= parameter
        TonResponse<MasterchainInfoResponse> response = tonCenter.getMasterchainInfo();
        
        // Verify the response is successful
        assertTrue("Response should be successful", response.isSuccess());
        assertNotNull("Response result should not be null", response.getResult());
    }
}
