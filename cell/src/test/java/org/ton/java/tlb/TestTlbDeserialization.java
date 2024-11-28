package org.ton.java.tlb;

import static org.assertj.core.api.Assertions.assertThat;

import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.ton.java.tlb.types.MsgAddressIntStd;

@Slf4j
@RunWith(JUnit4.class)
public class TestTlbDeserialization {

  @Test
  public void testSaveAddressWithZeros() {
    MsgAddressIntStd addr1 =
        MsgAddressIntStd.of("0:000212646ce2585c73ad02a115cd6e5c8485dda6cccd62e1b05385ff121e5a7c");
    MsgAddressIntStd addr2 =
        MsgAddressIntStd.of("0QAAAhJkbOJYXHOtAqEVzW5chIXdpszNYuGwU4X_Eh5afLsu");
    log.info("addr1 {}", addr1);
    log.info("addr2 {}", addr2);

    log.info("addr1 toAddress {}", addr1.toAddress());
    log.info("addr2 toAddress {}", addr2.toAddress());

    assertThat(addr1.toString())
        .isEqualTo("0:000212646ce2585c73ad02a115cd6e5c8485dda6cccd62e1b05385ff121e5a7c");
    assertThat(addr2.toString())
        .isEqualTo("0:000212646ce2585c73ad02a115cd6e5c8485dda6cccd62e1b05385ff121e5a7c");
  }
}
