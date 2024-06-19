package org.ton.java.emulator;

import java.io.Serializable;

public enum TvmVerbosityLevel implements Serializable {
    TRUNCATED, UNLIMITED, WITH_CELL_HASH_AND_OFFSET, WITH_ALL_STACK_VALUES
}
