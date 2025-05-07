package org.ton.java.tonlib.types;

import java.io.Serializable;
import lombok.Data;
import lombok.experimental.SuperBuilder;

@SuperBuilder
@Data
public class AccountAddressOnly implements Serializable {
  String account_address;
}
