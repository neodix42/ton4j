package org.ton.java.smartcontract.types;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;
import org.ton.java.cell.Cell;

import java.math.BigInteger;

@Builder
@Getter
@ToString
public class Destination {
    boolean bounce; // default true
    int mode; // default mode 3
    String address;
    BigInteger amount;
    String comment;
    Cell body;
}
