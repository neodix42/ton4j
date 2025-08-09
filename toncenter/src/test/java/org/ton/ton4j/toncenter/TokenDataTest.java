package org.ton.ton4j.toncenter;

import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.ton.ton4j.toncenter.model.TokenDataResponse;

import static org.junit.Assert.*;

/** Specific test for TokenData functionality */
@Slf4j
public class TokenDataTest {

  private static final String TESTNET_API_KEY =
      "188b29e2b477d8bb95af5041f75c57b62653add1170634f148ac71d7751d0c71";
  private static final String TESTNET_JETTON_ADDRESS =
      "kQBpqkbPrhSjleAQ8W9TJpZBj6K3GKijCH-Uz_6H7UnaqVTI";

  @Test
  public void testGetTokenDataWithJettonMaster() {
    TonCenter client = TonCenter.builder().apiKey(TESTNET_API_KEY).network(Network.TESTNET).build();

    try {
      TonResponse<TokenDataResponse> response = client.getTokenData(TESTNET_JETTON_ADDRESS);

      log.info("Response success: {}", response.isSuccess());
      log.info("Token data: {}", response.getResult());

      assertTrue("Token data should be successful", response.isSuccess());
      assertNotNull("Token data should not be null", response.getResult());

      TokenDataResponse tokenData = response.getResult();

      // Verify basic fields
      assertNotNull("Address should not be null", tokenData.getAddress());
      assertEquals(
          "Contract type should be jetton_master", "jetton_master", tokenData.getContractType());
      assertNotNull("Total supply should not be null", tokenData.getTotalSupply());
      assertNotNull("Mintable should not be null", tokenData.getMintable());
      assertNotNull("Admin address should not be null", tokenData.getAdminAddress());
      assertNotNull("Jetton wallet code should not be null", tokenData.getJettonWalletCode());

      // Verify jetton content
      assertNotNull("Jetton content should not be null", tokenData.getJettonContent());
      assertEquals(
          "Jetton content type should be onchain",
          "onchain",
          tokenData.getJettonContent().getType());
      assertNotNull(
          "Jetton content data should not be null", tokenData.getJettonContent().getData());

      // Verify jetton content data fields
      TokenDataResponse.JettonContentData contentData = tokenData.getJettonContent().getData();
      assertNotNull("Name should not be null", contentData.getName());
      assertNotNull("Symbol should not be null", contentData.getSymbol());
      assertNotNull("Decimals should not be null", contentData.getDecimals());
      assertNotNull("Description should not be null", contentData.getDescription());
      assertNotNull("Image should not be null", contentData.getImage());

      // Log the actual values
      log.info("Address: {}", tokenData.getAddress());
      log.info("Contract type: {}", tokenData.getContractType());
      log.info("Total supply: {}", tokenData.getTotalSupply());
      log.info("Mintable: {}", tokenData.getMintable());
      log.info("Admin address: {}", tokenData.getAdminAddress());
      log.info("Jetton content type: {}", tokenData.getJettonContent().getType());
      log.info("Token name: {}", contentData.getName());
      log.info("Token symbol: {}", contentData.getSymbol());
      log.info("Token decimals: {}", contentData.getDecimals());
      log.info("Token description: {}", contentData.getDescription());
      log.info("Token image: {}", contentData.getImage());

      // Verify expected values based on the API response
      assertEquals("MANGO JETTON", contentData.getName());
      assertEquals("MANGO", contentData.getSymbol());
      assertEquals("9", contentData.getDecimals());
      assertEquals("tasty mango.", contentData.getDescription());
      assertEquals(
          "https://freepngimg.com/save/14753-mango-png-pic/800x709", contentData.getImage());

    } finally {
      client.close();
    }
  }
}
