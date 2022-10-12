package org.ton.java.smartcontract.wallet.v4;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.ton.java.address.Address;

import java.math.BigInteger;

@Builder
@Getter
@Setter
@ToString
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
    boolean isPaymentready;
    long failedAttempts;
    long subscriptionId;
}
