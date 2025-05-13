package org.ton.ton4j.tlb;

import org.ton.ton4j.address.Address;
import org.ton.ton4j.cell.Cell;
import org.ton.ton4j.cell.CellSlice;

/**
 *
 *
 * <pre>{@code
 * addr_std$10 anycast:(Maybe Anycast)  workchain_id:int8 address:bits256  = MsgAddressInt;
 *
 * addr_var$11 anycast:(Maybe Anycast) addr_len:(## 9)  workchain_id:int32 address:(bits addr_len) = MsgAddressInt;
 * }</pre>
 */
public interface MsgAddressInt extends MsgAddress {
  Cell toCell();

  Address toAddress();

  static MsgAddressInt deserialize(CellSlice cs) {
    int magic = cs.preloadUint(2).intValue();
    switch (magic) {
      case 0b10:
        {
          return MsgAddressIntStd.deserialize(cs);
        }
      case 0b11:
        {
          return MsgAddressIntVar.deserialize(cs);
        }
    }
    throw new Error("Wrong magic for MsgAddressInt, found " + magic);
  }
}
