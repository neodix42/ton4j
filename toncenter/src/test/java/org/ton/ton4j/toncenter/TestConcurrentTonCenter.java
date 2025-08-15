package org.ton.ton4j.toncenter;

import com.anarsoft.vmlens.concurrent.junit.ConcurrentTestRunner;
import com.anarsoft.vmlens.concurrent.junit.ThreadCount;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ton.ton4j.toncenter.model.WalletInformationResponse;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@Slf4j
@RunWith(ConcurrentTestRunner.class)
public class TestConcurrentTonCenter {

  @Test
  @ThreadCount(10)
  public void testTonCenter() {
    TonCenter tonCenter =
        TonCenter.builder()
            .network(Network.TESTNET)
            .apiKey(TonCenterTest.TESTNET_API_KEY)
            .build();

    try {
      TonResponse<WalletInformationResponse> response =
              tonCenter.getWalletInformation(TonCenterTest.TESTNET_WALLET);
      log.info("response {}", response.getResult());
      assertTrue("Wallet information should be successful", response.isSuccess());
      assertNotNull("Result should not be null", response.getResult());
      log.info("Wallet information retrieved successfully");
    } finally {
      tonCenter.close();
    }
  }
}
