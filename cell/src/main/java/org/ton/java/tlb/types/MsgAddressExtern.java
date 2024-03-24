package org.ton.java.tlb.types;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.ton.java.cell.Cell;
import org.ton.java.cell.CellBuilder;
import org.ton.java.cell.CellSlice;

import java.math.BigInteger;

import static java.util.Objects.nonNull;

@Builder
@Getter
@Setter
/**
 * addr_none$00 = MsgAddressExt;
 * addr_extern$01 len:(## 9) external_address:(bits len) = MsgAddressExt;
 */
public class MsgAddressExtern implements MsgAddressExt {
    int len;
    public BigInteger externalAddress;

    @Override
    public String toString() {
        return nonNull(externalAddress) ? externalAddress.toString(16) : null;
    }

    public Cell toCell() {
        return CellBuilder.beginCell()
                .storeUint(1, 2)
                .storeUint(len, 9)
                .storeUint(externalAddress, len)
                .endCell();
    }

    static MsgAddressExtern deserialize(CellSlice cs) {
        cs.loadUint(2); // int flagMsg =
        int len = cs.loadUint(9).intValue();
        BigInteger externalAddress = cs.loadUint(len);
        return MsgAddressExtern.builder()
                .len(len)
                .externalAddress(externalAddress)
                .build();
    }
}
