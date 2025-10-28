package org.ton.ton4j.coverage;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.ton.ton4j.bitstring.BitString;
import org.ton.ton4j.cell.Cell;
import org.ton.ton4j.cell.CellBuilder;
import org.ton.ton4j.cell.CellSlice;
import org.ton.ton4j.cell.CellType;

@RunWith(JUnit4.class)
public class TestCellSliceCoverageMore {

  @Test
  public void testPreloadUintAndIntFastAndSlowPaths() {
    // Fast path (<= 64 bits)
    BitString bsFast = new BitString(32);
    bsFast.writeUint(BigInteger.valueOf(0xDEADBEEFL), 32);
    Cell cFast = CellBuilder.beginCell().storeBitString(bsFast).endCell();
    CellSlice sFast = CellSlice.beginParse(cFast);
    assertThat(sFast.preloadUint(32)).isEqualTo(BigInteger.valueOf(0xDEADBEEFL));
    // preloadInt interprets as signed two's-complement value for the given bitLength
    assertThat(sFast.preloadInt(32).intValue()).isEqualTo(-559038737);

    // Slow path (> 64 bits)
    BitString bsSlow = new BitString(128);
    // write 128 bits non-zero
    for (int i = 0; i < 128; i++) bsSlow.writeBit((i % 2) == 1);
    Cell cSlow = CellBuilder.beginCell().storeBitString(bsSlow).endCell();
    CellSlice sSlow = CellSlice.beginParse(cSlow);
    BigInteger pu = sSlow.preloadUint(128);
    assertThat(pu.bitLength()).isGreaterThan(64);
  }

  @Test
  public void testLoadListAndSliceToCellAndGetHashesConstructor() {
    // Prepare list of 3 values with 5-bit each
    List<BigInteger> values = Arrays.asList(BigInteger.valueOf(3), BigInteger.valueOf(7), BigInteger.valueOf(17));
    Cell c = CellBuilder.beginCell().storeList(values, 5).endCell();
    CellSlice s = CellSlice.beginParse(c);
    List<BigInteger> read = s.loadList(3, 5);
    assertThat(read).containsExactlyElementsOf(values);

    // Create CellSlice via full constructor with hashes
    BitString bs = new BitString(8);
    bs.writeUint8(0xAB);
    byte[] hashes = new byte[32];
    Arrays.fill(hashes, (byte) 1);
    CellSlice full = new CellSlice(bs, Collections.emptyList(), CellType.ORDINARY, hashes);
    assertThat(full.getHashes()).isSameAs(hashes);

    // sliceToCell should convert current slice into a Cell with same bits
    Cell fromSlice = full.sliceToCell();
    assertThat(fromSlice.getBitLength()).isEqualTo(8);
  }

  @Test
  public void testLoadIntMaybeAndLoadUintMaybe() {
    // loadIntMaybe: first null (flag false), then a value (flag true)
    Cell cInt =
        CellBuilder.beginCell()
            .storeBit(false)
            .storeBit(true)
            .storeInt(BigInteger.valueOf(-1), 4)
            .endCell();
    CellSlice si = CellSlice.beginParse(cInt);
    assertThat(si.loadIntMaybe(4)).isNull();
    assertThat(si.loadIntMaybe(4).intValue()).isEqualTo(-1);

    // loadUintMaybe: first null, then a value
    Cell cUint =
        CellBuilder.beginCell()
            .storeBit(false)
            .storeBit(true)
            .storeUint(BigInteger.valueOf(10), 5)
            .endCell();
    CellSlice su = CellSlice.beginParse(cUint);
    assertThat(su.loadUintMaybe(5)).isNull();
    assertThat(su.loadUintMaybe(5).intValue()).isEqualTo(10);
  }
}
