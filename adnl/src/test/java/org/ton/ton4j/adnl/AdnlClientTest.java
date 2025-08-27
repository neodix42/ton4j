package org.ton.ton4j.adnl;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import java.util.Base64;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

@Slf4j
public class AdnlClientTest {

  @Test
  void testAdnlClient() throws Exception {
    byte[] serverPublicKey =
        Base64.getDecoder().decode("n4VDnSCUuSpjnCyUk9e3QOOd6o0ItSWYbTnW3Wnn8wk=");

    AdnlTcpTransport adnlTcpTransport = new AdnlTcpTransport();
    adnlTcpTransport.connect("5.9.10.47", 19949, serverPublicKey);
    assertThat(adnlTcpTransport.isConnected()).isTrue();
    adnlTcpTransport.close();
  }
}
