package org.ton.ton4j.coverage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigInteger;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.ton.ton4j.bitstring.BitString;
import org.ton.ton4j.cell.Cell;
import org.ton.ton4j.cell.CellBuilder;
import org.ton.ton4j.cell.CellSlice;

@RunWith(JUnit4.class)
public class TestCellBuilderCoverageMore {

  @Test
  public void testStoreUintMaybeNullAndNonNull() {
    CellBuilder b = CellBuilder.beginCell();
    b.storeUintMaybe((BigInteger) null, 8);
    b.storeUintMaybe(BigInteger.TEN, 8);
    Cell c = b.endCell();
    CellSlice s = CellSlice.beginParse(c);
    // null branch wrote false, then non-null wrote true + 8 bits
    assertThat(s.loadBit()).isFalse();
    assertThat(s.loadBit()).isTrue();
    assertThat(s.loadUint(8).intValue()).isEqualTo(10);
  }

  @Test
  public void testStoreVarUintMaybeNullAndNonNull() {
    CellBuilder b = CellBuilder.beginCell();
    b.storeVarUintMaybe(null, 8);
    b.storeVarUintMaybe(BigInteger.valueOf(300), 16);
    Cell c = b.endCell();
    CellSlice s = CellSlice.beginParse(c);
    assertThat(s.loadBit()).isFalse();
    assertThat(s.loadBit()).isTrue();
    // For valueBits=16: writeVarUint uses bitLength(valueBits-1)=4 bits for length, and for 300
    // bytesSize=2, so we expect to read 2 from 4-bit field
    assertThat(s.loadUint(4).intValue()).isEqualTo(2);
  }

  @Test
  public void testStoreRefMaybeNullAndNonNull() {
    Cell ref = CellBuilder.beginCell().storeUint(7, 4).endCell();
    CellBuilder b = CellBuilder.beginCell();
    b.storeRefMaybe(null);
    b.storeRefMaybe(ref);
    Cell c = b.endCell();
    CellSlice s = CellSlice.beginParse(c);
    assertThat(s.loadBit()).isFalse();
    assertThat(s.loadBit()).isTrue();
    assertThat(s.loadRef().print()).contains("x{");
  }

  @Test
  public void testStoreSliceLazyAndSetRefsCount() {
    BitString bs = new BitString(4);
    bs.writeBits("1010");
    byte[] hashes = new byte[64]; // 2 refs (64/32)
    Cell c = CellBuilder.beginCell().storeSliceLazy(bs, hashes).setRefsCount(2).endCell();
    assertThat(c.getRefs().size()).isEqualTo(0); // lazy slice stores only hashes, no refs
    assertThat(c.getHashes().length).isEqualTo(64);
  }

  @Test
  public void testOverflowBitsAndRefsAndIntRange() {
    // Bits overflow
    assertThatThrownBy(() -> CellBuilder.beginCell().storeUint(1, 1024))
        .isInstanceOf(Error.class)
        .hasMessageContaining("Bits overflow");

    // Refs overflow (max 4)
    Cell r = CellBuilder.beginCell().endCell();
    assertThatThrownBy(
            () ->
                CellBuilder.beginCell()
                    .storeRef(r)
                    .storeRef(r)
                    .storeRef(r)
                    .storeRef(r)
                    .storeRef(r))
        .isInstanceOf(Error.class)
        .hasMessageContaining("Refs overflow");

    // storeInt out of range
    assertThatThrownBy(() -> CellBuilder.beginCell().storeInt(BigInteger.valueOf(4), 2))
        .isInstanceOf(Error.class)
        .hasMessageContaining("Can't store an Int");
  }

  @Test
  public void testFromBocOverloads() {
    Cell orig = CellBuilder.beginCell().storeUint(0xAA, 8).endCell();
    byte[] boc = orig.toBoc();

    // byte[]
    Cell c1 = CellBuilder.beginCell().fromBoc(boc).endCell();
    assertThat(c1.print()).contains("x{");

    // hex
    String hex = orig.toHex();
    Cell c2 = CellBuilder.beginCell().fromBoc(hex).endCell();
    assertThat(c2.print()).contains("x{");

    // base64
    String b64 = orig.toBase64();
    Cell c3 = CellBuilder.beginCell().fromBocBase64(b64).endCell();
    assertThat(c3.print()).contains("x{");
  }
}
