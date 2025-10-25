package org.ton.ton4j.toncenter;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.anarsoft.vmlens.concurrent.junit.ConcurrentTestRunner;
import com.anarsoft.vmlens.concurrent.junit.ThreadCount;
import java.math.BigInteger;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ton.ton4j.toncenter.model.TransactionResponse;
import org.ton.ton4j.toncenter.model.WalletInformationResponse;
import org.ton.ton4j.utils.Utils;

@Slf4j
@RunWith(ConcurrentTestRunner.class)
public class TestConcurrentTonCenterMyLocalTon {

  private static final String VALIDATOR0_ADDRESS =
      "-1:6744e92c6f71c776fbbcef299e31bf76f39c245cd56f2075b89c6a22026b4131";

  @Test
  @ThreadCount(20)
  public void testTonCenter() {
    TonCenter tonCenter = TonCenter.builder().mylocalton().debug().build();

    for (int i = 0; i < 20; i++) {
      try {
        // request 1
        TonResponse<WalletInformationResponse> response =
            tonCenter.getWalletInformation(VALIDATOR0_ADDRESS);

        assertTrue("Wallet information should be successful", response.isSuccess());
        assertNotNull("Result should not be null", response.getResult());
        log.info("seqno {}", response.getResult().getSeqno());
        Utils.sleepMs(Utils.getRandomInt() % 1000);

        // request 2
        BigInteger balance = tonCenter.getBalance(VALIDATOR0_ADDRESS);
        assertNotNull("Result should not be null", balance);
        log.info("balance {}", balance);
        Utils.sleepMs(Utils.getRandomInt() % 500);

        // request 3
        List<TransactionResponse> list = tonCenter.getTransactions(VALIDATOR0_ADDRESS).getResult();
        log.info("txs size {}", list.size());
      } finally {
        tonCenter.close();
      }
    }
  }
}
