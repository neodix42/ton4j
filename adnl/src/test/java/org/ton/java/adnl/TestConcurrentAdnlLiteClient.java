package org.ton.java.adnl;

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

  private static AdnlLiteClient adnlLiteClient;

  static {
    try {
      adnlLiteClient =
          AdnlLiteClient.builder().configUrl(Utils.getGlobalConfigUrlMainnetGithub()).build();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @Test
  @ThreadCount(10)
  public void testAdnlLiteClientRunMethod() throws Exception {
    MasterchainInfo masterchainInfo = adnlLiteClient.getMasterchainInfo();
    System.out.println(masterchainInfo);
    BigInteger balance =
        adnlLiteClient.getBalance(
            Address.of("-1:3333333333333333333333333333333333333333333333333333333333333333"));
    System.out.printf(balance.toString());
  }
}
