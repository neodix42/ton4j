package org.ton.ton4j.smartcontract.unittests;

import static org.assertj.core.api.Assertions.assertThat;

import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.ton.ton4j.smartcontract.SendMode;

@Slf4j
@RunWith(JUnit4.class)
public class TestSendMode {

  @Test
  public void testSendMode() {
    assertThat(SendMode.PAY_GAS_SEPARATELY_AND_IGNORE_ERRORS.getValue()).isEqualTo(3);
    assertThat(SendMode.IGNORE_ERRORS.getValue()).isEqualTo(2);
    assertThat(SendMode.PAY_GAS_SEPARATELY.getValue()).isEqualTo(1);
  }
}
