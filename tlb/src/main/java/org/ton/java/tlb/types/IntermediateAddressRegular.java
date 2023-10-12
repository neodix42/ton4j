package org.ton.java.tlb.types;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * interm_addr_regular$0 use_dest_bits:(#<= 96) = IntermediateAddress;
 */
@Builder
@Getter
@Setter
@ToString
public class IntermediateAddressRegular implements IntermediateAddress {
    int use_dest_bits; // 7 bits
}
