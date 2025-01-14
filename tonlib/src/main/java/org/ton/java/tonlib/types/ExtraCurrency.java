package org.ton.java.tonlib.types;

import java.io.Serializable;
import java.math.BigInteger;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Builder
@Setter
@Getter
@ToString
public class ExtraCurrency implements Serializable {
  long id;
  BigInteger amount;
}
