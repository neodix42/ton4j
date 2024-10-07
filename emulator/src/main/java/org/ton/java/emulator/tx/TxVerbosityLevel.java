package org.ton.java.emulator.tx;

import java.io.Serializable;

public enum TxVerbosityLevel implements Serializable {
  TRUNCATED,
  UNLIMITED,
  WITH_CELL_HASH_AND_OFFSET,
  WITH_ALL_STACK_VALUES
}
