package org.ton.java.smartcontract.payments;

import lombok.Builder;import lombok.Data;
import lombok.Getter;
import lombok.ToString;

import java.math.BigInteger;

@Builder
@Getter
@ToString
public class ClosingConfig {
    long quarantineDuration;
    BigInteger misbehaviorFine;
    long conditionalCloseDuration;
}
