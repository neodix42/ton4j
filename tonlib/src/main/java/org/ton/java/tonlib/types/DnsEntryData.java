package org.ton.java.tonlib.types;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@SuperBuilder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class DnsEntryData {
    private String type;
    private String bytes;
    private String text;
    private AccountAddressOnly resolver;
    private AccountAddressOnly smc_address;
    private AccountAddressOnly adnl_address;
}
