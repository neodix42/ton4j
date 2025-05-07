package org.ton.java.tonlib.types;

import java.io.Serializable;
import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class RawAccountForm implements Serializable {
  final String given_type = "raw_form";
  // final String given_type = "friendly_non_bounceable";
  String raw_form;
  boolean test_only;
  Bounceable bounceable;
  NonBounceable non_bounceable;
}
