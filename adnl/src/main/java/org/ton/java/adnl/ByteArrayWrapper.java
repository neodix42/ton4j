package org.ton.java.adnl;

import lombok.Getter;

import java.util.Arrays;

/** Wrapper for byte arrays to use as keys in maps */
@Getter
public class ByteArrayWrapper {
  private final byte[] data;

  public ByteArrayWrapper(byte[] data) {
    this.data = data;
  }

  @Override
  public boolean equals(Object other) {
    if (!(other instanceof ByteArrayWrapper)) {
      return false;
    }
    return Arrays.equals(data, ((ByteArrayWrapper) other).data);
  }

  @Override
  public int hashCode() {
    return Arrays.hashCode(data);
  }
}
