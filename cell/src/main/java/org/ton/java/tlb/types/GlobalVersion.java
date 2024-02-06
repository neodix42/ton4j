package org.ton.java.tlb.types;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.ton.java.cell.Cell;
import org.ton.java.cell.CellBuilder;
import org.ton.java.cell.CellSlice;

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

    public static GlobalVersion deserialize(CellSlice cs) {
        long magic = cs.loadUint(8).longValue();
        assert (magic == 0xc4L) : "GlobalVersion: magic not equal to 0xc4, found 0x" + Long.toHexString(magic);

        return GlobalVersion.builder()
                .magic(0xc4L)
                .version(cs.loadUint(32).longValue())
                .capabilities(cs.loadUint(64))
                .build();
    }
}
