package org.ton.java.tonlib;

import static org.ton.java.tonlib.TestTonlibJson.ELECTOR_ADDRESSS;

import com.anarsoft.vmlens.concurrent.junit.ConcurrentTestRunner;
import com.anarsoft.vmlens.concurrent.junit.ThreadCount;
import java.util.ArrayDeque;
import java.util.Deque;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ton.java.address.Address;
import org.ton.java.tonlib.types.FullAccountState;
import org.ton.java.tonlib.types.MasterChainInfo;
import org.ton.java.utils.Utils;

@Slf4j
@RunWith(ConcurrentTestRunner.class)
public class TestConcurrentTonlibTestnet {

  String tonlibPath = Utils.getTonlibGithubUrl();
  //  static String tonlibPath = "tonlibjson-prev.dll";

  private Tonlib tonlib = Tonlib.builder().testnet(true).pathToTonlibSharedLib(tonlibPath).build();

  @Test
  @ThreadCount(10)
  public void testTonlibRunMethod() throws InterruptedException {
    log.info("tonlib instance {}", tonlib);
    MasterChainInfo last = tonlib.getLast();
    log.info("last: {}", last);
    Thread.sleep(100);

    FullAccountState accountState =
        tonlib.getAccountState(Address.of("EQCwHyzOrKP1lBHbvMrFHChifc1TLgeJVpKgHpL9sluHU-gV"));
    log.info("account {}", accountState.getBalance());

    Address elector = Address.of(ELECTOR_ADDRESSS);
    Deque<String> stack = new ArrayDeque<>();
    Address address = Address.of("EQCwHyzOrKP1lBHbvMrFHChifc1TLgeJVpKgHpL9sluHU-gV");
    stack.offer("[num, " + address.toDecimal() + "]");

    log.info("seqno {}", tonlib.getSeqno(address));
  }
}
