package org.ton.java.tlb.types;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

import java.math.BigInteger;
import lombok.Builder;
import lombok.Data;
import org.ton.java.address.Address;
import org.ton.java.cell.Cell;
import org.ton.java.cell.CellBuilder;
import org.ton.java.cell.CellSlice;

/**
 *
 *
 * <pre>
 * addr_std$10 anycast:(Maybe Anycast)  workchain_id:int8 address:bits256  = MsgAddressInt;
 *
 * anycast - default is storeBit(false)
 * </pre>
 */
@Builder
@Data
public class MsgAddressIntStd implements MsgAddressInt {
  int magic;
  Anycast anycast;
  byte workchainId;
  BigInteger address;

  @Override
  public String toString() {
    return nonNull(address) ? (workchainId + ":" + address.toString(16)) : null;
  }

  public Cell toCell() {
    CellBuilder result = CellBuilder.beginCell();
    result.storeUint(0b10, 2);
    if (isNull(anycast)) {
      result.storeBit(false);
    } else {
      result.storeBit(true);
      result.storeCell(anycast.toCell());
    }
    result.storeInt(workchainId, 8);
    result.storeUint(address, 256);
    return result.endCell();
  }

  public static MsgAddressIntStd deserialize(CellSlice cs) {
    int magic = cs.loadUint(2).intValue();
    assert (magic == 0b10) : "MsgAddressIntStd: magic not equal to 0b10, found " + magic;

    Anycast anycast = null;
    if (cs.loadBit()) {
      anycast = Anycast.deserialize(cs);
    }
    return MsgAddressIntStd.builder()
        .magic(magic)
        .anycast(anycast)
        .workchainId(cs.loadInt(8).byteValue())
        .address(cs.loadUint(256))
        .build();
  }

  public Address toAddress() {
    return Address.of(toString());
  }

  public static MsgAddressIntStd of(String address) {
    Address addr = new Address(address);
    return MsgAddressIntStd.builder()
        .workchainId(addr.wc)
        .address(addr.toBigInteger())
        .anycast(null)
        .build();
  }

  public static MsgAddressIntStd of(Address address) {
    return MsgAddressIntStd.builder()
        .workchainId(address.wc)
        .address(address.toBigInteger())
        .anycast(null)
        .build();
  }
}
