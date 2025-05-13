package org.ton.ton4j.tonconnect;

import lombok.*;

@Builder
@Data
public class Domain {
  private int lengthBytes;
  private String value;
}
