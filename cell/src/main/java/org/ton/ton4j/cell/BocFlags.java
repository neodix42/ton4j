package org.ton.ton4j.cell;

import java.io.Serializable;

public class BocFlags implements Serializable {
  boolean hasIndex;
  boolean hasCrc32c;
  boolean hasCacheBits;
  int cellNumSizeBytes;
}
