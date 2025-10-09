package org.ton.ton4j.exporter.lazy;

import org.ton.ton4j.address.Address;
import org.ton.ton4j.cell.Cell;
import org.ton.ton4j.tlb.MsgAddress;

/**
 *
 *
 * <pre>{@code
 * addr_std$10 anycast:(Maybe Anycast)  workchain_id:int8 address:bits256  = MsgAddressInt;
 *
 * addr_var$11 anycast:(Maybe Anycast) addr_len:(## 9)  workchain_id:int32 address:(bits addr_len) = MsgAddressInt;
 * }</pre>
 */
public interface MsgAddressIntLazy extends MsgAddress {
  Cell toCell();

  Address toAddress();

  static MsgAddressIntLazy deserialize(CellSliceLazy cs) {
    int magic = cs.preloadUint(2).intValue();
    switch (magic) {
      case 0b10:
        {
          return MsgAddressIntLazyStd.deserialize(cs);
        }
      case 0b11:
        {
          return MsgAddressIntLazyVar.deserialize(cs);
        }
    }
    throw new Error("Wrong magic for MsgAddressInt, found " + magic);
  }
}
