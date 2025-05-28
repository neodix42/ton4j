package org.ton.ton4j.tonlib.types;

import lombok.Builder;
import lombok.Data;

import java.io.Serializable;
import java.math.BigInteger;
import java.util.List;

@Builder
@Data
public class RunResult implements Serializable {
  List<Object> stack;
  BigInteger gas_used;
  long exit_code;
}
