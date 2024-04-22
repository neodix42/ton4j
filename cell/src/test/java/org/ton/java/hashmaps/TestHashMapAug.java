package org.ton.java.hashmaps;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.ton.java.cell.*;

import static org.assertj.core.api.Assertions.assertThat;


@Slf4j
@RunWith(JUnit4.class)
public class TestHashMapAug {

    @Test
    public void testHashMapAugSerialization() {
        TonHashMapAug hashmapAug = new TonHashMapAug(32);
        for (long i = 1; i <= 10; i++) {
            long value = i * 3;
            long extra = value * value;
            hashmapAug.elements.put(i, Pair.of(value, extra));
        }
        log.info("HashmapAug {}", hashmapAug);

        Cell cell = hashmapAug.serialize(
                k -> CellBuilder.beginCell().storeUint((Long) k, 32).endCell().bits,
                v -> CellBuilder.beginCell().storeUint((Long) v, 32).endCell(),
                e -> CellBuilder.beginCell().storeUint((Long) e, 32).endCell(),
                (fv, fe) -> CellBuilder.beginCell().storeUint(
                        CellSlice.beginParse((Cell) fv).loadUint(32).longValue() +
                                CellSlice.beginParse((Cell) fe).loadUint(32).longValue(), 32)
        );

        log.info("serialized cell {}", cell.print());
        log.info("serialized cell {}", cell.toHex()); //wrong, below can't be read

        CellSlice cs = CellSlice.beginParse(cell);
        TonHashMapAug loadedDict = cs.loadDictAug(32,
                k -> k.readUint(32),
                v -> CellSlice.beginParse(v).loadUint(32),
                e -> CellSlice.beginParse(e).loadUint(32)
        );

        log.info("Deserialized hashmap from cell {}", loadedDict);

        assertThat(loadedDict.elements.size()).isEqualTo(10);
    }

    @Test
    public void testHashMapAugTenEntriesDeserialization() {

        Cell cell = CellBuilder.beginCell().fromBoc("b5ee9c724101130100c000020bce2c00000640020102094d00000898070902090b000004a00a03020900000011a0041002090000001720060500110000003840000003e00011000000240000000320020900000051a0080f00110000009000000006200011d000003840000001e802092c000001e00b0c0011b00000009000000038020900000007600d0e00110000000900000001a000110000001440000002600011000000b640000006e002090000002fe0121100110000006e400000056000110000005100000004a034f83fbf").endCell();
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