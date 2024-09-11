package org.ton.java.smartcontract.wallet.v4;

import lombok.Builder;
import lombok.Data;
import org.ton.java.address.Address;

import java.math.BigInteger;

@Builder
@Data
public class SubscriptionInfo {
    Address walletAddress;
    Address beneficiary;
    BigInteger subscriptionFee;
    long period;
    long startTime;
    long timeOut;
    long lastPaymentTime;
    long lastRequestTime;
    boolean isPaid;
    boolean isPaymentReady;
    long failedAttempts;
    long subscriptionId;
}
