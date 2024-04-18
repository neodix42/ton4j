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
 * addr_std$10 anycast:(Maybe Anycast)  workchain_id:int8 address:bits256  = MsgAddressInt;
 *
 */
public class MsgAddressIntStd implements MsgAddressInt {
    int magic;
    Anycast anycast;
    int addrLen;
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
            result.writeCell(anycast.toCell());
        }
        result.storeInt(workchainId, 8)
                .storeUint(address, 256)
                .endCell();
        return result;
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
}
