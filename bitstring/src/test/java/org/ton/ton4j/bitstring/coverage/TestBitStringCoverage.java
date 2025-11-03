package org.ton.ton4j.bitstring.coverage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertThrows;

import java.lang.reflect.Method;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.ton.ton4j.address.Address;
import org.ton.ton4j.bitstring.BitString;
import org.ton.ton4j.utils.Utils;

@RunWith(JUnit4.class)
public class TestBitStringCoverage {

  @Test
  public void testConstructorsAndBasics() {
    BitString def = new BitString();
    assertThat(def.getFreeBits()).isEqualTo(1023);
    assertThat(def.getUsedBits()).isEqualTo(0);
    assertThat(def.getUsedBytes()).isEqualTo(0);
    assertThat(def.toHex()).isEqualTo("");
    // Cover empty conversions
    assertThat(def.toByteArray()).isEmpty();
    assertThat(def.toUnsignedByteArray()).isEmpty();
    assertThat(def.toByteList()).isEmpty();

    BitString byLen = new BitString(4);
    byLen.writeBits("1010");
    assertThat(byLen.toBitString()).isEqualTo("1010");
    assertThat(byLen.toHex()).isEqualTo("A");

    BitString fromBytes = new BitString(new byte[] {0x0F, (byte) 0x80});
    assertThat(fromBytes.toBitString()).isEqualTo("0000111110000000");

    BitString fromBytesSized = new BitString(new byte[] {0x0F, (byte) 0x80}, 9);
    assertThat(fromBytesSized.toBitString()).isEqualTo("000011111");

    // Cover byte[]+size constructor when bytes is empty
    BitString fromEmptyBytesSized = new BitString(new byte[] {}, 0);
    assertThat(fromEmptyBytesSized.toBitString()).isEqualTo("");

    BitString fromInts = new BitString(new int[] {0x01, 0xFF});
    assertThat(fromInts.toBitString()).isEqualTo("0000000111111111");

    BitString emptyBytes = new BitString(new byte[] {});
    assertThat(emptyBytes.toBitString()).isEqualTo("");

    BitString emptyInts = new BitString(new int[] {});
    assertThat(emptyInts.toBitString()).isEqualTo("");

    BitString copy = new BitString(byLen);
    assertThat(copy.toBitString()).isEqualTo("1010");
  }

  @Test
  public void testGetAndCheckRange() {
    BitString bs = new BitString(2);
    bs.writeBits("10");
    assertThat(bs.get(0)).isTrue();
    assertThat(bs.get(1)).isFalse();
    assertThrows(Error.class, () -> bs.get(2));
  }

  @Test
  public void testWriteBitVariantsAndArrays() {
    BitString bs = new BitString(32);
    // Boolean
    bs.writeBit(Boolean.TRUE);
    bs.writeBit(Boolean.FALSE);
    // byte via writeBitArray(byte[])
    bs.writeBitArray(new byte[] {1, 0, -1});
    // boolean[]
    bs.writeBitArray(new boolean[] {true, false});
    // Boolean[]
    bs.writeBitArray(new Boolean[] {Boolean.TRUE, Boolean.FALSE});
    // writeBits
    bs.writeBits("10");

    assertThat(bs.toBitString()).isEqualTo("10100101010");
  }

  @Test
  public void testWriteReadNumbersAndEdgeCases() {
    // writeUint BigInteger path with errors and zero
    BitString bs = new BitString(128);
    assertThrows(Error.class, () -> bs.writeUint(new BigInteger("-1"), 8));
    // zero with zero bitLength should be a no-op
    int before = bs.writeCursor;
    bs.writeUint(BigInteger.ZERO, 0);
    assertThat(bs.writeCursor).isEqualTo(before);
    // too small bitLength
    assertThrows(Error.class, () -> bs.writeUint(BigInteger.valueOf(256), 8));

    // writeUint long overload
    bs.writeUint(7L, 3);
    assertThat(bs.preReadUint(3).intValue()).isEqualTo(7);

    // writeInt special 1-bit cases and errors
    BitString i1 = new BitString(8);
    i1.writeInt(BigInteger.valueOf(-1), 1);
    assertThat(i1.toBitString()).isEqualTo("1");
    BitString i0 = new BitString(8);
    i0.writeInt(BigInteger.ZERO, 1);
    assertThat(i0.toBitString()).isEqualTo("0");
    BitString ie = new BitString(8);
    assertThrows(Error.class, () -> ie.writeInt(BigInteger.ONE, 1));

    // writeInt positive/negative general path and then read back
    BitString in = new BitString(32);
    in.writeInt(BigInteger.valueOf(-20), 8);
    in.writeInt(BigInteger.valueOf(20), 8);
    assertThat(in.readInt(8).intValue()).isEqualTo(-20);
    assertThat(in.readInt(8).intValue()).isEqualTo(20);

    // writeUint8 and writeBytes (byte[] and int[])
    BitString bb = new BitString(64);
    bb.writeUint8(0x7F);
    bb.writeBytes(new byte[] {(byte) 0x80, 0x01});
    bb.writeBytes(new int[] {0xFF});
    assertThat(Utils.bytesToHex(bb.toByteArray()).toUpperCase()).isEqualTo("7F8001FF");

    // writeVarUint
    BitString vu = new BitString(64);
    vu.writeVarUint(new BigInteger("65535"), 16);
    // first write is the length (in bytes) encoded into (valueBits-1) length, then the value
    BigInteger lenField = vu.preReadUint(4); // 16-1 -> bitLength 4
    assertThat(lenField.intValue()).isEqualTo(2);
  }

  @Test
  public void testCoins() {
    BitString c0 = new BitString(8);
    c0.writeCoins(BigInteger.ZERO);
    assertThat(c0.toBitString()).isEqualTo("0000");

    BitString cp = new BitString(64);
    cp.writeCoins(BigInteger.valueOf(255));
    // 255 -> needs 1 byte: length 1 (4 bits) + value 8 bits
    assertThat(cp.preReadUint(4).intValue()).isEqualTo(1);

    assertThrows(Error.class, () -> new BitString(8).writeCoins(BigInteger.valueOf(-1)));

    // too big amount (>= 2^120)
    BigInteger tooBig = BigInteger.valueOf(2).pow(121);
    assertThrows(Error.class, () -> new BitString(2048).writeCoins(tooBig));
  }

  @Test
  public void testAddressReadWriteAndNull() {
    Address addr = Address.of("0QAs9VlT6S776tq3unJcP5Ogsj-ELLunLXuOb1EKcOQi4-QO");
    BitString bs = new BitString(400);
    bs.writeAddress(addr);
    bs.writeAddress(null); // null branch

    // Read the first address back
    bs.readBits(0); // no-op
    Address readBack = bs.readAddress();
    assertThat(readBack.toRaw()).isEqualTo(addr.toRaw());

    // For null, preReadUint sees 0 and method returns null after consuming 2 bits
    Address none = bs.readAddress();
    assertThat(none).isNull();
  }

  @Test
  public void testReadPrimitivesAndSlices() {
    BitString bs = new BitString(256);
    bs.writeUint(0xAA, 8);
    bs.writeUint(0x55, 8);
    bs.writeUint(0x1234, 16);
    bs.writeUint(0xDEADBEEFL, 32);

    assertThat(bs.prereadBit()).isTrue(); // first bit of 0xAA is 1
    assertThat(bs.readBit()).isTrue();

    BitString pre = bs.preReadBits(7); // should not move cursor
    assertThat(pre.getLength()).isGreaterThan(0);
    int afterPre = bs.readCursor;
    BitString read7 = bs.readBits(7); // complete the first byte
    assertThat(read7.toByteArray().length).isEqualTo(1);
    assertThat(bs.readCursor).isEqualTo(afterPre + 7);

    BitString rest = bs.readBits(); // read remaining
    assertThat(rest.toByteArray().length).isGreaterThan(0);
  }

  @Test
  public void testPreReadAndReadUintIntErrorsAndHelpers() {
    BitString bs = new BitString(16);
    assertThrows(Error.class, () -> new BitString(16).preReadUint(0));
    assertThrows(Error.class, () -> new BitString(16).readUint(0));
    assertThrows(Error.class, () -> new BitString(16).readInt(0));

    bs.writeInt(BigInteger.valueOf(-1), 8);
    BigInteger i8 = bs.readInt8();
    assertThat(i8.intValue()).isEqualTo(-1);

    // refill
    bs = new BitString(64);
    bs.writeUint(0xFF, 8);
    bs.writeUint(0xFFFF, 16);
    bs.writeInt(BigInteger.valueOf(-2), 8);

    assertThat(bs.readUint8().intValue()).isEqualTo(0xFF);
    assertThat(bs.readUint16().intValue()).isEqualTo(0xFFFF);
    assertThat(bs.readUint32()).isEqualTo(new BigInteger("4261412864")); // reads 0xFE then pads zeros -> 0xFE000000
    // Put 64-bit patterns
    BitString u64 = new BitString(64);
    u64.writeUint(new BigInteger("9223372036854775807"), 64);
    assertThat(u64.readUint64().toString()).isEqualTo("9223372036854775807");

    BitString i64 = new BitString(64);
    i64.writeInt(new BigInteger("-1"), 64);
    assertThat(i64.readInt64().toString()).isEqualTo("-1");
  }

  @Test
  public void testStringAndBytesReadWrite() {
    String s = "Hello";
    BitString bs = new BitString(8 * s.length());
    bs.writeString(s);
    assertThat(new String(bs.readBytes(8 * s.length()), StandardCharsets.UTF_8)).isEqualTo(s);

    bs = new BitString(8 * s.length());
    bs.writeString(s);
    assertThat(bs.readString(8 * s.length())).isEqualTo(s);
  }

  @Test
  public void testConversionsAndCloning() throws Exception {
    BitString bs = new BitString();
    for (int i = 0; i < 10; i++) bs.writeBit(i % 2 == 0);

    assertThat(bs.toString()).isEqualTo(bs.toBitString());
    assertThat(bs.getBitString()).isEqualTo(bs.toBitString());

    byte[] bytes = bs.toByteArray();
    assertThat(bytes.length).isEqualTo((bs.writeCursor + 7) / 8);

    int[] unsigned = bs.toUnsignedByteArray();
    assertThat(unsigned.length).isEqualTo(bytes.length);

    byte[] signed = bs.toSignedByteArray();
    assertThat(Arrays.equals(signed, bytes)).isTrue();

    List<BigInteger> list = bs.toByteList();
    assertThat(list.size()).isEqualTo((bs.writeCursor + 7) / 8);

    boolean[] bitArr = bs.toBitArray();
    int[] zo = bs.toZeroOneArray();
    assertThat(bitArr.length).isEqualTo(bs.writeCursor);
    assertThat(zo.length).isEqualTo(bs.writeCursor);

    BitString cloned = bs.clone();
    assertThat(cloned.toBitString()).isEqualTo(bs.toBitString());

    BitString clonedFrom = bs.cloneFrom(1);
    assertThat(clonedFrom.writeCursor).isEqualTo(bs.writeCursor - 8);

    BitString cleared = bs.cloneClear();
    assertThat(cleared.writeCursor).isEqualTo(0);

    // toHex branches: exact bytes, nibble padding, and empty already tested
    BitString hex1 = new BitString(8);
    hex1.writeUint(0xAB, 8);
    assertThat(hex1.toHex()).isEqualTo("AB");
    BitString hex4 = new BitString(4);
    hex4.writeUint(0xF, 4);
    assertThat(hex4.toHex()).isEqualTo("F");
    BitString hexPad = new BitString(6);
    hexPad.writeBits("1111");
    hexPad.writeBits("11"); // causes padding path and trailing '_'
    assertThat(hexPad.toHex()).endsWith("_");
  }

  @Test
  public void testTryCatchBranchesUsingReflection() throws Exception {
    // on/off/toggle methods are package-private; use reflection to invoke and trigger exceptions
    BitString bs = new BitString(8);

    Method on = BitString.class.getDeclaredMethod("on", int.class);
    on.setAccessible(true);
    Method off = BitString.class.getDeclaredMethod("off", int.class);
    off.setAccessible(true);
    Method toggle = BitString.class.getDeclaredMethod("toggle", int.class);
    toggle.setAccessible(true);

    // Negative index should cause IndexOutOfBoundsException internally and be swallowed
    on.invoke(bs, -1);
    off.invoke(bs, -1);
    toggle.invoke(bs, -1);

    // Also exercise writeBit calling on/off normally
    bs.writeCursor = -1; // force Exception path inside on/off
    bs.writeBit(true);
    bs.writeCursor = -1;
    bs.writeBit(false);

    // Cover normal toggle path as well
    BitString b2 = new BitString(2);
    b2.writeBits("10");
    assertThat(b2.get(0)).isTrue();
    toggle.invoke(b2, 0); // flip first bit
    assertThat(b2.get(0)).isFalse();
  }

  @Test
  public void testWriteBitStringAndToBooleanArray() {
    BitString src = new BitString(8);
    src.writeBits("10110011");
    BitString dst = new BitString(16);
    dst.writeBitString(src);
    assertThat(dst.toBitString()).isEqualTo(src.toBitString());

    // toBooleanArray should produce an array of size getLength with first writeCursor entries filled
    Boolean[] arr = dst.toBooleanArray();
    assertThat(arr.length).isGreaterThanOrEqualTo(dst.writeCursor);
    for (int i = 0; i < dst.writeCursor; i++) {
      assertThat(arr[i]).isEqualTo(dst.get(i));
    }
  }

  @Test
  public void testReadIntOneBitBranches() {
    // sign bit = 0, expect 0
    BitString z = new BitString(1);
    z.writeBit(false);
    assertThat(z.readInt(1)).isEqualTo(BigInteger.ZERO);

    // sign bit = 1, expect -1
    BitString m1 = new BitString(1);
    m1.writeBit(true);
    assertThat(m1.readInt(1)).isEqualTo(new BigInteger("-1"));
  }
}
