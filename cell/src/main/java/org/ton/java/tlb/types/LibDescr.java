package org.ton.java.tlb.types;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.ton.java.cell.Cell;
import org.ton.java.cell.CellBuilder;
import org.ton.java.cell.CellSlice;
import org.ton.java.cell.TonHashMap;

@Builder
@Getter
@Setter
@ToString
/**
 shared_lib_descr$00 lib:^Cell publishers:(Hashmap 256 True)
 = LibDescr;
 */
public class LibDescr {
    long magic;
    Cell lib;
    TonHashMap publishers;

    private String getMagic() {
        return Long.toBinaryString(magic);
    }

    public Cell toCell() {
        return CellBuilder.beginCell()
                .storeUint(0, 2)
                .storeRef(lib)
                .storeDict(publishers.serialize(
                        k -> CellBuilder.beginCell().storeUint((Long) k, 256).endCell().bits,
                        v -> CellBuilder.beginCell().storeBit(true).endCell()))
                .endCell();
    }

    public static LibDescr deserialize(CellSlice cs) {
        long magic = cs.loadUint(2).longValue();
        assert (magic == 0b00) : "LibDescr: magic not equal to 0b00, found 0x" + Long.toHexString(magic);
        return LibDescr.builder()
                .magic(0b00)
                .lib(cs.loadRef())
                .publishers(cs.loadDict(256,
                        k -> k.readInt(256),
                        v -> CellSlice.beginParse(v).loadBit()))
                .build();
    }
}