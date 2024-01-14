package org.ton.java.tlb.types;

import org.ton.java.address.Address;
import org.ton.java.cell.Cell;


/**
 * addr_std$10 anycast:(Maybe Anycast)  workchain_id:int8 address:bits256  = MsgAddressInt;
 * <p>
 * addr_var$11
 * anycast:(Maybe Anycast)
 * addr_len:(## 9)
 * workchain_id:int32
 * address:(bits addr_len) = MsgAddressInt;
 */
public interface MsgAddressInt {
    public Cell toCell();

    public Address toAddress();
}
