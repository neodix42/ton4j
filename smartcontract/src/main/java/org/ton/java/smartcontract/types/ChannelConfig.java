package org.ton.java.smartcontract.types;

import lombok.Builder;
import lombok.Getter;
import org.ton.java.address.Address;

import java.math.BigInteger;

@Builder
@Getter
public class ChannelConfig {
    BigInteger channelId;
    Address addressA;
    Address addressB;
    BigInteger initBalanceA;
    BigInteger initBalanceB;
}
