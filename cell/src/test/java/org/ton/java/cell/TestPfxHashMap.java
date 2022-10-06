package org.ton.java.cell;

import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.ton.java.utils.Utils;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertThrows;

@Slf4j
@RunWith(JUnit4.class)
public class TestPfxHashMap {

    @Test
    public void testPfxHashMapDeserializationFromBoc() {
        String t = "B5EE9C7241010501007A00020374C001020045A0E034CD6A3000596F07C3F0AB332935D3E3FC98F1E78F6AE1FC710EA4D98732772F1002057FBFB003040043BFB333333333333333333333333333333333333333333333333333333333333333400043BF955555555555555555555555555555555555555555555555555555555555555540DE161D24";

        Cell cell = Cell.fromBoc(t);

        CellSlice cs = CellSlice.beginParse(cell);
        TonPfxHashMap dict = cs.loadDictPfx(267,
                k -> k.readAddress(),
                v -> true
        );

        for (Map.Entry<Object, Object> entry : dict.elements.entrySet()) {
            log.info("key {}, value {}", entry.getKey(), entry.getValue());
        }

        assertThat(dict.elements.size()).isEqualTo(3);
    }

    @Test
    public void testEmptyPfxHashMapSerialization() {
        TonPfxHashMap x = new TonPfxHashMap(9);
        assertThrows(Error.class, () -> x.serialize(
                k -> CellBuilder.beginCell().storeUint((Long) k, 9).bits,
                v -> CellBuilder.beginCell().storeUint((byte) v, 3)
        ));
    }

    @Test
    public void testPfxHashMapSerialization() {
        int dictKeySize = 9;
        TonPfxHashMap x = new TonPfxHashMap(dictKeySize);

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
        TonPfxHashMap dex = cs.loadDictPfx(dictKeySize,
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