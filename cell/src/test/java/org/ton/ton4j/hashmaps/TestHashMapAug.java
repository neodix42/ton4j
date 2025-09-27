package org.ton.ton4j.hashmaps;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.ton.ton4j.cell.CellSlice.beginParse;

import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.ton.ton4j.cell.*;

@Slf4j
@RunWith(JUnit4.class)
public class TestHashMapAug {

  @Test
  public void testHashMapAugSerialization() {
    TonHashMapAug hashmapAug = new TonHashMapAug(32);
    //    hashmapAug.keyParser1 = cellSlice -> cellSlice.loadUint(32);
    //    hashmapAug.valueParser1 = cellSlice -> cellSlice.loadUint(32);
    //    hashmapAug.extraParser1 = cellSlice -> cellSlice.loadUint(32);
    for (long i = 1; i <= 10; i++) {
      long value = i * 3;
      long extra = value * value;
      hashmapAug.elements.put(i, new ValueExtra(value, extra));
    }

    Cell cell =
        hashmapAug.serialize(
            k -> CellBuilder.beginCell().storeUint((Long) k, 32).endCell().getBits(),
            v -> CellBuilder.beginCell().storeUint((Long) v, 32).endCell(),
            e -> CellBuilder.beginCell().storeUint((Long) e, 32).endCell(),
            (fv, fe) ->
                CellBuilder.beginCell()
                    .storeUint(
                        CellSlice.beginParse((Cell) fv).loadUint(32).longValue()
                            + CellSlice.beginParse((Cell) fe).loadUint(32).longValue(),
                        32));

    log.info("serialized cell {}", cell.print());
    log.info("serialized cell {}", cell.toHex()); // wrong, below can't be read

    CellSlice cs = CellSlice.beginParse(cell);
    TonHashMapAug loadedDict =
        cs.loadDictAug(
            32,
            k -> k.readUint(32),
            v -> CellSlice.beginParse(v).loadUint(32),
            e -> CellSlice.beginParse(e).loadUint(32));
    log.info("deserialized");

    assertThat(loadedDict.elements.size()).isEqualTo(10);

    log.info("HashmapAug {}", loadedDict);
  }

  @Test
  public void testHashMapAugTenEntriesDeserialization() {
    Cell cell =
        CellBuilder.beginCell()
            .fromBoc(
                "b5ee9c724101130100c000020bce2c00000640020102094d00000898070902090b000004a00a03020900000011a0041002090000001720060500110000003840000003e00011000000240000000320020900000051a0080f00110000009000000006200011d000003840000001e802092c000001e00b0c0011b00000009000000038020900000007600d0e00110000000900000001a000110000001440000002600011000000b640000006e002090000002fe0121100110000006e400000056000110000005100000004a034f83fbf")
            .endCell();
    log.info("cell {}", cell.print());

    CellSlice cs = beginParse(cell);

    TonHashMapAug loadedDict =
        cs.loadDictAug(
            32,
            k -> k.readUint(32),
            v -> beginParse(v).loadUint(32),
            e -> beginParse(e).loadUint(32));

    log.info("Deserialized hashmap from cell count {}", loadedDict.elements.size());
  }
}
