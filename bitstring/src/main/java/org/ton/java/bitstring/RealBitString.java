package org.ton.java.bitstring;

import static java.util.Objects.isNull;

import java.io.Serializable;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.logging.Logger;
import org.ton.java.address.Address;
import org.ton.java.utils.Utils;

/**
 * Each element is one bit in memory.
 * Interestingly, but this solution loses to Deque<Boolean> array solution from memory footprint allocation.
 */
public class RealBitString implements Serializable {
    private static final Logger log = Logger.getLogger(RealBitString.class.getName());
    byte[] array;
    public int writeCursor;
    public int readCursor;
    public int length;

    public RealBitString(RealBitString bs) {
        for (int i = bs.readCursor; i < bs.writeCursor; i++) {
            writeBit(bs.get(i));
        }
    }

    /**
     * Create RealBitString limited by length
     *
     * @param length int    length of RealBitString in bits
     */
    public RealBitString(int length) {
        array = new byte[(int) Math.ceil(length / (double) 8)];
        writeCursor = 0;
        readCursor = 0;
        this.length = length;
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
     * @return boolean    bit value at position `n`
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
        if (n >= length) {
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
        checkRange(n);
        array[(n / 8)] &= ~(1 << (7 - (n % 8)));
    }

    /**
     * Toggle bit value at position n
     *
     * @param n int
     */
    void toggle(int n) {
        this.checkRange(n);
        array[(n / 8)] ^= 1 << (7 - (n % 8));
    }

    /**
     * Write bit and increase cursor
     *
     * @param b boolean
     */
    public void writeBit(boolean b) {
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
     * Write unsigned int
     *
     * @param number    BigInteger
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
            throw new Error("bitLength is too small for number, got number=" + number + ", bitLength=" + bitLength);
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
     * Appends RealBitString with Address
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
     * Write another RealBitString to this RealBitString
     *
     * @param anotherRealBitString RealBitString
     */
    public void writeRealBitString(RealBitString anotherRealBitString) {
        for (int i = anotherRealBitString.readCursor; i < anotherRealBitString.writeCursor; i++) {
            writeBit(anotherRealBitString.get(i));
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
     * @return true or false
     */
    public boolean readBit() {
        boolean result = get(readCursor);
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
    public RealBitString preReadBits(int n) {
        int oldReadCursor = readCursor;
        RealBitString result = new RealBitString(n);
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
    public RealBitString readBits(int n) {
        RealBitString result = new RealBitString(n);
        for (int i = 0; i < n; i++) {
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

        String address = workchain + ":" + String.format("%64s", hashPart.toString(16)).replace(' ', '0');
        return Address.of(address);
    }

    public String readString(int length) {
        RealBitString RealBitString = readBits(length);
        return new String(RealBitString.toByteArray());
    }

    /**
     * @param length in bits
     * @return byte array
     */
    public byte[] readBytes(int length) {
        RealBitString RealBitString = readBits(length);
        return RealBitString.toByteArray();
    }

    /**
     * @return hex string
     */
    public String toString() {
        return toRealBitString();
    }

    /**
     * @return RealBitString from 0 to writeCursor
     */
    public String toRealBitString() {
        StringBuilder s = new StringBuilder();
        for (int i = 0; i < writeCursor; i++) {
            char bit = get(i) ? '1' : '0';
            s.append(bit);
        }
        return s.toString();
    }

    /**
     * @return RealBitString from current position to writeCursor
     */
    public String getRealBitString() {
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

    public RealBitString clone() {
        RealBitString result = new RealBitString(0);
        result.array = Arrays.copyOfRange(array, 0, array.length);
        result.length = length;
        result.writeCursor = writeCursor;
        result.readCursor = readCursor;
        return result;
    }

    public RealBitString cloneFrom(int from) {
        RealBitString result = new RealBitString(0);
        result.array = Arrays.copyOfRange(array, from, array.length);
        result.length = length;
        result.writeCursor = writeCursor - (from * 8);
        result.readCursor = readCursor;
        return result;
    }

    public RealBitString cloneClear() {
        RealBitString result = new RealBitString(0);
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
            RealBitString temp = clone();
            temp.writeBit(true);
            while (temp.writeCursor % 4 != 0) {
                temp.writeBit(false);
            }
            return temp.toHex().toUpperCase() + '_';
        }
    }

    public void setTopUppedArray(byte[] arr, boolean fulfilledBytes) {
        length = arr.length * 8;
        array = arr;
        writeCursor = length;
//        int saveWriteCursor = writeCursor;


        if (!(fulfilledBytes || (length == 0))) {
            boolean foundEndBit = false;
            for (byte c = 0; c < 7; c++) {
                writeCursor -= 1;
                if (get(writeCursor)) {
                    foundEndBit = true;
                    off(writeCursor);
//                    writeCursor += 3;
                    break;
                }
            }
//            writeCursor = saveWriteCursor;

            if (!foundEndBit) {
                log.info((Arrays.toString(arr) + ", " + fulfilledBytes));
                throw new Error("Incorrect TopUppedArray");
            }
        }
    }

    public byte[] getTopUppedArray() {
        RealBitString ret = clone();
        int tu = (int) Math.ceil(ret.writeCursor / (double) 8) * 8 - ret.writeCursor;
        if (tu > 0) {
            tu = tu - 1;
            ret.writeBit(true);
            while (tu > 0) {
                tu = tu - 1;
                ret.writeBit(false);
            }
        }
        ret.array = Arrays.copyOfRange(ret.array, 0, (int) Math.ceil(ret.writeCursor / (double) 8));
        return ret.array;
    }
}
