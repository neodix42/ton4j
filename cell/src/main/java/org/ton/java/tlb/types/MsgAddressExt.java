package org.ton.java.tlb.types;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.ton.java.cell.Cell;
import org.ton.java.cell.CellBuilder;

import java.math.BigInteger;

import static java.util.Objects.nonNull;

@Builder
@Getter
@Setter
@ToString
/**
 * addr_none$00 = MsgAddressExt;
 * addr_extern$01 len:(## 9) external_address:(bits len) = MsgAddressExt;
 */
public class MsgAddressExt {
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
    
}
