package org.ton.java.tonlib.types;

import java.io.Serializable;
import java.math.BigInteger;
import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class ExtraCurrency implements Serializable {
  long id;
  BigInteger amount;
}
