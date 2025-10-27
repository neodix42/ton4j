package org.ton.ton4j.hashmaps;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertThrows;

import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.ton.ton4j.bitstring.BitString;
import org.ton.ton4j.cell.Cell;
import org.ton.ton4j.cell.CellBuilder;
import org.ton.ton4j.cell.CellSlice;
import org.ton.ton4j.cell.TonPfxHashMap;
import org.ton.ton4j.utils.Utils;

@Slf4j
@RunWith(JUnit4.class)
public class TestPfxHashMap {

  @Test
  public void testPfxHashMapDeserializationFromBoc() {
    String t =
        "B5EE9C7241010501007A00020374C001020045A0E034CD6A3000596F07C3F0AB332935D3E3FC98F1E78F6AE1FC710EA4D98732772F1002057FBFB003040043BFB333333333333333333333333333333333333333333333333333333333333333400043BF955555555555555555555555555555555555555555555555555555555555555540DE161D24";

    Cell cellWithDict = CellBuilder.beginCell().fromBoc(t).endCell();

    CellSlice cs = CellSlice.beginParse(cellWithDict);
    TonPfxHashMap dict = cs.loadDictPfx(267, BitString::readAddress, v -> true);

    log.info("pfx-hashmap dict {}, count {}", dict, dict.elements.size());

    assertThat(dict.elements.size()).isEqualTo(3);
  }

  @Test
  public void testEmptyPfxHashMapSerialization() {
    TonPfxHashMap x = new TonPfxHashMap(9);
    assertThrows(
        Error.class,
        () ->
            x.serialize(
                k -> CellBuilder.beginCell().storeUint((Long) k, 9).endCell().getBits(),
                v -> CellBuilder.beginCell().storeUint((byte) v, 3).endCell()));

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

    Cell dictCell =
        x.serialize(
            k -> CellBuilder.beginCell().storeUint((Long) k, dictKeySize).endCell().getBits(),
            v -> CellBuilder.beginCell().storeUint((byte) v, 3).endCell());

    log.info("serialized cell: \n{}", dictCell.print());
    log.info("serialized boc: \n{}", dictCell.toHex());
    log.info("cell hash {}", Utils.bytesToHex(dictCell.hash()));

    CellSlice cs = CellSlice.beginParse(dictCell);
    TonPfxHashMap dex =
        cs.loadDictPfx(
            dictKeySize, k -> k.readUint(dictKeySize), v -> CellSlice.beginParse(v).loadUint(3));

    log.info("pfx-hashmap dict {}", dex);

    assertThat(Utils.bytesToHex(dictCell.toBoc())).isEqualTo(Utils.bytesToHex(dictCell.toBoc()));
    assertThat(dex.elements.size()).isEqualTo(4);
  }
}
