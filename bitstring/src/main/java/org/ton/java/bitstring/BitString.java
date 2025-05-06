package org.ton.java.bitstring;

import static java.util.Objects.isNull;

import java.io.Serializable;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;
import org.ton.java.address.Address;
import org.ton.java.utils.Utils;

/**
 * Each element is one bit in memory. Interestingly, but this solution loses to Deque<Boolean> array
 * solution from memory footprint allocation.
 */
public class BitString implements Serializable {
  private static final Logger log = Logger.getLogger(BitString.class.getName());
  byte[] array;
  public int writeCursor;
  public int readCursor;
  public int length;

  public BitString() {
    array = new byte[1023];
    writeCursor = 0;
    readCursor = 0;
    length = 1023 * 8; // Each byte holds 8 bits
  }

  public BitString(BitString bs) {
    for (int i = bs.readCursor; i < bs.writeCursor; i++) {
      writeBit(bs.get(i));
    }
  }

  /**
   * Create RealBitString limited by length
   *
   * @param length int length of RealBitString in bits
   */
  public BitString(int length) {
    array = new byte[(int) Math.ceil(length / (double) 8)];
    writeCursor = 0;
    readCursor = 0;
    this.length = length;
  }

  /**
   * Create RealBitString from byte array
   *
   * @param bytes byte[] array of bytes
   */
  public BitString(byte[] bytes) {
    this(Utils.signedBytesToUnsigned(bytes));
  }

  /**
   * Create RealBitString from byte array with specified size
   *
   * @param bytes byte[] array of bytes
   * @param size int number of bits to read
   */
  public BitString(byte[] bytes, int size) {
    if (bytes.length == 0) {
      array = new byte[0];
      writeCursor = 0;
      readCursor = 0;
      length = 0;
    } else {
      byte[] bits =
          Utils.leftPadBytes(
              Utils.bytesToBitString(bytes).getBytes(StandardCharsets.UTF_8),
              bytes.length * 8,
              '0');

      length = size;
      array = new byte[(int) Math.ceil(length / (double) 8)];
      writeCursor = 0;
      readCursor = 0;

      for (int i = 0; i < size && i < bits.length; i++) { // specified length
        if (bits[i] == (byte) '1') {
          writeBit(true);
        } else if (bits[i] == (byte) '0') {
          writeBit(false);
        } else {
          // else '-' sign - do nothing
        }
      }
    }
  }

  /**
   * Create RealBitString from int array
   *
   * @param bytes int[] array of bytes
   */
  public BitString(int[] bytes) {
    if (bytes.length == 0) {
      array = new byte[0];
      writeCursor = 0;
      readCursor = 0;
      length = 0;
    } else {
      byte[] bits =
          Utils.leftPadBytes(
              Utils.bytesToBitString(bytes).getBytes(StandardCharsets.UTF_8),
              bytes.length * 8,
              '0');

      length = bits.length;
      array = new byte[(int) Math.ceil(length / (double) 8)];
      writeCursor = 0;
      readCursor = 0;

      for (byte bit : bits) { // whole length
        if (bit == (byte) '1') {
          writeBit(true);
        } else if (bit == (byte) '0') {
          writeBit(false);
        } else {
          // else '-' sign - do nothing
        }
      }
    }
  }

  /**
   * Return free bits, that derives from total length minus bits written
   *
   * @return int
   */
  public int getFreeBits() {
    return length - writeCursor;
  }

  /**
   * Returns used bits, i.e. last position of writeCursor
   *
   * @return int
   */
  public int getUsedBits() {
    return writeCursor;
  }

  /**
   * @return int
   */
  public int getUsedBytes() {
    return (int) Math.ceil(writeCursor / (double) 8);
  }

  /**
   * Return bit's value at position n
   *
   * @param n int
   * @return boolean bit value at position `n`
   */
  public boolean get(int n) {
    checkRange(n);
    return (array[(n / 8)] & (1 << (7 - (n % 8)))) > 0;
  }

  /**
   * Check if bit at position n is reachable
   *
   * @param n int
   */
  private void checkRange(int n) {
    if (n > length) {
      throw new Error("RealBitString overflow. n[" + n + "] >= length[" + length + "]");
    }
  }

  /**
   * Set bit value to 1 at position n
   *
   * @param n int
   */
  void on(int n) {
    checkRange(n);
    array[(n / 8)] |= 1 << (7 - (n % 8));
  }

  /**
   * Set bit value to 0 at position n
   *
   * @param n int
   */
  void off(int n) {
    //        checkRange(n);
    array[(n / 8)] &= ~(1 << (7 - (n % 8)));
  }

  /**
   * Toggle bit value at position n
   *
   * @param n int
   */
  void toggle(int n) {
    //    this.checkRange(n);
    array[(n / 8)] ^= 1 << (7 - (n % 8));
  }

  /**
   * Write bit and increase cursor
   *
   * @param b boolean
   */
  //  public void writeBit(boolean b) {
  //    if (b) {
  //      on(writeCursor);
  //    } else {
  //      off(writeCursor);
  //    }
  //    writeCursor++;
  //  }

  public void writeBit(Boolean b) {
    if (b) {
      on(writeCursor);
    } else {
      off(writeCursor);
    }
    writeCursor++;
  }

  /**
   * Write bit and increase cursor
   *
   * @param b byte
   */
  void writeBit(byte b) {
    if ((b) > 0) {
      on(writeCursor);
    } else {
      off(writeCursor);
    }
    writeCursor++;
  }

  /**
   * @param ba Boolean[]
   */
  public void writeBitArray(Boolean[] ba) {
    for (Boolean b : ba) {
      writeBit(b);
    }
  }

  /**
   * @param ba boolean[]
   */
  public void writeBitArray(boolean[] ba) {
    for (boolean b : ba) {
      writeBit(b);
    }
  }

  /**
   * @param ba byte[]
   */
  public void writeBitArray(byte[] ba) {
    for (byte b : ba) {
      writeBit(b);
    }
  }

  /**
   * Write bits from a string representation (e.g., "1010")
   *
   * @param b String containing '0' and '1' characters
   */
  public void writeBits(String b) {
    for (Character c : b.toCharArray()) {
      writeBit(c == '1');
    }
  }

  /**
   * Write unsigned int
   *
   * @param number BigInteger
   * @param bitLength int size of uint in bits
   */
  public void writeUint(BigInteger number, int bitLength) {
    if (number.compareTo(BigInteger.ZERO) < 0) {
      throw new Error("Unsigned number cannot be less than 0");
    }
    if (bitLength == 0 || (number.bitLength() > bitLength)) {
      if (number.compareTo(BigInteger.ZERO) == 0) {
        return;
      }
      throw new Error(
          "bitLength is too small for number, got number=" + number + ", bitLength=" + bitLength);
    }

    String s = number.toString(2);

    if (s.length() != bitLength) {
      s = repeatZeros(bitLength - s.length()) + s;
    }

    for (int i = 0; i < bitLength; i++) {
      writeBit(s.charAt(i) == '1');
    }
  }

  public String repeatZeros(int count) {
    char[] zeros = new char[count];
    Arrays.fill(zeros, '0');
    return new String(zeros);
  }

  /**
   * Write unsigned int
   *
   * @param number value
   * @param bitLength size of uint in bits
   */
  public void writeUint(long number, int bitLength) {
    writeUint(BigInteger.valueOf(number), bitLength);
  }

  /**
   * Write signed int
   *
   * @param number BigInteger
   * @param bitLength int size of int in bits
   */
  public void writeInt(BigInteger number, int bitLength) {
    if (bitLength == 1) {
      if (number.compareTo(BigInteger.valueOf(-1)) == 0) {
        writeBit(true);
        return;
      }
      if (number.compareTo(BigInteger.ZERO) == 0) {
        writeBit(false);
        return;
      }
      throw new Error("bitLength is too small for number");
    } else {
      if (number.signum() == -1) {
        writeBit(true);
        BigInteger b = BigInteger.valueOf(2);
        BigInteger nb = b.pow(bitLength - 1);
        writeUint(nb.add(number), bitLength - 1);
      } else {
        writeBit(false);
        writeUint(number, bitLength - 1);
      }
    }
  }

  /**
   * Write unsigned 8-bit int
   *
   * @param ui8 int
   */
  public void writeUint8(int ui8) {
    writeUint(BigInteger.valueOf(ui8), 8);
  }

  /**
   * Write array of unsigned 8-bit ints
   *
   * @param ui8 byte[]
   */
  public void writeBytes(byte[] ui8) {
    for (byte b : ui8) {
      writeUint8(b & 0xff);
    }
  }

  /**
   * Write array of signed 8-bit ints
   *
   * @param ui8 byte[]
   */
  public void writeBytes(int[] ui8) {
    for (int b : ui8) {
      writeUint8(b);
    }
  }

  /**
   * Write UTF-8 string
   *
   * @param value String
   */
  public void writeString(String value) {
    writeBytes(value.getBytes(StandardCharsets.UTF_8));
  }

  /**
   * @param amount positive BigInteger in nano-coins
   */
  public void writeCoins(BigInteger amount) {
    if (amount.signum() == -1) {
      throw new Error("Coins value must be positive.");
    }

    if (amount.compareTo(BigInteger.ZERO) == 0) {
      writeUint(BigInteger.ZERO, 4);
    } else {
      int bytesSize = (int) Math.ceil((amount.bitLength() / (double) 8));
      if (bytesSize >= 16) {
        throw new Error("Amount is too big. Maximum amount 2^120-1");
      }
      writeUint(BigInteger.valueOf(bytesSize), 4);
      writeUint(amount, bytesSize * 8);
    }
  }

  /**
   * Write variable-length unsigned integer
   *
   * @param value BigInteger value to write
   * @param bitLength int size of length field in bits
   */
  public void writeVarUint(BigInteger value, int bitLength) {
    if (value.compareTo(BigInteger.ZERO) == 0) {
      writeUint(BigInteger.ZERO, bitLength);
    } else {
      int bytesSize = (value.bitLength() + 7) / 8;
      if (bytesSize > bitLength) {
        throw new Error("Amount is too big. Should fit in " + bitLength + " bits");
      }
      writeUint(BigInteger.valueOf(bytesSize), bitLength);
      writeUint(value, bytesSize * 8);
    }
  }

  /**
   * Appends RealBitString with Address addr_none$00 = MsgAddressExt; addr_std$10 anycast:(Maybe
   * Anycast) workchain_id:int8 address:uint256 = MsgAddressInt;
   *
   * @param address Address
   */
  public void writeAddress(Address address) {
    if (isNull(address)) {
      writeUint(0, 2);
    } else {
      writeUint(2, 2);
      writeUint(0, 1);
      writeInt(BigInteger.valueOf(address.wc), 8);
      writeBytes(address.hashPart);
    }
  }

  /**
   * Write another RealBitString to this RealBitString
   *
   * @param anotherBitString RealBitString
   */
  public void writeBitString(BitString anotherBitString) {
    for (int i = anotherBitString.readCursor; i < anotherBitString.writeCursor; i++) {
      writeBit(anotherBitString.get(i));
    }
  }

  /**
   * Read one bit without moving readCursor
   *
   * @return true or false
   */
  public boolean prereadBit() {
    return get(readCursor);
  }

  //  /**
  //   * Read one bit and moves readCursor forward by one position
  //   *
  //   * @return true or false
  //   */
  //  public boolean readBit() {
  //    boolean result = get(readCursor);
  //    readCursor++;
  //    //        if (readCursor > writeCursor) {
  //    //            throw new Error("Parse error: not enough bits. readCursor > writeCursor");
  //    //        }
  //    return result;
  //  }

  public Boolean readBit() {
    Boolean result = get(readCursor);
    readCursor++;
    //        if (readCursor > writeCursor) {
    //            throw new Error("Parse error: not enough bits. readCursor > writeCursor");
    //        }
    return result;
  }

  /**
   * Read n bits from the RealBitString
   *
   * @param n integer
   * @return RealBitString with length n read from original RealBitString
   */
  public BitString preReadBits(int n) {
    int oldReadCursor = readCursor;
    BitString result = new BitString(n);
    for (int i = 0; i < n; i++) {
      result.writeBit(readBit());
    }
    readCursor = oldReadCursor;
    return result;
  }

  /**
   * Read n bits from the RealBitString
   *
   * @param n integer
   * @return RealBitString with length n read from original RealBitString
   */
  public BitString readBits(int n) {
    BitString result = new BitString(n);
    for (int i = 0; i < n; i++) {
      result.writeBit(readBit());
    }
    return result;
  }

  /**
   * Read rest of bits from the RealBitString
   *
   * @return RealBitString with length n read from original RealBitString
   */
  public BitString readBits() {
    BitString result = new BitString(); // todo
    for (int i = 0; i < writeCursor; i++) {
      result.writeBit(readBit());
    }
    return result;
  }

  /**
   * Read bits of bitLength without moving readCursor, i.e. modifying RealBitString
   *
   * @param bitLength length in bits
   * @return BigInteger
   */
  public BigInteger preReadUint(int bitLength) {
    int oldReadCursor = readCursor;

    if (bitLength < 1) {
      throw new Error("Incorrect bitLength");
    }
    StringBuilder s = new StringBuilder();
    for (int i = 0; i < bitLength; i++) {
      boolean b = readBit();
      if (b) {
        s.append("1");
      } else {
        s.append("0");
      }
    }
    readCursor = oldReadCursor;
    return new BigInteger(s.toString(), 2);
  }

  /**
   * Read unsigned int of bitLength
   *
   * @param bitLength int bitLength Size of uint in bits
   * @return BigInteger
   */
  public BigInteger readUint(int bitLength) {
    if (bitLength < 1) {
      throw new Error("Incorrect bitLength");
    }
    StringBuilder s = new StringBuilder();
    for (int i = 0; i < bitLength; i++) {
      boolean b = readBit();
      if (b) {
        s.append("1");
      } else {
        s.append("0");
      }
    }
    return new BigInteger(s.toString(), 2);
  }

  /**
   * Read signed int of bitLength
   *
   * @param bitLength int bitLength Size of signed int in bits
   * @return BigInteger
   */
  public BigInteger readInt(int bitLength) {
    if (bitLength < 1) {
      throw new Error("Incorrect bitLength");
    }

    boolean sign = readBit();
    if (bitLength == 1) {
      return sign ? new BigInteger("-1") : BigInteger.ZERO;
    }

    BigInteger number = readUint(bitLength - 1);
    if (sign) {
      BigInteger b = BigInteger.valueOf(2);
      BigInteger nb = b.pow(bitLength - 1);
      number = number.subtract(nb);
    }
    return number;
  }

  public BigInteger readUint8() {
    return readUint(8);
  }

  public BigInteger readUint16() {
    return readUint(16);
  }

  public BigInteger readUint32() {
    return readUint(32);
  }

  public BigInteger readUint64() {
    return readUint(64);
  }

  public BigInteger readInt8() {
    return readInt(8);
  }

  public BigInteger readInt16() {
    return readInt(16);
  }

  public BigInteger readInt32() {
    return readInt(32);
  }

  public BigInteger readInt64() {
    return readInt(64);
  }

  public Address readAddress() {
    BigInteger i = preReadUint(2);
    if (i.intValue() == 0) {
      readBits(2);
      return null;
    }
    readBits(2);
    readBits(1);
    int workchain = readInt(8).intValue();
    BigInteger hashPart = readUint(256);

    String address =
        workchain + ":" + String.format("%64s", hashPart.toString(16)).replace(' ', '0');
    return Address.of(address);
  }

  public String readString(int length) {
    BitString BitString = readBits(length);
    return new String(BitString.toByteArray());
  }

  /**
   * @param length in bits
   * @return byte array
   */
  public byte[] readBytes(int length) {
    BitString BitString = readBits(length);
    return BitString.toByteArray();
  }

  /**
   * @return hex string
   */
  public String toString() {
    return toBitString();
  }

  /**
   * @return RealBitString from 0 to writeCursor
   */
  public String toBitString() {
    StringBuilder s = new StringBuilder();
    for (int i = 0; i < writeCursor; i++) {
      char bit = get(i) ? '1' : '0';
      s.append(bit);
    }
    return s.toString();
  }

  public Boolean[] toBooleanArray() {
    Boolean[] result = new Boolean[getLength()];
    for (int i = 0; i < writeCursor; i++) {
      result[i++] = get(i);
    }
    return result;
  }

  public int getLength() {
    return array.length * 8;
  }

  /**
   * @return RealBitString from current position to writeCursor
   */
  public String getBitString() {
    StringBuilder s = new StringBuilder();
    for (int i = readCursor; i < writeCursor; i++) {
      char bit = get(i) ? '1' : '0';
      s.append(bit);
    }
    return s.toString();
  }

  public byte[] toByteArray() {
    return array.clone();
  }

  /**
   * Convert to unsigned byte array (int[] where each element is 0-255)
   *
   * @return int[] array of unsigned bytes
   */
  public int[] toUnsignedByteArray() {
    if (writeCursor == 0) {
      return new int[0];
    }

    int sz = writeCursor;
    int[] result = new int[(sz + 7) / 8];

    for (int i = 0; i < sz; i += 8) {
      int value = 0;
      for (int j = 0; j < 8 && i + j < sz; j++) {
        if (get(i + j)) {
          value |= (1 << (7 - j));
        }
      }
      result[i / 8] = value;
    }

    return result;
  }

  /**
   * Convert to signed byte array (byte[] where each element is -128 to 127)
   *
   * @return byte[] array of signed bytes
   */
  public byte[] toSignedByteArray() {
    if (writeCursor == 0) {
      return new byte[0];
    }

    int sz = writeCursor;
    byte[] result = new byte[(sz + 7) / 8];

    for (int i = 0; i < sz; i += 8) {
      int value = 0;
      for (int j = 0; j < 8 && i + j < sz; j++) {
        if (get(i + j)) {
          value |= (1 << (7 - j));
        }
      }
      result[i / 8] = (byte) (value & 0xFF);
    }

    return result;
  }

  /**
   * Convert to list of BigInteger values
   *
   * @return List<BigInteger> list of BigInteger values
   */
  public List<BigInteger> toByteList() {
    if (writeCursor == 0) {
      return new ArrayList<>();
    }

    int sz = writeCursor;
    List<BigInteger> result = new ArrayList<>((sz + 7) / 8);

    for (int i = 0; i < sz; i += 8) {
      StringBuilder binStr = new StringBuilder();
      for (int j = 0; j < 8 && i + j < sz; j++) {
        binStr.append(get(i + j) ? '1' : '0');
      }
      result.add(new BigInteger(binStr.toString(), 2));
    }

    return result;
  }

  public boolean[] toBitArray() {
    boolean[] result = new boolean[writeCursor];
    for (int i = readCursor; i < writeCursor; i++) {
      result[i] = get(i);
    }
    return result;
  }

  public int[] toZeroOneArray() {
    int[] result = new int[writeCursor];
    for (int i = readCursor; i < writeCursor; i++) {
      result[i] = get(i) ? 1 : 0;
    }
    return result;
  }

  public BitString clone() {
    BitString result = new BitString(0);
    result.array = Arrays.copyOfRange(array, 0, array.length);
    result.length = length;
    result.writeCursor = writeCursor;
    result.readCursor = readCursor;
    return result;
  }

  public BitString cloneFrom(int from) {
    BitString result = new BitString(0);
    result.array = Arrays.copyOfRange(array, from, array.length);
    result.length = length;
    result.writeCursor = writeCursor - (from * 8);
    result.readCursor = readCursor;
    return result;
  }

  public BitString cloneClear() {
    BitString result = new BitString(0);
    result.array = Arrays.copyOfRange(array, 0, array.length);
    result.length = length;
    result.writeCursor = 0;
    result.readCursor = 0;
    return result;
  }

  /**
   * like Fift
   *
   * @return String
   */
  public String toHex() {
    if (writeCursor % 4 == 0) {
      byte[] arr = Arrays.copyOfRange(array, 0, (int) Math.ceil(writeCursor / (double) 8));
      String s = Utils.bytesToHex(arr).toUpperCase();
      if (writeCursor % 8 == 0) {
        return s;
      } else {
        return s.substring(0, s.length() - 1);
      }
    } else {
      BitString temp = clone();
      temp.writeBit(true);
      while (temp.writeCursor % 4 != 0) {
        temp.writeBit(false);
      }
      return temp.toHex().toUpperCase() + '_';
    }
  }
}
