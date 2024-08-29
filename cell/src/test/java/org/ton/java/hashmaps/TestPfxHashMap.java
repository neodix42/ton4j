package org.ton.java.hashmaps;

import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.ton.java.cell.Cell;
import org.ton.java.cell.CellBuilder;
import org.ton.java.cell.CellSlice;
import org.ton.java.cell.TonPfxHashMap;
import org.ton.java.utils.Utils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertThrows;

@Slf4j
@RunWith(JUnit4.class)
public class TestPfxHashMap {

    @Test
    public void testPfxHashMapDeserializationFromBoc() {
        String t = "B5EE9C7241010501007A00020374C001020045A0E034CD6A3000596F07C3F0AB332935D3E3FC98F1E78F6AE1FC710EA4D98732772F1002057FBFB003040043BFB333333333333333333333333333333333333333333333333333333333333333400043BF955555555555555555555555555555555555555555555555555555555555555540DE161D24";

        Cell cell = CellBuilder.beginCell().fromBoc(t).endCell();

        CellSlice cs = CellSlice.beginParse(cell);
        TonPfxHashMap dict = cs.loadDictPfx(267,
                k -> k.readAddress(),
                v -> true
        );

        log.info("pfx-hashmap dict {}", dict);

        assertThat(dict.elements.size()).isEqualTo(3);
    }

    @Test
    public void testEmptyPfxHashMapSerialization() {
        TonPfxHashMap x = new TonPfxHashMap(9);
        assertThrows(Error.class, () -> x.serialize(
                k -> CellBuilder.beginCell().storeUint((Long) k, 9).endCell().getBits(),
                v -> CellBuilder.beginCell().storeUint((byte) v, 3).endCell()
        ));

        log.info("pfx-hashmap dict {}", x);
    }

    @Test
    public void testPfxHashMapSerialization() {
        int dictKeySize = 9;
        TonPfxHashMap x = new TonPfxHashMap(dictKeySize);

        x.elements.put(100L, (byte) 1);
        x.elements.put(200L, (byte) 2);
        x.elements.put(300L, (byte) 3);
        x.elements.put(400L, (byte) 4);

        log.info("pfx-hashmap dict {}", x);

        Cell cell = x.serialize(
                k -> CellBuilder.beginCell().storeUint((Long) k, dictKeySize).endCell().getBits(),
                v -> CellBuilder.beginCell().storeUint((byte) v, 3).endCell()
        );

        log.info("serialized cell: \n{}", cell.print());
        log.info("serialized boc: \n{}", cell.toHex());
        log.info("cell hash {}", Utils.bytesToHex(cell.hash()));

        CellSlice cs = CellSlice.beginParse(cell);
        TonPfxHashMap dex = cs.loadDictPfx(dictKeySize,
                k -> k.readUint(dictKeySize),
                v -> CellSlice.beginParse(v).loadUint(3)
        );

        log.info("pfx-hashmap dict {}", dex);

        assertThat(Utils.bytesToHex(cell.toBoc())).isEqualTo(Utils.bytesToHex(cell.toBoc()));
        assertThat(dex.elements.size()).isEqualTo(4);
    }
}