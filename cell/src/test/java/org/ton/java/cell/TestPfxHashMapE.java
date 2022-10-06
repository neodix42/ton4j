package org.ton.java.cell;

import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.ton.java.utils.Utils;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
@RunWith(JUnit4.class)
public class TestPfxHashMapE {

    @Test
    public void testEmptyPfxHashMapESerialization() {
        TonPfxHashMapE x = new TonPfxHashMapE(9);
        x.serialize(
                k -> CellBuilder.beginCell().storeUint((Long) k, 9).bits,
                v -> CellBuilder.beginCell().storeUint((byte) v, 3));
    }

    @Test
    public void testPfxHashMapESerialization() {
        int dictKeySize = 9;
        TonPfxHashMapE x = new TonPfxHashMapE(dictKeySize);

        x.elements.put(100L, (byte) 1);
        x.elements.put(200L, (byte) 2);
        x.elements.put(300L, (byte) 3);
        x.elements.put(400L, (byte) 4);

        Cell cell = x.serialize(
                k -> CellBuilder.beginCell().storeUint((Long) k, dictKeySize).bits,
                v -> CellBuilder.beginCell().storeUint((byte) v, 3)
        );

        log.info("serialized cell {}", cell.print());

        CellSlice cs = CellSlice.beginParse(cell);
        TonPfxHashMapE dex = cs.loadDictPfxE(dictKeySize,
                k -> k.readUint(dictKeySize),
                v -> CellSlice.beginParse(v).loadUint(3)
        );

        // traverse deserialized hashmap
        log.info("Deserialized hashmap from cell");
        for (Map.Entry<Object, Object> entry : dex.elements.entrySet()) {
            log.info("key {}, value {}", entry.getKey(), entry.getValue());
        }

        assertThat(Utils.bytesToHex(cell.toBoc(false))).isEqualTo(Utils.bytesToHex(cell.toBoc(false)));
    }
}