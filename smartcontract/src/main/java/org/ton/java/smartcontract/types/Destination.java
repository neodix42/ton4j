package org.ton.java.smartcontract.types;

import java.math.BigInteger;
import java.util.List;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;
import org.ton.java.cell.Cell;
import org.ton.java.tonlib.types.ExtraCurrency;

@Builder
@Getter
@ToString
public class Destination {
    boolean bounce; // default true
    int mode; // default mode 3
    String address;
    BigInteger amount;
    List<ExtraCurrency> extraCurrencies;
    String comment;
    Cell body;
}
