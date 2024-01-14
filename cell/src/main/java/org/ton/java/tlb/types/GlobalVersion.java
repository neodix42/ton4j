package org.ton.java.tlb.types;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.ton.java.cell.Cell;
import org.ton.java.cell.CellBuilder;

import java.math.BigInteger;

@Builder
@Getter
@Setter
@ToString
/**
 * capabilities#c4 version:uint32 capabilities:uint64 = GlobalVersion;
 */
public class GlobalVersion {
    long magic;
    long version;
    BigInteger capabilities;

    private String getMagic() {
        return Long.toHexString(magic);
    }

    public Cell toCell() {
        return CellBuilder.beginCell()
                .storeUint(0xc4, 32)
                .storeUint(version, 32)
                .storeUint(capabilities, 64)
                .endCell();
    }
}
