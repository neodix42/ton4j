package org.ton.java.tonconnect;

import lombok.*;

@Builder
@Data
public class Domain {
  private int lengthBytes;
  private String value;
}
