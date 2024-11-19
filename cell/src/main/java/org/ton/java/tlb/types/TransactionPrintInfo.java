package org.ton.java.tlb.types;

import lombok.Builder;
import lombok.Data;

import java.math.BigInteger;

@Builder
@Data
public class TransactionPrintInfo {
    String hash; // in_msg.hash
    long now;
    String op;
    String type;
    BigInteger valueIn;
    BigInteger valueOut;

    BigInteger totalFees;
    BigInteger storageFeesCollected;
    BigInteger storageDueFees;
    String storageStatus;
    String computeSuccess;
    BigInteger computeGasFees;
    BigInteger computeGasUsed;
    long computeVmSteps;
    BigInteger computeExitCode;
    String actionSuccess;
    BigInteger actionTotalFwdFees;
    BigInteger actionTotalActionFees;
    long actionTotalActions;
    long actionResultCode;
    BigInteger inForwardFee;
    BigInteger outForwardFee;
    long exitCode;
    long actionCode;
    long outMsgs;
    BigInteger lt;
    String account;
    BigInteger balance;
    Transaction tx;
}
