package org.ton.ton4j.smartcontract.types;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import java.math.BigInteger;

@Builder
@Getter
@ToString
public class ChannelState {
    BigInteger balanceA;
    BigInteger balanceB;
    BigInteger seqnoA;
    BigInteger seqnoB;
}
