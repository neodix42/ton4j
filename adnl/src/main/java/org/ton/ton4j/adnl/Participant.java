package org.ton.ton4j.adnl;

import java.io.Serializable;
import java.math.BigInteger;
import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class Participant implements Serializable {
  BigInteger address;
  BigInteger stake;
}
