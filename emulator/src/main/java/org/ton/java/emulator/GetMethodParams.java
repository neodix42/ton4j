package org.ton.java.emulator;

import lombok.Builder;import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.ton.java.address.Address;
import org.ton.java.cell.Cell;

import java.io.Serializable;
import java.math.BigInteger;

@Builder
@Setter
@Getter
@ToString
public class GetMethodParams implements Serializable {
    Cell code;
    Cell data;
    TvmVerbosityLevel verbosityLevel;
    Cell libs;
    Address address;
    long unixTime;
    BigInteger balance;
    String randSeed;
    long gasLimit;
    long methodId;
    boolean debugEnabled;
}

