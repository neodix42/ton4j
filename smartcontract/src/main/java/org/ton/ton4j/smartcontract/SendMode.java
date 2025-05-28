package org.ton.ton4j.smartcontract;

public enum SendMode {
  CARRY_ALL_REMAINING_BALANCE(128),
  CARRY_ALL_REMAINING_INCOMING_VALUE(64),
  DESTROY_ACCOUNT_IF_ZERO(32),
  PAY_GAS_SEPARATELY_AND_IGNORE_ERRORS(3),
  PAY_GAS_SEPARATELY(1),
  IGNORE_ERRORS(2);

  private final int value;

  SendMode(final int newValue) {
    value = newValue;
  }

  public int getValue() {
    return value;
  }

  public static SendMode valueOfInt(int value) {
    switch (value) {
      case 1:
        return PAY_GAS_SEPARATELY;
      case 2:
        return IGNORE_ERRORS;
      case 3:
        return PAY_GAS_SEPARATELY_AND_IGNORE_ERRORS;
      case 32:
        return DESTROY_ACCOUNT_IF_ZERO;
      case 64:
        return CARRY_ALL_REMAINING_INCOMING_VALUE;
      case 128:
        return CARRY_ALL_REMAINING_BALANCE;
    }
    throw new IllegalArgumentException();
  }

  public int getIndex() {
    return ordinal();
  }
}
