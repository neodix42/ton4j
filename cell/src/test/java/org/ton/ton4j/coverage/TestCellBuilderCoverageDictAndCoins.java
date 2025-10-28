package org.ton.ton4j.coverage;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigInteger;
import java.util.Arrays;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.ton.ton4j.bitstring.BitString;
import org.ton.ton4j.cell.Cell;
import org.ton.ton4j.cell.CellBuilder;
import org.ton.ton4j.cell.CellSlice;

@RunWith(JUnit4.class)
public class TestCellBuilderCoverageDictAndCoins {

  @Test
  public void testStoreDictAndDictInLineAndCellMaybe() {
    // Prepare a small cell acting like a dict
    Cell dict = CellBuilder.beginCell().storeUint(0xF, 4).endCell();

    // storeDictInLine copies bits/refs into current cell
    Cell inLine = CellBuilder.beginCell().storeDictInLine(dict).endCell();
    assertThat(inLine.getBitLength()).isEqualTo(4);

    // storeDict uses storeRefMaybe
    Cell withRef = CellBuilder.beginCell().storeDict(dict).endCell();
    CellSlice s = CellSlice.beginParse(withRef);
    assertThat(s.loadBit()).isTrue();
    assertThat(s.loadRef().getBitLength()).isEqualTo(4);

    // storeCell and storeCellMaybe(null/non-null)
    Cell base = CellBuilder.beginCell().storeUint(1, 1).endCell();
    Cell withCell = CellBuilder.beginCell().storeCell(base).endCell();
    assertThat(withCell.getBitLength()).isEqualTo(1);

    Cell maybe = CellBuilder.beginCell().storeCellMaybe(null).storeCellMaybe(base).endCell();
    CellSlice sm = CellSlice.beginParse(maybe);
    assertThat(sm.loadBit()).isFalse();
    assertThat(sm.loadBit()).isTrue();
    assertThat(sm.loadUint(1).intValue()).isEqualTo(1);
  }

  @Test
  public void testStoreCoinsAndMaybeAndStoreSlice() {
    // storeCoins (non-null)
    Cell coins = CellBuilder.beginCell().storeCoins(BigInteger.valueOf(123456789)).endCell();
    assertThat(coins.getBitLength()).isGreaterThan(0);

    // storeCoinsMaybe null vs non-null
    Cell cMaybe =
        CellBuilder.beginCell()
            .storeCoinsMaybe(null)
            .storeCoinsMaybe(BigInteger.valueOf(42))
            .endCell();
    CellSlice s = CellSlice.beginParse(cMaybe);
    assertThat(s.loadBit()).isFalse();
    assertThat(s.loadBit()).isTrue();
    assertThat(s.loadCoins()).isEqualTo(BigInteger.valueOf(42));

    // storeSlice with bits and refs
    Cell ref = CellBuilder.beginCell().storeUint(2, 2).endCell();
    Cell src = CellBuilder.beginCell().storeUint(0xAB, 8).storeRef(ref).endCell();
    Cell target = CellBuilder.beginCell().storeSlice(CellSlice.beginParse(src)).endCell();
    assertThat(target.getBitLength()).isEqualTo(8);
    assertThat(target.getUsedRefs()).isEqualTo(1);
  }

  @Test
  public void testStoreHashesSetsRefsCountThroughLazy() {
    // storeSliceLazy will invoke storeHashes which sets refs count from hashes length
    BitString bs = new BitString(8);
    bs.writeUint8(0xAA);
    byte[] hashes = new byte[64]; // 2 refs
    Cell c = CellBuilder.beginCell().storeSliceLazy(bs, hashes).endCell();
    assertThat(c.getRefsCount()).isEqualTo(2);
  }
}
