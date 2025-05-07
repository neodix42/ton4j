package org.ton.java.tonlib.types;

import java.io.Serializable;
import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class Bounceable implements Serializable {
  String b64;
  String b64url;
}
