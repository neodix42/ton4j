package org.ton.java.tonlib.types;

import lombok.*;
import lombok.experimental.SuperBuilder;

@SuperBuilder
@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class AccountAddressOnly {
    private String account_address;
}