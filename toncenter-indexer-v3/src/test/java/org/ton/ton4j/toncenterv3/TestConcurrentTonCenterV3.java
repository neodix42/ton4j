package org.ton.ton4j.toncenterv3;

import static org.junit.Assert.assertNotNull;
import static org.ton.ton4j.toncenterv3.TonCenterV3Test.TESTNET_API_KEY;
import static org.ton.ton4j.toncenterv3.TonCenterV3Test.TEST_ADDRESS;

import com.anarsoft.vmlens.concurrent.junit.ConcurrentTestRunner;
import com.anarsoft.vmlens.concurrent.junit.ThreadCount;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ton.ton4j.toncenterv3.model.ResponseModels;

@Slf4j
@RunWith(ConcurrentTestRunner.class)
public class TestConcurrentTonCenterV3 {

  @Test
  @ThreadCount(10)
  public void testTonCenter() {
    TonCenterV3 tonCenterV3 =
        TonCenterV3.builder().network(Network.TESTNET).apiKey(TESTNET_API_KEY).build();

    try {
      ResponseModels.V2AddressInformation response =
          tonCenterV3.getAddressInformation(TEST_ADDRESS);
      log.info("response {}", response);
      assertNotNull("Result should not be null", response);
      log.info("Wallet information retrieved successfully");
    } finally {
      tonCenterV3.close();
    }
  }
}
