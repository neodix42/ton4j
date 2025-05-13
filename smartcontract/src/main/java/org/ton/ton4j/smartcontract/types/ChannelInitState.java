package org.ton.ton4j.smartcontract.types;

import lombok.Builder;
import lombok.Getter;

import java.math.BigInteger;

@Builder
@Getter
public class ChannelInitState {
    BigInteger balanceA;
    BigInteger balanceB;
    BigInteger seqnoA;
    BigInteger seqnoB;
}
