package org.ton.java.tlb.types;

import java.math.BigInteger;
import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class TransactionFees {
  String op;
  String type;
  BigInteger totalFees;
  BigInteger computeFee;
  BigInteger inForwardFee;
  BigInteger outForwardFee;
  BigInteger valueIn;
  BigInteger valueOut;
  long exitCode;
  long actionCode;
  long totalActions;
  long outMsgs;
  long now;
  BigInteger lt;
  String account;
  BigInteger balance;
}
