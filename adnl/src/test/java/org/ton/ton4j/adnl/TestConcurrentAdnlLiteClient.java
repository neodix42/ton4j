package org.ton.ton4j.adnl;

import com.anarsoft.vmlens.concurrent.junit.ConcurrentTestRunner;
import com.anarsoft.vmlens.concurrent.junit.ThreadCount;
import java.math.BigInteger;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ton.ton4j.address.Address;
import org.ton.ton4j.tl.liteserver.responses.MasterchainInfo;
import org.ton.ton4j.utils.Utils;

@Slf4j
@RunWith(ConcurrentTestRunner.class)
public class TestConcurrentAdnlLiteClient {

  public static final String TESTNET_ADDRESS = "0QAyni3YDAhs7c-7imWvPyEbMEeVPMX8eWDLQ5GUe-B-Bl9Z";

  AdnlLiteClient adnlLiteClient =
      AdnlLiteClient.builder().configUrl(Utils.getGlobalConfigUrlTestnetGithub()).build();

  public TestConcurrentAdnlLiteClient() throws Exception {}

  @Test
  @ThreadCount(20)
  public void testAdnlLiteClientRunMethod() throws Exception {
    Utils.sleepMs(Utils.getRandomInt() % 5000);

    MasterchainInfo masterchainInfo = adnlLiteClient.getMasterchainInfo();
    BigInteger balance = adnlLiteClient.getBalance(Address.of(TESTNET_ADDRESS));
    System.out.println(
        Thread.currentThread().getName()
            + " - "
            + masterchainInfo.getLast().getSeqno()
            + " - "
            + balance);
  }
}
