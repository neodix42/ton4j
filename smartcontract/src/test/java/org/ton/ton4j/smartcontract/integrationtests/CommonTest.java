package org.ton.ton4j.smartcontract.integrationtests;

import lombok.extern.slf4j.Slf4j;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.ton.ton4j.tonlib.Tonlib;
import org.ton.ton4j.utils.Utils;

@Slf4j
@RunWith(JUnit4.class)
public class CommonTest {
  static Tonlib tonlib;

  public static final String TESTNET_API_KEY =
          "188b29e2b477d8bb95af5041f75c57b62653add1170634f148ac71d7751d0c71";

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
