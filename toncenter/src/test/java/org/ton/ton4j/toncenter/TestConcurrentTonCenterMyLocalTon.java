package org.ton.ton4j.toncenter;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.anarsoft.vmlens.concurrent.junit.ConcurrentTestRunner;
import com.anarsoft.vmlens.concurrent.junit.ThreadCount;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ton.ton4j.toncenter.model.WalletInformationResponse;
import org.ton.ton4j.utils.Utils;

@Slf4j
@RunWith(ConcurrentTestRunner.class)
public class TestConcurrentTonCenterMyLocalTon {

  @Test
  @ThreadCount(10)
  public void testTonCenter() {
    TonCenter tonCenter = TonCenter.builder().mylocalton().build();

    for (int i = 0; i < 10; i++) {
      try {
        TonResponse<WalletInformationResponse> response =
            tonCenter.getWalletInformation(
                "-1:6744e92c6f71c776fbbcef299e31bf76f39c245cd56f2075b89c6a22026b4131");
        log.info("response {}", response.getResult());
        assertTrue("Wallet information should be successful", response.isSuccess());
        assertNotNull("Result should not be null", response.getResult());
        log.info("Wallet information retrieved successfully");
      } finally {
        tonCenter.close();
      }
      Utils.sleep(Utils.getRandomInt() % 3);
    }
  }
}
