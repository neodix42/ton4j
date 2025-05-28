package org.ton.ton4j.hashmaps;

import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.ton.ton4j.cell.*;
import org.ton.ton4j.utils.Utils;

import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
@RunWith(JUnit4.class)
public class TestPfxHashMapE {

    @Test
    public void testEmptyPfxHashMapESerialization() {
        TonPfxHashMapE x = new TonPfxHashMapE(9);
        x.serialize(
                k -> CellBuilder.beginCell().storeUint((Long) k, 9).endCell().getBits(),
                v -> CellBuilder.beginCell().storeUint((byte) v, 3).endCell());

        log.info("pfx-hashmapE x {}", x);
    }

    @Test
    public void testPfxHashMapESerialization() {
        int dictKeySize = 9;
        TonPfxHashMapE x = new TonPfxHashMapE(dictKeySize);

        x.elements.put(100L, (byte) 1);
        x.elements.put(200L, (byte) 2);
        x.elements.put(300L, (byte) 3);
        x.elements.put(400L, (byte) 4);

        log.info("pfx-hashmapE x {}", x);

        Cell dictCell = x.serialize(
                k -> CellBuilder.beginCell().storeUint((Long) k, dictKeySize).endCell().getBits(),
                v -> CellBuilder.beginCell().storeUint((byte) v, 3).endCell()
        );

        log.info("serialized cell: \n{}", dictCell.print());
        log.info("serialized boc: \n{}", dictCell.toHex());
        log.info("cell hash {}", Utils.bytesToHex(dictCell.hash()));

        Cell cellWithDict = CellBuilder.beginCell().storeDict(dictCell).endCell();

        CellSlice cs = CellSlice.beginParse(cellWithDict);
        TonPfxHashMapE dex = cs.loadDictPfxE(dictKeySize,
                k -> k.readUint(dictKeySize),
                v -> CellSlice.beginParse(v).loadUint(3)
        );

        log.info("pfx-hashmapE x {}", dex);

        assertThat(Utils.bytesToHex(dictCell.toBoc())).isEqualTo(Utils.bytesToHex(dictCell.toBoc()));
    }

    @Test
    public void testPfxHashMapESerializationParse() {
        int dictKeySize = 9;
        TonPfxHashMapE x = new TonPfxHashMapE(dictKeySize);

        x.elements.put(100L, (byte) 1);
        x.elements.put(200L, (byte) 2);
        x.elements.put(300L, (byte) 3);
        x.elements.put(400L, (byte) 4);

        log.info("pfx-hashmapE x {}", x);

        Cell dictCell = x.serialize(
                k -> CellBuilder.beginCell().storeUint((Long) k, dictKeySize).endCell().getBits(),
                v -> CellBuilder.beginCell().storeUint((byte) v, 3).endCell()
        );

        log.info("serialized cell: \n{}", dictCell.print());
        log.info("serialized boc: \n{}", dictCell.toHex());
        log.info("cell hash {}", Utils.bytesToHex(dictCell.hash()));

        CellSlice cs = CellSlice.beginParse(dictCell);
        TonPfxHashMap dex = cs.parseDictPfx(dictKeySize,
                k -> k.readUint(dictKeySize),
                v -> CellSlice.beginParse(v).loadUint(3)
        );

        log.info("pfx-hashmapE x {}", dex);

        assertThat(Utils.bytesToHex(dictCell.toBoc())).isEqualTo(Utils.bytesToHex(dictCell.toBoc()));
    }
}