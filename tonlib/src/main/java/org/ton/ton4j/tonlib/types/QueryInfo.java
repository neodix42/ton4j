package org.ton.ton4j.tonlib.types;

import java.io.Serializable;
import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class QueryInfo implements Serializable {
  long id;
  long valid_until;
  String body_hash; // byte[]
}
