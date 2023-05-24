package org.ton.java.tlb.types;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.ton.java.address.Address;
import org.ton.java.cell.TonHashMap;

@Builder
@Getter
@Setter
public class ConfigParams {
    Address configAddr;
    TonHashMap config;  //  *cell.Dictionary
}
