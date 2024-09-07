package org.ton.java.tlb.types;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.ton.java.cell.Cell;
import org.ton.java.cell.CellBuilder;
import org.ton.java.cell.CellSlice;

import java.math.BigInteger;

import static java.util.Objects.nonNull;

/**
 * <pre>
 * addr_extern$01 len:(## 9) external_address:(bits len) = MsgAddressExt;
 * </pre>
 */
@Builder
@Getter
@Setter

public class MsgAddressExternal implements MsgAddressExt {
    int magic;
    int len;
    public BigInteger externalAddress;

    @Override
    public String toString() {
        return nonNull(externalAddress) ? externalAddress.toString(16) : null;
    }

    public Cell toCell() {
        return CellBuilder.beginCell()
                .storeUint(0b01, 2)
                .storeUint(len, 9)
                .storeUint(externalAddress, len)
                .endCell();
    }

    public static MsgAddressExternal deserialize(CellSlice cs) {
        int magic = cs.loadUint(2).intValue();
        assert (magic == 0b01) : "MsgAddressExternal: magic not equal to 0b01, found " + magic;
        int len = cs.loadUint(9).intValue();
        BigInteger externalAddress = cs.loadUint(len);
        return MsgAddressExternal.builder()
                .magic(magic)
                .len(len)
                .externalAddress(externalAddress)
                .build();
    }
}