package org.ton.java.tonlib.types;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

import java.io.Serializable;

@SuperBuilder
@Setter
@Getter
@ToString
public class AccountAddressOnly implements Serializable {
    String account_address;
}