package org.ton.ton4j.tonlib.types;

import lombok.Builder;
import lombok.Data;

import java.io.Serializable;

@Builder
@Data
public class ShortTxId implements Serializable {
  long mode;
  String account; // base64
  long lt;
  String hash;
}
