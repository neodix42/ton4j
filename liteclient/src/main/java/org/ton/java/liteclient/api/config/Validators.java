package org.ton.java.liteclient.api.config;

import lombok.Builder;
import lombok.Data;

import java.math.BigInteger;
import java.util.List;

@Builder
@Data
public class Validators {
  long since; // utime_since
  long until; // utime_until
  long total; // total
  long main; //
  BigInteger totalWeight; // total_weight:100
  List<Validator> validators;
}
