package org.ton.ton4j.smartcontract.payments;

import lombok.Builder;
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
