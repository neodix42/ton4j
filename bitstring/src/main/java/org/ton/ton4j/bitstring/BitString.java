package org.ton.ton4j.bitstring;

import static java.util.Objects.isNull;

import java.io.Serializable;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.List;
import java.util.logging.Logger;
import org.ton.ton4j.address.Address;
import org.ton.ton4j.utils.Utils;

/**
 * Each element is one bit in memory. Implementation uses Java's BitSet for efficient bit
 * manipulation.
 */
public class BitString implements Serializable {
  private static final Logger log = Logger.getLogger(BitString.class.getName());
  private BitSet bitSet;
  public int writeCursor;
  public int readCursor;
  public int length;

  public BitString() {
    bitSet = new BitSet(1023);
    writeCursor = 0;
    readCursor = 0;
    length = 1023;
  }

  public BitString(BitString bs) {
    bitSet = new BitSet();
    writeCursor = 0;
    readCursor = 0;
    length = bs.length;
    for (int i = bs.readCursor; i < bs.writeCursor; i++) {
      writeBit(bs.get(i));
    }
  }

  /**
   * Create BitString limited by length
   *
   * @param length int length of BitString in bits
   */
  public BitString(int length) {
    bitSet = new BitSet(length);
    writeCursor = 0;
    readCursor = 0;
    this.length = length;
  }

  /**
   * Create BitString from byte array
   *
   * @param bytes byte[] array of bytes
   */
  public BitString(byte[] bytes) {
    this(Utils.signedBytesToUnsigned(bytes));
  }

  /**
   * Create BitString from byte array with specified size using BitSet methods
   *
   * @param bytes byte[] array of bytes
   * @param size int number of bits to read
   */
  public BitString(byte[] bytes, int size) {
    if (bytes.length == 0) {
      bitSet = new BitSet(0);
      writeCursor = 0;
      readCursor = 0;
      length = 0;
    } else {
      length = size;
      bitSet = new BitSet(length);
      writeCursor = 0;
      readCursor = 0;

      // Calculate how many complete bytes we can process
      int bytesToProcess = Math.min(bytes.length, (size + 7) / 8);

      for (int i = 0; i < bytesToProcess; i++) {
        int value = bytes[i] & 0xFF; // Ensure we treat the value as unsigned byte
        for (int j = 0; j < 8; j++) {
          // Check if we've reached the size limit
          if (writeCursor >= size) {
            break;
          }

          // Check if the bit at position j (from MSB) is set
          boolean bitValue = ((value >> (7 - j)) & 1) == 1;
          if (bitValue) {
            bitSet.set(writeCursor);
          } else {
            bitSet.clear(writeCursor);
          }
          writeCursor++;
        }
      }
    }
  }

  /**
   * Create BitString from int array using BitSet methods
   *
   * @param bytes int[] array of bytes
   */
  public BitString(int[] bytes) {
    if (bytes.length == 0) {
      bitSet = new BitSet();
      writeCursor = 0;
      readCursor = 0;
      length = 0;
    } else {
      length = bytes.length * 8;
      bitSet = new BitSet(length);
      writeCursor = 0;
      readCursor = 0;

      for (int aByte : bytes) {
        int value = aByte & 0xFF; // Ensure we treat the value as unsigned byte
        for (int j = 0; j < 8; j++) {
          // Check if the bit at position j (from MSB) is set
          boolean bitValue = ((value >> (7 - j)) & 1) == 1;
          if (bitValue) {
            bitSet.set(writeCursor);
          } else {
            bitSet.clear(writeCursor);
          }
          writeCursor++;
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
    return writeCursor - readCursor;
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
  public Boolean get(int n) {
    checkRange(n);
    return bitSet.get(n);
  }

  /**
   * Check if bit at position n is reachable
   *
   * @param n int
   */
  private void checkRange(int n) {
    if (n >= length) {
      throw new Error("BitString overflow. n[" + n + "] >= length[" + length + "]");
    }
  }

  /**
   * Set bit value to 1 at position n
   *
   * @param n int
   */
  void on(int n) {
    try {
      bitSet.set(n);
    } catch (Exception e) {
      // ignore
    }
  }

  /**
   * Set bit value to 0 at position n
   *
   * @param n int
   */
  void off(int n) {
    try {
      bitSet.clear(n);
    } catch (Exception e) {
      // ignore
    }
  }

  /**
   * Toggle bit value at position n
   *
   * @param n int
   */
  void toggle(int n) {
    try {
      bitSet.flip(n);
    } catch (Exception e) {
      // ignore
    }
  }

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
   * Appends BitString with Address addr_none$00 = MsgAddressExt; addr_std$10 anycast:(Maybe
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
   * Write another BitString to this BitString
   *
   * @param anotherBitString BitString
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

  /**
   * Read one bit and moves readCursor forward by one position
   *
   * @return Boolean
   */
  public Boolean readBit() {
    Boolean result = get(readCursor);
    readCursor++;
    return result;
  }

  /**
   * Read n bits from the BitString
   *
   * @param n integer
   * @return BitString with length n read from original BitString
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
   * Read n bits from the BitString
   *
   * @param n integer
   * @return BitString with length n read from original BitString
   */
  public BitString readBits(int n) {
    BitString result = new BitString(n);
    for (int i = 0; i < n; i++) {
      result.writeBit(readBit());
    }
    return result;
  }

  /**
   * Read rest of bits from the BitString
   *
   * @return BitString with length n read from original BitString
   */
  public BitString readBits() {
    BitString result = new BitString(); // todo
    for (int i = 0; i < writeCursor; i++) {
      result.writeBit(readBit());
    }
    return result;
  }

  /**
   * Read bits of bitLength without moving readCursor, i.e. modifying BitString
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
   * @return BitString from 0 to writeCursor
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
    int j = 0;
    for (int i = 0; i < writeCursor; i++) {
      result[j++] = get(i);
    }
    return result;
  }

  public int getLength() {
    // Calculate length based on BitSet capacity
    return Math.max(length, bitSet.length());
  }

  /**
   * @return BitString from current position to writeCursor
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
    if (writeCursor == 0) {
      return new byte[0];
    }

    // Create a temporary BitSet with the same bits but in reverse order (MSB to LSB)
    BitSet reversedBitSet = new BitSet(writeCursor);
    for (int i = 0; i < writeCursor; i++) {
      if (bitSet.get(i)) {
        // For each byte, reverse the bit order
        int byteIndex = i / 8;
        int bitPosition = i % 8;
        // Convert from MSB-first to LSB-first within each byte
        int reversedBitPosition = 7 - bitPosition;
        reversedBitSet.set(byteIndex * 8 + reversedBitPosition);
      }
    }

    // Calculate the number of bytes needed
    int numBytes = (writeCursor + 7) / 8;
    byte[] result = new byte[numBytes];

    // Convert the reversed BitSet to a byte array
    byte[] tempBytes = reversedBitSet.toByteArray();

    // Copy bytes, ensuring we have the right number of bytes
    System.arraycopy(tempBytes, 0, result, 0, Math.min(tempBytes.length, numBytes));

    return result;
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

    byte[] bytes = toByteArray();
    int[] result = new int[bytes.length];

    for (int i = 0; i < bytes.length; i++) {
      result[i] = bytes[i] & 0xFF; // Convert signed byte to unsigned int
    }

    return result;
  }

  /**
   * Convert to signed byte array (byte[] where each element is -128 to 127)
   *
   * @return byte[] array of signed bytes
   */
  public byte[] toSignedByteArray() {
    // Since Java bytes are already signed, we can just use toByteArray
    return toByteArray();
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
    BitString result = new BitString(bitSet.size());
    result.bitSet = (BitSet) bitSet.clone();
    result.length = length;
    result.writeCursor = writeCursor;
    result.readCursor = readCursor;
    return result;
  }

  public BitString cloneFrom(int from) {
    BitString result = new BitString(bitSet.size());
    result.bitSet = (BitSet) bitSet.clone();
    result.length = length;
    result.writeCursor = writeCursor - (from * 8);
    result.readCursor = readCursor;
    return result;
  }

  public BitString cloneClear() {
    BitString result = new BitString(bitSet.size());
    result.bitSet = (BitSet) bitSet.clone();
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
    if (writeCursor == 0) {
      return "";
    }

    if (writeCursor % 4 == 0) {
      byte[] arr = toByteArray();
      String s = Utils.bytesToHex(arr).toUpperCase();
      if (writeCursor % 8 == 0) {
        return s;
      } else {
        return s.substring(0, s.length() - 1);
      }
    } else {
      // Create a temporary BitString with increased length to accommodate padding
      BitString temp = new BitString(writeCursor + 4);
      // Copy all bits from the original BitString
      for (int i = 0; i < writeCursor; i++) {
        temp.writeBit(get(i));
      }
      temp.writeBit(true);
      while (temp.writeCursor % 4 != 0) {
        temp.writeBit(false);
      }
      return temp.toHex().toUpperCase() + '_';
    }
  }
}
