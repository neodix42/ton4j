package org.ton.java.hashmaps;

import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.ton.java.cell.Cell;
import org.ton.java.cell.CellSlice;
import org.ton.java.cell.TonHashMapAugE;

@Slf4j
@RunWith(JUnit4.class)
public class TestHashMapAugE {

    // all hashmaps have structure: HashMapAugE 32 uint32 uint32
    @Test
    public void testHashMapAugeEmptyDeserialization() {

        Cell cell = Cell.fromBoc("b5ee9c7201010101000300000140");
        log.info("cell {}", cell.print());

        CellSlice cs = CellSlice.beginParse(cell);

        TonHashMapAugE loadedDict = cs.loadDictAugE(32,
                k -> k.readUint(32),
                v -> CellSlice.beginParse(v).loadUint(32),
                e -> CellSlice.beginParse(e).loadUint(32)
        );

        log.info("Deserialized hashmap from cell {}", loadedDict);
    }

    @Test
    public void testHashMapAugeOneEntriesDeserialization() {

        Cell cell = Cell.fromBoc("b5ee9c72010102010013000101c001001aa0000000010000000900000003");
        log.info("cell {}", cell.print());

        CellSlice cs = CellSlice.beginParse(cell);

        TonHashMapAugE loadedDict = cs.loadDictAugE(32,
                k -> k.readUint(32),
                v -> CellSlice.beginParse(v).loadUint(32),
                e -> CellSlice.beginParse(e).loadUint(32)
        );

        log.info("Deserialized hashmap from cell {}", loadedDict);
    }

    @Test
    public void testHashMapAugeTwoEntriesDeserialization() {

        Cell cell = Cell.fromBoc("b5ee9c72010104010024000101c001020bcf00000016c0020300115000000090000000380011400000024000000068");
        log.info("cell {}", cell.print());

        CellSlice cs = CellSlice.beginParse(cell);

        TonHashMapAugE loadedDict = cs.loadDictAugE(32,
                k -> k.readUint(32),
                v -> CellSlice.beginParse(v).loadUint(32),
                e -> CellSlice.beginParse(e).loadUint(32)
        );

        log.info("Deserialized hashmap from cell {}", loadedDict);
    }

    @Test
    public void testHashMapAugeTenEntriesDeserialization() {

        Cell cell = Cell.fromBoc("b5ee9c720101140100c4000101c001020bce000006c4c0020302090000013b200405020940000089d8101102090000001fa0060702090000011ba00a0b001150000000900000003802090000001d60080900110000000900000001a0001100000014400000026002090000005c600c0d0209000000bf600e0f001100000024000000032000110000003840000003e000110000005100000004a000110000006e400000056002090000014660121300114000003840000001e800110000009000000006200011000000b640000006e0");
        log.info("cell {}", cell.print());

        CellSlice cs = CellSlice.beginParse(cell);

        TonHashMapAugE loadedDict = cs.loadDictAugE(32,
                k -> k.readUint(32),
                v -> CellSlice.beginParse(v).loadUint(32),
                e -> CellSlice.beginParse(e).loadUint(32)
        );

        log.info("Deserialized hashmap from cell {}", loadedDict);
    }
}