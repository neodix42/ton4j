package org.ton.java.smartcontract.types;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;
import org.ton.java.address.Address;

import java.math.BigInteger;

@Builder
@Getter
@ToString
public class Destination {
    int mode; // default mode 3
    Address address;
    BigInteger amount;
    String comment;
}
