package org.ton.java.tlb.types;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.ton.java.address.Address;
import org.ton.java.cell.Cell;
import org.ton.java.cell.CellBuilder;
import org.ton.java.cell.CellSlice;

import java.math.BigInteger;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

@Builder
@Getter
@Setter
/**
 * addr_var$11
 *   anycast:(Maybe Anycast)
 *   addr_len:(## 9)
 *   workchain_id:int32
 *   address:(bits addr_len) = MsgAddressInt;
 */
public class MsgAddressIntVar implements MsgAddressInt {
    int magic;
    Anycast anycast;
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
        result.storeUint(addrLen, 9)
                .storeUint(workchainId, 32)
                .storeUint(address, addrLen);
        return result.endCell();
    }

    public static MsgAddressIntVar deserialize(CellSlice cs) {
        int magic = cs.loadUint(2).intValue();
        assert (magic == 0b11) : "MsgAddressIntVar: magic not equal to 0b11, found " + magic;

        Anycast anycast = null;
        if (cs.loadBit()) {
            anycast = Anycast.deserialize(cs);
        }
        int addrLen = cs.loadUint(9).intValue();
        return MsgAddressIntVar.builder()
                .magic(magic)
                .anycast(anycast)
                .addrLen(addrLen)
                .workchainId(cs.loadUint(32).intValue())
                .address(cs.loadUint(addrLen))
                .build();
    }

    public Address toAddress() {
        return Address.of(toString());
    }
}
