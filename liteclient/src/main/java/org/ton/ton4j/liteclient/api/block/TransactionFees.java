package org.ton.ton4j.liteclient.api.block;

import lombok.Builder;
import lombok.Data;

import java.math.BigInteger;

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
    boolean aborted;
    String block;
    String hash;
}