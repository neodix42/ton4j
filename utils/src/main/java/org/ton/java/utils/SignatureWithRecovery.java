package org.ton.java.utils;

import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class SignatureWithRecovery {
  private byte[] r;
  private byte[] s;
  private byte[] v;

  public byte[] getSignature() {

    return Utils.concatBytes(r, s);
  }
}
