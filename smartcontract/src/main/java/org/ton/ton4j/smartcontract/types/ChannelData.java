package org.ton.ton4j.smartcontract.types;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;
import org.ton.ton4j.address.Address;
import org.ton.ton4j.cell.Cell;

import java.math.BigInteger;

@Builder
@Getter
@ToString
public class ChannelData {
    long state;
    BigInteger balanceA;
    BigInteger balanceB;
    byte[] publicKeyA;
    byte[] publicKeyB;
    BigInteger channelId;
    long quarantineDuration;
    BigInteger misbehaviorFine;
    long conditionalCloseDuration;
    BigInteger seqnoA;
    BigInteger seqnoB;
    Cell quarantine;
    BigInteger excessFee;
    Address addressA;
    Address addressB;
}
