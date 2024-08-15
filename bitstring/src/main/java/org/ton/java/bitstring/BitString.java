package org.ton.java.bitstring;

import org.apache.commons.lang3.StringUtils;
import org.ton.java.address.Address;
import org.ton.java.utils.Utils;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.*;

import static java.util.Objects.isNull;

/**
 * Implements BitString where each bit is actually a Boolean variable in memory.
 * Not efficient, but perfect for educational purposes.
 * See RealBitString implementation where each element of BitString stored as real bit in a memory.
 */
public class BitString implements Bits<Boolean> {

    Deque<Boolean> array;

    private static final int MAX_LENGTH = 1023;

    private final int initialLength;

    public BitString(BitString bs) {
        array = new ArrayDeque<>(bs.array.size());
        for (Boolean b : bs.array) {
            writeBit(b);
        }
        initialLength = bs.array.isEmpty() ? MAX_LENGTH : bs.array.size();
    }

    public BitString(byte[] bytes) {
        this(Utils.signedBytesToUnsigned(bytes));
    }

    public BitString(int[] bytes) {
        if (bytes.length == 0) {
            array = new ArrayDeque<>(0);
            initialLength = 0;
        } else {
            byte[] bits = Utils.leftPadBytes(Utils.bytesToBitString(bytes).getBytes(StandardCharsets.UTF_8), bytes.length * 8, '0');

            array = new ArrayDeque<>(bits.length);
            for (byte bit : bits) { // whole length
                if (bit == (byte) '1') {
                    array.addLast(true);
                } else if (bit == (byte) '0') {
                    array.addLast(false);
                } else {
                    // else '-' sign - do nothing
                }
            }
            initialLength = bits.length;
        }
    }

//    public BitString(byte[] bytes, int size) {
//        this(Utils.signedBytesToUnsigned(bytes), size); // todo redo below
//    }

    public BitString(byte[] bytes, int size) {
        if (bytes.length == 0) {
            array = new ArrayDeque<>(0);
            initialLength = 0;
        } else {
            byte[] bits = Utils.leftPadBytes(Utils.bytesToBitString(bytes).getBytes(StandardCharsets.UTF_8), bytes.length * 8, '0');
            array = new ArrayDeque<>(bits.length);
            for (int i = 0; i < size; i++) { // specified length
                if (bits[i] == (byte) '1') {
                    array.addLast(true);
                } else if (bits[i] == (byte) '0') {
                    array.addLast(false);
                } else {
                    // else '-' sign - do nothing
                }
            }
            initialLength = bits.length;
        }
    }


    /**
     * Create BitString limited by length
     *
     * @param length int    length of BitString in bits
     */
    public BitString(int length) {
        array = new ArrayDeque<>(length);
        initialLength = length;
    }

    public BitString() {
        array = new ArrayDeque<>(MAX_LENGTH);
        initialLength = MAX_LENGTH;
    }

    /**
     * Return free bits, that derives from total length minus bits written
     *
     * @return int
     */
    public int getFreeBits() {
        return initialLength - array.size();
    }

    /**
     * Returns used bits, i.e. last position of writeCursor
     *
     * @return int
     */
    public int getUsedBits() {
        return array.size();
    }

    /**
     * @return int
     */
    public int getUsedBytes() {
        return (array.size() + 7) / 8;
    }

    /**
     * Gets current bit without removing it
     *
     * @return Boolean    bit value at position `n`
     */
    public Boolean get() {
        return array.peekFirst();
    }

    /**
     * Check if bit at position n is reachable
     *
     * @param n int
     */
    private void checkRange(int n) {
        if (n > getLength()) {
            throw new Error("BitString overflow");
        }
    }

    /**
     * Write bit and increase cursor
     *
     * @param b Boolean
     */
    public void writeBit(Boolean b) {
        array.addLast(b);
    }

    public void writeBits(String b) {
        for (Character c : b.toCharArray()) {
            array.addLast(c == '1');
        }
    }

    /**
     * Write bit and increase cursor
     *
     * @param b byte
     */
    void writeBit(byte b) {
        array.addLast(b > 0);
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
     * @param ba byte[]
     */
    public void writeBitArray(byte[] ba) {
        for (byte b : ba) {
            writeBit(b);
        }
    }

    /**
     * Write unsigned int
     *
     * @param number    BigInteger
     * @param bitLength int size of uint in bits
     */
    public void writeUint(BigInteger number, int bitLength) {
        if (number.compareTo(BigInteger.ZERO) < 0) {
            throw new Error("Unsigned number cannot be less than 0");
        }
        if (bitLength == 0 || number.bitLength() > bitLength) {
            if (number.compareTo(BigInteger.ZERO) == 0) {
                return;
            }
            throw new Error("bitLength is too small for number, got number=" + number + ", bitLength=" + bitLength);
        }

        byte[] s = number.toString(2).getBytes(StandardCharsets.UTF_8);

        if (s.length != bitLength) {
            s = repeatZerosAndMerge(bitLength - s.length, s);
        }

        for (int i = 0; i < bitLength; i++) {
            writeBit(s[i] == (byte) '1');
        }
    }

    private byte[] repeatZerosAndMerge(int count, byte[] s) {
        byte[] a = new byte[count + s.length];
        Arrays.fill(a, 0, count, (byte) '0');
        System.arraycopy(s, 0, a, count, s.length);
        return a;
    }

    /**
     * Write unsigned int
     *
     * @param number    value
     * @param bitLength size of uint in bits
     */
    public void writeUint(long number, int bitLength) {
        writeUint(BigInteger.valueOf(number), bitLength);
    }

    /**
     * Write signed int
     *
     * @param number    BigInteger
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
            int bytesSize = (amount.bitLength() + 7) / 8;
            if (bytesSize >= 16) {
                throw new Error("Amount is too big. Maximum amount 2^120-1");
            }
            writeUint(BigInteger.valueOf(bytesSize), 4);
            writeUint(amount, bytesSize * 8);
        }
    }

    public void writeVarUint(BigInteger value, int bitLength) {

        if (value.compareTo(BigInteger.ZERO) == 0) {
            writeUint(BigInteger.ZERO, bitLength);
        } else {
            int bytesSize = (value.bitLength() + 7) / 8;
            if (bytesSize >= bitLength) {
                throw new Error("Amount is too big. Should fit in " + bitLength + " bits");
            }
            writeUint(BigInteger.valueOf(bytesSize), bitLength);
            writeUint(value, bytesSize * 8);
        }
    }

    /**
     * Appends BitString with Address
     * addr_none$00 = MsgAddressExt;
     * addr_std$10
     * anycast:(Maybe Anycast)
     * workchain_id:int8
     * address:uint256 = MsgAddressInt;
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
        for (Boolean b : anotherBitString.array) {
            writeBit(anotherBitString.readBit());
        }
    }

    /**
     * Read one bit without removing it
     *
     * @return true or false
     */
    public Boolean preReadBit() {
        return get();
    }

    /**
     * Read and removes one bit from start
     *
     * @return true or false
     */
    public Boolean readBit() {
        return array.pollFirst();
    }

    /**
     * Read n bits from the BitString
     *
     * @param n integer
     * @return BitString with length n read from original Bitstring
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
     * @return BitString with length of read bits from original Bitstring
     */
    public BitString readBits() {
        BitString result = new BitString(array.size());
        for (int i = 0; i < array.size(); i++) {
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
        if (bitLength < 1) {
            throw new Error("Incorrect bitLength");
        }

        BitString cloned = clone();
        StringBuilder s = new StringBuilder();

        for (int i = 0; i < bitLength; i++) {
            Boolean b = cloned.readBit();
            s.append(b ? 1 : 0);
        }

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
            Boolean b = readBit();
            s.append(b ? 1 : 0);
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

        Boolean sign = readBit();
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

        String address = workchain + ":" + String.format("%64s", hashPart.toString(16)).replace(' ', '0');
        return Address.of(address);
    }

    public String readString(int length) {
        BitString bitString = readBits(length);
        return new String(bitString.toByteArray());
    }

    /**
     * @param length in bits
     * @return byte array
     */
    public byte[] readBytes(int length) {
        BitString bitString = readBits(length);
        return bitString.toByteArray();
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
        BitString cloned = clone();
        StringBuilder s = new StringBuilder();
        for (Boolean b : cloned.array) {
            s.append(b ? '1' : '0');
        }
        return s.toString();
    }

    public int getLength() {
        return array.size();
    }

    /**
     * @return BitString from current position to writeCursor
     */
    public String getBitString() {
        StringBuilder s = new StringBuilder();
        for (Boolean b : array) {
            s.append(b ? '1' : '0');
        }
        return s.toString();
    }

    public int[] toUnsignedByteArray() {
        if (array.isEmpty()) {
            return new int[0];
        }

        String bin = getBitString();
        int sz = bin.length();
        int[] result = new int[(sz + 7) / 8];

        for (int i = 0; i < sz; i += 8) {
            String str = bin.substring(i, Math.min(sz, i + 8));
            result[i / 8] = Integer.parseInt(str, 2);
        }

        return result;
    }

    public byte[] toSignedByteArray() {
        if (array.isEmpty()) {
            return new byte[0];
        }

        String bin = getBitString();
        int sz = bin.length();
        byte[] result = new byte[(sz + 7) / 8];

        for (int i = 0; i < sz; i += 8) {
            String str = bin.substring(i, Math.min(sz, i + 8));
            result[i / 8] = (byte) (Integer.parseInt(str, 2) & 0xFF);
        }

        return result;
    }

    public List<BigInteger> toByteList() {
        if (array.isEmpty()) {
            return new ArrayList<>();
        }

        String bin = getBitString();
        int sz = bin.length();
        List<BigInteger> result = new ArrayList<>((sz + 7) / 8);

        for (int i = 0; i < sz; i += 8) {
            String str = bin.substring(i, Math.min(sz, i + 8));
            result.add(new BigInteger(str, 2));
        }

        return result;
    }

    public byte[] toByteArray() {
        if (array.isEmpty()) {
            return new byte[0];
        }

        byte[] bin = getBitString().getBytes(StandardCharsets.UTF_8);
        int sz = bin.length;
        byte[] result = new byte[(sz + 7) / 8];

        for (int i = 0; i < sz; i++) {
            if (bin[i] == (byte) '1') {
                result[(i / 8)] |= (byte) (1 << (7 - (i % 8)));
            } else {
                result[(i / 8)] &= (byte) ~(1 << (7 - (i % 8)));
            }
        }

        return result;
    }

    public int[] toUintArray() {
        if (array.isEmpty()) {
            return new int[0];
        }

        byte[] bin = getBitString().getBytes(StandardCharsets.UTF_8);
        int sz = bin.length;
        int[] result = new int[(sz + 7) / 8];

        for (int i = 0; i < sz; i++) {
            if (bin[i] == (byte) '1') {
                result[(i / 8)] |= 1 << (7 - (i % 8));
            } else {
                result[(i / 8)] &= ~(1 << (7 - (i % 8)));
            }
        }

        return result;
    }

    public Boolean[] toBooleanArray() {
        Boolean[] result = new Boolean[getLength()];
        int i = 0;
        for (Boolean b : array) {
            result[i++] = b;
        }
        return result;
    }

    public BitString clone() {
        return new BitString(this);
    }

    public BitString cloneFrom(int from) {
        BitString cloned = clone();
        for (int i = 0; i < from; i++) {
            cloned.readBit();
        }
        return cloned;
    }

    /**
     * like Fift
     *
     * @return String
     */
    public String toHex() {

        if (array.size() % 4 == 0) {
            byte[] arr = toByteArray();
            String s = Utils.bytesToHex(arr).toUpperCase();
            if (array.size() % 8 == 0) {
                return s;
            } else {
                return s.substring(0, s.length() - 1);
            }
        } else {
            BitString temp = clone();
            temp.writeBit(true);
            while (temp.array.size() % 4 != 0) {
                temp.writeBit(false);
            }
            return temp.toHex().toUpperCase() + '_';
        }
    }
}
