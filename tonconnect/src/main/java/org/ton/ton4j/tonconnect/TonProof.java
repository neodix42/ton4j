package org.ton.ton4j.tonconnect;

import lombok.*;

@Builder
@Data
public class TonProof {
  private long timestamp;
  private Domain domain;
  private String signature; // Base64Url
  private String payload; // plain
}
