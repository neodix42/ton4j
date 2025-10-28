package org.ton.ton4j.coverage;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.ton.ton4j.bitstring.BitString;
import org.ton.ton4j.cell.Cell;
import org.ton.ton4j.cell.CellBuilder;
import org.ton.ton4j.cell.CellSlice;
import org.ton.ton4j.cell.CellType;
import org.ton.ton4j.cell.LevelMask;

@RunWith(JUnit4.class)
public class TestCellBuilderStoreAllMethods {

  @Test
  public void testStoreBitsOverloadsAndTypesAndEndCells() {
    // storeBit, storeBits(String), storeBits(List<Boolean>), storeBits(Boolean[])
    List<Boolean> listBits = Arrays.asList(true, false, true);
    Boolean[] arrayBits = new Boolean[] {false, false, true, true};

    CellBuilder b = CellBuilder.beginCell();
    b.storeBit(true)
        .storeBits("01")
        .storeBits(listBits)
        .storeBits(arrayBits)
        .cellType(CellType.ORDINARY)
        .setExotic(false)
        .setLevelMask(new LevelMask(0));

    Cell c = b.endCell();
    assertThat(c.getBitLength()).isGreaterThan(0);

    // endCells path: accumulate multiple cells
    CellBuilder multi = CellBuilder.beginCell();
    multi.storeUint(1, 1);
    Cell c1 = multi.endCell();
    Cell c2 = CellBuilder.beginCell().storeUint(0, 1).endCell();
    // emulate list path by adding a second cell into builder's internal list via storeRefs
    Cell finalCell = CellBuilder.beginCell().storeRefs(c1, c2).endCell();
    assertThat(finalCell.getUsedRefs()).isEqualTo(2);
  }

  @Test
  public void testStoreUintOverloads() {
    Cell c =
        CellBuilder.beginCell()
            .storeUint(1L, 8)
            .storeUintMaybe(2L, 8)
            .storeUint(3, 8)
            .storeUintMaybe(4, 8)
            .storeUint((short) 5, 8)
            .storeUintMaybe((short) 6, 8)
            .storeUint(Byte.valueOf((byte) 7), 8)
            .storeUintMaybe(Byte.valueOf((byte) 8), 8)
            .storeUint("9", 8)
            .storeUintMaybe("10", 8)
            .storeUint(BigInteger.valueOf(11), 8)
            .storeUintMaybe(BigInteger.valueOf(12), 8)
            .endCell();
    assertThat(c.getBitLength()).isGreaterThan(0);
  }

  @Test
  public void testStoreIntOverloadsAndListAndRefsAndArrays() {
    // storeInt overloads
    CellBuilder b = CellBuilder.beginCell();
    b.storeInt(1L, 4)
        .storeIntMaybe(2L, 4)
        .storeInt(3, 4)
        .storeIntMaybe(1, 4)
        .storeInt((short) 2, 4)
        .storeIntMaybe((short) 1, 4)
        .storeInt((byte) 1, 4)
        .storeIntMaybe((byte) 0, 4)
        .storeInt(BigInteger.ZERO, 4)
        .storeIntMaybe(BigInteger.ONE, 4);

    // storeList
    List<BigInteger> lst = new ArrayList<>();
    lst.add(BigInteger.valueOf(3));
    lst.add(BigInteger.valueOf(2));
    b.storeList(lst, 4);

    // storeRefs(List) and storeRefs(varargs)
    Cell r1 = CellBuilder.beginCell().endCell();
    Cell r2 = CellBuilder.beginCell().endCell();
    List<Cell> rlist = Arrays.asList(r1, r2);
    b.storeRefs(rlist);
    b.storeRefs(r1, r2);

    // toSigned/Unsigned arrays
    int[] unsignedBytes = b.toUnsignedByteArray();
    byte[] signedBytes = b.toSignedByteArray();

    Cell c = b.endCell();
    assertThat(unsignedBytes.length).isGreaterThan(0);
    assertThat(signedBytes.length).isGreaterThan(0);
    assertThat(c.getUsedRefs()).isEqualTo(4);
  }

  @Test
  public void testStoreBytesOverloads() {
    byte[] bytes = new byte[] {0x01, 0x02};
    int[] ints = new int[] {0x03, 0x04};
    List<Byte> list = Arrays.asList((byte) 0x05, (byte) 0x06);

    Cell c1 = CellBuilder.beginCell().storeBytes(bytes).endCell();
    Cell c2 = CellBuilder.beginCell().storeBytesUnlimited(bytes).endCell();
    Cell c3 = CellBuilder.beginCell().storeBytes(ints).endCell();
    Cell c4 = CellBuilder.beginCell().storeBytes(list).endCell();
    Cell c5 = CellBuilder.beginCell().storeBytes(bytes, 16).endCell();
    Cell c6 = CellBuilder.beginCell().storeBytes(ints, 16).endCell();

    assertThat(c1.getBitLength()).isEqualTo(16);
    assertThat(c2.getBitLength()).isEqualTo(16);
    assertThat(c3.getBitLength()).isEqualTo(16);
    assertThat(c4.getBitLength()).isEqualTo(16);
    assertThat(c5.getBitLength()).isEqualTo(16);
    assertThat(c6.getBitLength()).isEqualTo(16);
  }

  @Test
  public void testStoreSliceAndStoreCellMaybe() {
    Cell ref = CellBuilder.beginCell().storeUint(1, 1).endCell();
    Cell src = CellBuilder.beginCell().storeUint(0xF, 4).storeRef(ref).endCell();
    Cell target = CellBuilder.beginCell().storeSlice(CellSlice.beginParse(src)).endCell();
    assertThat(target.getBitLength()).isEqualTo(4);
    assertThat(target.getUsedRefs()).isEqualTo(1);

    Cell maybe = CellBuilder.beginCell().storeCellMaybe(null).storeCellMaybe(ref).endCell();
    CellSlice s = CellSlice.beginParse(maybe);
    assertThat(s.loadBit()).isFalse();
    assertThat(s.loadBit()).isTrue();
  }
}
