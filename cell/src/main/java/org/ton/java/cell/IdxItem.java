package org.ton.java.cell;

import lombok.Builder;
import lombok.Data;

import java.io.Serializable;
import java.math.BigInteger;

@Builder
@Data
public class IdxItem implements Serializable {
  BigInteger index;
  long dataIndex;
  long repeats;
  boolean withHash;
  Cell cell;
}
