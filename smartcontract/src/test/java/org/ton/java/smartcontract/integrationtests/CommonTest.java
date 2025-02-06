package org.ton.java.smartcontract.integrationtests;

import lombok.extern.slf4j.Slf4j;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.ton.java.tonlib.Tonlib;
import org.ton.java.utils.Utils;

@Slf4j
@RunWith(JUnit4.class)
public class CommonTest {
  static Tonlib tonlib;

  @BeforeClass
  public static void setUpBeforeClass() {
    tonlib =
        Tonlib.builder()
            .testnet(true)
            .pathToTonlibSharedLib(Utils.getTonlibGithubUrl())
            .ignoreCache(false)
            .build();
  }
}
