package org.ton.java.tonlib.types;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

@SuperBuilder
@Setter
@Getter
@ToString
public class AccountAddressOnly {
    String account_address;
}