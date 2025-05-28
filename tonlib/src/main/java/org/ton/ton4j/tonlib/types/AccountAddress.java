package org.ton.ton4j.tonlib.types;

import java.io.Serializable;
import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class AccountAddress implements Serializable {
  RawAccountForm account_address;
}
