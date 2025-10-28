package org.ton.ton4j.coverage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.ton.ton4j.bitstring.BitString;
import org.ton.ton4j.cell.Cell;
import org.ton.ton4j.cell.CellBuilder;
import org.ton.ton4j.cell.CellSlice;

@RunWith(JUnit4.class)
public class TestCellBuilderCoverageEvenMore {

  @Test
  public void testStoreStringAndSnakeString() {
    // storeString
    String s = "hello";
    Cell c = CellBuilder.beginCell().storeString(s).endCell();
    String hex = c.toHex();
    assertThat(hex).isNotEmpty();

    // storeSnakeString forces splitting into refs for long strings
    char[] bigChars = new char[300];
    Arrays.fill(bigChars, 'A');
    String big = new String(bigChars);
    Cell snake = CellBuilder.beginCell().storeSnakeString(big).endCell();
    // Expect at least one reference created due to chunking
    assertThat(snake.getUsedRefs()).isGreaterThan(0);
  }

  @Test
  public void testStoreBitStringExactPartialAndUnsafeAndOverflow() {
    BitString bs = new BitString(16);
    bs.writeBits("1010101010101010");

    Cell c1 = CellBuilder.beginCell().storeBitString(bs).endCell();
    assertThat(c1.getBitLength()).isEqualTo(16);

    BitString bs2 = new BitString(32);
    bs2.writeBits("11110000111100001111000011110000");
    Cell c2 = CellBuilder.beginCell().storeBitString(bs2, 12).endCell();
    assertThat(c2.getBitLength()).isEqualTo(12);

    // unsafe bypasses overflow check but still must keep within cell max bits -> use 1023
    BitString large = new BitString(1023);
    for (int i = 0; i < 1023; i++) large.writeBit(true);
    Cell c3 = CellBuilder.beginCell().storeBitStringUnsafe(large).endCell();
    assertThat(c3.getBitLength()).isEqualTo(1023);

    // overflow in safe storeBitString
    BitString tooLarge = new BitString(1100);
    for (int i = 0; i < 1100; i++) tooLarge.writeBit(false);
    assertThatThrownBy(() -> CellBuilder.beginCell().storeBitString(tooLarge))
        .isInstanceOf(Error.class)
        .hasMessageContaining("Bits overflow");
  }

  @Test
  public void testStoreBytesVariants() {
    byte[] data = "data".getBytes(StandardCharsets.UTF_8);
    int[] ints = new int[] {1, 2, 3, 4};
    java.util.ArrayList<Byte> bytesList = new java.util.ArrayList<>();
    for (byte b : data) { bytesList.add(b); }

    Cell c1 = CellBuilder.beginCell().storeBytes(data).endCell();
    Cell c2 = CellBuilder.beginCell().storeBytesUnlimited(data).endCell();
    Cell c3 = CellBuilder.beginCell().storeBytes(ints).endCell();
    Cell c4 = CellBuilder.beginCell().storeBytes(bytesList).endCell();

    assertThat(c1.getBitLength()).isEqualTo(data.length * 8);
    assertThat(c2.getBitLength()).isEqualTo(data.length * 8);
    assertThat(c3.getBitLength()).isEqualTo(ints.length * 8);
    assertThat(c4.getBitLength()).isEqualTo(data.length * 8);

    Cell c5 = CellBuilder.beginCell().storeBytes(data, 8).endCell();
    Cell c6 = CellBuilder.beginCell().storeBytes(ints, 8).endCell();
    // storeBytes(number, bitLength) validates overflow against bitLength but writes full payload
    assertThat(c5.getBitLength()).isEqualTo(data.length * 8);
    assertThat(c6.getBitLength()).isEqualTo(ints.length * 8);
  }

  @Test
  public void testStoreIntMaybeBoundaryAndNegative() {
    // null branch
    CellSlice s1 = CellSlice.beginParse(CellBuilder.beginCell().storeIntMaybe((BigInteger) null, 8).endCell());
    assertThat(s1.loadBit()).isFalse();

    // non-null min/max for 3 bits (-4..3)
    CellSlice s2 = CellSlice.beginParse(CellBuilder.beginCell().storeInt(BigInteger.valueOf(-4), 3).endCell());
    assertThat(s2.loadInt(3).intValue()).isEqualTo(-4);
    CellSlice s3 = CellSlice.beginParse(CellBuilder.beginCell().storeInt(BigInteger.valueOf(3), 3).endCell());
    assertThat(s3.loadInt(3).intValue()).isEqualTo(3);

    // negative check in storeUint via checkSign
    assertThatThrownBy(() -> CellBuilder.beginCell().storeUint(BigInteger.valueOf(-1), 8))
        .isInstanceOf(Error.class)
        .hasMessageContaining("must be unsigned");
  }

  @Test
  public void testRefsCountBoundaryAndOverflowWithSetRefsCount() {
    Cell r = CellBuilder.beginCell().endCell();
    CellBuilder b = CellBuilder.beginCell();
    // Add 4 refs (max allowed)
    b.storeRef(r).storeRef(r).storeRef(r).storeRef(r);
    // Adding one more should overflow
    assertThatThrownBy(() -> b.storeRef(r))
        .isInstanceOf(Error.class)
        .hasMessageContaining("Refs overflow");
  }

  @Test
  public void testVarUintOverloads() {
    Cell c1 = CellBuilder.beginCell().storeVarUint(BigInteger.valueOf(255)).endCell();
    assertThat(c1.getBitLength()).isGreaterThan(0);

    Cell c2 = CellBuilder.beginCell().storeVarUint((byte) 7, 8).endCell();
    assertThat(c2.getBitLength()).isGreaterThan(0);
  }

  @Test
  public void testFromBocIntArrayAndMultiRoot() {
    Cell orig = CellBuilder.beginCell().storeUint(0xAB, 8).endCell();
    byte[] boc = orig.toBoc();

    int[] unsigned = new int[boc.length];
    for (int i = 0; i < boc.length; i++) unsigned[i] = boc[i] & 0xFF;

    Cell c1 = CellBuilder.beginCell().fromBoc(unsigned).endCell();
    assertThat(c1.getBitLength()).isEqualTo(8);

    // Multi-root from same boc (though it's single root, method should still handle array)
    Cell c2 = CellBuilder.beginCell().fromBocMultiRoot(boc).endCell();
    assertThat(c2.print()).contains("x{");
  }
}
