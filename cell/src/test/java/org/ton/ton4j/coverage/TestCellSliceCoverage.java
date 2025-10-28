package org.ton.ton4j.coverage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertThrows;

import java.math.BigInteger;
import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.ton.ton4j.bitstring.BitString;
import org.ton.ton4j.cell.Cell;
import org.ton.ton4j.cell.CellBuilder;
import org.ton.ton4j.cell.CellSlice;

@RunWith(JUnit4.class)
public class TestCellSliceCoverage {

  private Cell simpleCell(byte... bytes) {
    return CellBuilder.beginCell().storeBytes(bytes).endCell();
  }

  @Test
  public void testBeginParseVariantsAndErrors() {
    Cell c = simpleCell((byte) 0xDE, (byte) 0xAD);
    CellSlice s1 = CellSlice.beginParse(c);
    assertThat(s1).isNotNull();

    // with hashes
    byte[] fakeHashes = new byte[0];
    CellSlice s2 = CellSlice.beginParse(c, fakeHashes);
    assertThat(s2).isNotNull();

    // beginParse with object
    CellSlice s3 = CellSlice.beginParse((Object) c);
    assertThat(s3).isNotNull();

    // of(Object) with Cell and with CellSlice
    assertThat(CellSlice.of(c)).isNotNull();
    assertThat(CellSlice.of(s3)).isNotNull();

    // errors
    assertThrows(IllegalArgumentException.class, () -> CellSlice.beginParse((Cell) null));
    assertThrows(Error.class, () -> CellSlice.beginParse("not a cell"));
    assertThrows(Error.class, () -> CellSlice.of("also bad"));
  }

  @Test
  public void testRefsLoadAndPreloadAndSkip() {
    Cell a = simpleCell((byte) 1);
    Cell b = simpleCell((byte) 2);
    Cell parent = CellBuilder.beginCell().storeRef(a).storeRef(b).endCell();

    CellSlice s = CellSlice.beginParse(parent);
    assertThat(s.getRefsCount()).isEqualTo(2);

    // preload
    assertThat(s.preloadRef().toBitString()).isNotEmpty();
    assertThat(s.getRefsCount()).isEqualTo(2);

    // preloadRefs
    List<Cell> pre = s.preloadRefs(2);
    assertThat(pre).hasSize(2);

    // loadRefs
    List<Cell> loaded = s.loadRefs(1);
    assertThat(loaded).hasSize(1);
    assertThat(s.getRefsCount()).isEqualTo(1);

    // skip remaining
    s.skipRefs(1);
    assertThat(s.getRefsCount()).isEqualTo(0);
  }

  @Test
  public void testMaybeRefXAndUnary() {
    // maybe=false path
    Cell c0 = CellBuilder.beginCell().storeBit(false).endCell();
    CellSlice s0 = CellSlice.beginParse(c0);
    assertThat(s0.loadMaybeRefX()).isNull();

    // maybe=true path
    Cell child = simpleCell((byte) 7);
    Cell c1 = CellBuilder.beginCell().storeBit(true).storeRef(child).endCell();
    CellSlice s1 = CellSlice.beginParse(c1);
    assertThat(s1.loadMaybeRefX()).isNotNull();

    // unary 0
    Cell c2 = CellBuilder.beginCell().storeBit(false).endCell();
    assertThat(CellSlice.beginParse(c2).loadUnary()).isEqualTo(0);

    // unary >=1 (recursive)
    Cell c3 = CellBuilder.beginCell().storeBit(true).storeBit(false).endCell();
    assertThat(CellSlice.beginParse(c3).loadUnary()).isEqualTo(1);
  }

  @Test
  public void testBitsAndBytesLoading() {
    byte[] data = new byte[] {0x11, 0x22, 0x33, 0x44};
    Cell c = CellBuilder.beginCell().storeBytes(data).endCell();
    CellSlice s = CellSlice.beginParse(c);

    // loadBytes(len)
    assertArrayEquals(new byte[] {0x11, 0x22}, s.loadBytes(2 * 8));

    // loadBytes() remaining unsigned ints
    int[] rem = s.loadBytes();
    assertThat(rem).containsExactly(0x33, 0x44);

    // loadSignedBytes none remaining now
    CellSlice s2 = CellSlice.beginParse(c);
    byte[] signed = s2.loadSignedBytes();
    assertArrayEquals(data, signed);
  }

  @Test
  public void testLoadSliceAndStrings() {
    // Prepare bits not aligned and aligned
    BitString bs = new BitString(16);
    bs.writeBits("1010101011110000");
    Cell c = CellBuilder.beginCell().storeBitString(bs, 12).endCell();

    CellSlice s = CellSlice.beginParse(c);
    int[] sl = s.loadSlice(12); // 12 bits -> 2 bytes padding inside method
    assertThat(sl.length).isGreaterThan(0);

    // Strings
    Cell c2 = CellBuilder.beginCell().storeString("HELLO").endCell();
    CellSlice s2 = CellSlice.beginParse(c2);
    assertThat(s2.loadString("HELLO".length() * 8)).isEqualTo("HELLO");

    Cell c3 = CellBuilder.beginCell().storeSnakeString("WORLD").endCell();
    CellSlice s3 = CellSlice.beginParse(c3);
    assertThat(s3.loadSnakeString()).isEqualTo("WORLD");
  }

  @Test
  public void testIntsAndCoinsAndVarUInt() {
    // store some numbers to read back
    Cell c =
        CellBuilder.beginCell()
            .storeUint(10, 5)
            .storeInt(-3, 4)
            .storeVarUint(BigInteger.valueOf(300))
            .storeCoins(BigInteger.valueOf(123456789))
            .endCell();

    CellSlice s = CellSlice.beginParse(c);
    assertThat(s.loadUint(5).intValue()).isEqualTo(10);
    assertThat(s.loadInt(4).intValue()).isEqualTo(-3);
    assertThat(s.loadVarUInteger(16).intValue()).isEqualTo(300);
    assertThat(s.preloadCoins()).isEqualTo(BigInteger.valueOf(123456789));
    assertThat(s.skipCoins()).isEqualTo(BigInteger.valueOf(123456789));
    assertThat(s.isSliceEmpty()).isTrue();
  }

  @Test
  public void testOverflowChecks() {
    // checkBitsOverflow triggers error when too short
    Cell c = CellBuilder.beginCell().storeUint(1, 1).endCell();
    CellSlice s = CellSlice.beginParse(c);
    assertThrows(Error.class, () -> s.loadBytes(4));

    // checkRefsOverflow
    Cell p = CellBuilder.beginCell().endCell();
    CellSlice sp = CellSlice.beginParse(p);
    assertThrows(Error.class, sp::loadRef);
  }

  @Test
  public void testPreloadBitAndPositions() {
    Cell c = CellBuilder.beginCell().storeBits("1011").endCell();
    CellSlice s = CellSlice.beginParse(c);
    assertThat(s.preloadBit()).isTrue(); // first is 1
    //    assertThat(s.preloadBitAt(0)).isTrue();
    assertThat(s.preloadBitAt(1)).isTrue(); // index starts with 1 at preloadBitAt!
    assertThat(s.preloadBitAt(3)).isTrue();
    assertThat(s.getRestBits()).isEqualTo(4);
    assertThat(s.getFreeBits()).isEqualTo(4);

    s.skipBit().skipBits(3);
    assertThat(s.isSliceEmpty()).isTrue();
  }
}
