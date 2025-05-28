package org.ton.ton4j.emulator.tvm;

import java.io.Serializable;

public enum TvmVerbosityLevel implements Serializable {
  TRUNCATED,
  UNLIMITED,
  WITH_EXEC_LOCATION,
  WITH_GAS_REMAINING,
  DUMP_STACK,
  DUMP_STACK_VERBOSE_C5
}
