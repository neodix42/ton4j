package org.ton.java.tlb.types;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.math.BigInteger;

import static java.util.Objects.nonNull;

@Builder
@Getter
@Setter
@ToString
/**
 * addr_std$10 anycast:(Maybe Anycast)  workchain_id:int8 address:bits256  = MsgAddressInt;
 *
 * addr_var$11
 *   anycast:(Maybe Anycast)
 *   addr_len:(## 9)
 *   workchain_id:int32
 *   address:(bits addr_len) = MsgAddressInt;
 */
public class MsgAddressInt {
    Anycast anycast;
    int addrLen;
    int workchainId;
    BigInteger address;

    @Override
    public String toString() {
        return nonNull(address) ? (workchainId + ":" + address.toString(16)) : null;
    }
}
