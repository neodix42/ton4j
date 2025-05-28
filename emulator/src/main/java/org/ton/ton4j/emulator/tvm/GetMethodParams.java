package org.ton.ton4j.emulator.tvm;

import java.io.Serializable;
import java.math.BigInteger;
import lombok.*;
import org.ton.ton4j.address.Address;
import org.ton.ton4j.cell.Cell;

@Builder
@Data
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
