package org.ton.ton4j.emulator.tx;

import java.io.Serializable;

public enum TxVerbosityLevel implements Serializable {
  TRUNCATED,
  UNLIMITED,
  WITH_EXEC_LOCATION,
  WITH_GAS_REMAINING,
  DUMP_STACK,
  DUMP_STACK_VERBOSE_C5
}
