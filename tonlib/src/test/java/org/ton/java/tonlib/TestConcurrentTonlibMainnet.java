package org.ton.java.tonlib;

import static org.ton.java.tonlib.TestTonlibJson.ELECTOR_ADDRESSS;

import com.anarsoft.vmlens.concurrent.junit.ConcurrentTestRunner;
import com.anarsoft.vmlens.concurrent.junit.ThreadCount;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ton.java.address.Address;
import org.ton.java.tonlib.types.FullAccountState;
import org.ton.java.tonlib.types.MasterChainInfo;
import org.ton.java.tonlib.types.RunResult;
import org.ton.java.utils.Utils;

@Slf4j
@RunWith(ConcurrentTestRunner.class)
public class TestConcurrentTonlibMainnet {

  static String tonlibPath = Utils.getTonlibGithubUrl();
  //  String tonlibPath = "tonlibjson-prev.dll";

  Tonlib tonlib = Tonlib.builder().pathToTonlibSharedLib(tonlibPath).build();

  @Test
  @ThreadCount(10)
  public void testTonlibDownloadGlobalConfig() throws InterruptedException {
    log.info("tonlib instance {}", tonlib);
    MasterChainInfo last = tonlib.getLast();
    log.info("last: {}", last);
    Thread.sleep(100);

    FullAccountState accountState = tonlib.getAccountState(Address.of(ELECTOR_ADDRESSS));
    log.info("account {}", accountState.getBalance());

    Address address = Address.of("Ef8-sf_0CQDgwW6kNuNY8mUvRW-MGQ34Evffj8O0Z9Ly1tZ4");
    RunResult result = tonlib.runMethod(address, "seqno");
    log.info("res {}", result);
  }
}
