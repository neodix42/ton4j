package org.ton.java.emulator.tvm;

import java.io.Serializable;

public enum TvmVerbosityLevel implements Serializable {
  TRUNCATED,
  UNLIMITED,
  WITH_CELL_HASH_AND_OFFSET,
  WITH_ALL_STACK_VALUES
}
