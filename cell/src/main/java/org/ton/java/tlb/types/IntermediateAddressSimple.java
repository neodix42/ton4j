package org.ton.java.tlb.types;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.math.BigInteger;

/**
 * interm_addr_simple$10 workchain_id:int8 addr_pfx:uint64 = IntermediateAddress;
 */
@Builder
@Getter
@Setter
@ToString
public class IntermediateAddressSimple implements IntermediateAddress {
    int workchainId;
    BigInteger addrPfx;
}
