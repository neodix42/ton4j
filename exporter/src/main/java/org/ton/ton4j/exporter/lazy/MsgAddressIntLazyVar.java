package org.ton.ton4j.exporter.lazy;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

import java.io.Serializable;
import java.math.BigInteger;
import lombok.Builder;
import lombok.Data;
import org.ton.ton4j.address.Address;
import org.ton.ton4j.cell.Cell;
import org.ton.ton4j.cell.CellBuilder;

/**
 *
 *
 * <pre>
 * addr_var$11
 *   anycast:(Maybe Anycast)
 *   addr_len:(## 9)
 *   workchain_id:int32
 *   address:(bits addr_len) = MsgAddressInt;
 *   </pre>
 */
@Builder
@Data
public class MsgAddressIntLazyVar implements MsgAddressIntLazy, Serializable {
  int magic;
  AnycastLazy anycast;
  int addrLen;
  int workchainId;
  BigInteger address;

  @Override
  public String toString() {
    return nonNull(address) ? (workchainId + ":" + address.toString(16)) : null;
  }

  public Cell toCell() {
    CellBuilder result = CellBuilder.beginCell();
    result.storeUint(0b11, 2);
    if (isNull(anycast)) {
      result.storeBit(false);
    } else {
      result.storeBit(true);
      result.storeCell(anycast.toCell());
    }
    result.storeUint(addrLen, 9).storeInt(workchainId, 32).storeUint(address, addrLen);
    return result.endCell();
  }

  public static MsgAddressIntLazyVar deserialize(CellSliceLazy cs) {
    int magic = cs.loadUint(2).intValue();
    assert (magic == 0b11) : "MsgAddressIntVar: magic not equal to 0b11, found " + magic;

    AnycastLazy anycast = null;
    if (cs.loadBit()) {
      anycast = AnycastLazy.deserialize(cs);
    }
    int addrLen = cs.loadUint(9).intValue();
    return MsgAddressIntLazyVar.builder()
        .magic(magic)
        .anycast(anycast)
        .addrLen(addrLen)
        .workchainId(cs.loadInt(32).intValue())
        .address(cs.loadUint(addrLen))
        .build();
  }

  public Address toAddress() {
    return Address.of(toString());
  }
}
