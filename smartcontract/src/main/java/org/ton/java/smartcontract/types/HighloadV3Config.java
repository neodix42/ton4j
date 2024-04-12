package org.ton.java.smartcontract.types;

import java.math.BigInteger;

import org.ton.java.address.Address;
import org.ton.java.cell.Cell;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Builder
@Getter
@Setter
@ToString
public class HighloadV3Config {
    byte mode;
    int queryId;
    long createdAt;
    Address destination;
    BigInteger amount;
    Cell body;
}
