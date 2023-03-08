package org.ton.java.tonlib.types;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;

@Builder
@Setter
@Getter
@ToString
public class AccountAddress implements Serializable {
    RawAccountForm account_address;
}