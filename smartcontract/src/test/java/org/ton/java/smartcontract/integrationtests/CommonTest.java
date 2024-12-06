package org.ton.java.smartcontract.integrationtests;

import lombok.extern.slf4j.Slf4j;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.ton.java.tonlib.Tonlib;

@Slf4j
@RunWith(JUnit4.class)
public class CommonTest {
  static Tonlib tonlib;

  static String tonlibPath =
      System.getProperty("user.dir") + "/../2.ton-test-artifacts/tonlibjson.dll";

  @BeforeClass
  public static void setUpBeforeClass() {
    tonlib =
        Tonlib.builder().testnet(true).pathToTonlibSharedLib(tonlibPath).ignoreCache(false).build();
  }
}
