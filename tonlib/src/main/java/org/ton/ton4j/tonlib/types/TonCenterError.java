package org.ton.ton4j.tonlib.types;

import java.io.Serializable;
import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class TonCenterError implements Serializable {
  long code;
  String message;
}
