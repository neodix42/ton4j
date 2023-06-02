package org.ton.java.cell;

import org.ton.java.address.Address;
import org.ton.java.bitstring.BitString;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;

public class CellBuilder extends Cell {

    private CellBuilder() {

    }

    private CellBuilder(int cellSizeInBits) {
        super(cellSizeInBits);
    }

    public static CellBuilder beginCell() {
        return new CellBuilder();
    }

    public static CellBuilder beginCell(int cellSize) {
        return new CellBuilder(cellSize);
    }

    /**
     * Converts a builder into an ordinary cell.
     */
    public Cell endCell() {
        return this;
    }

    public CellBuilder storeBit(boolean bit) {
        checkBitsOverflow(1);
        bits.writeBit(bit);
        return this;
    }

    public CellBuilder storeBits(List<Boolean> arrayBits) {
        checkBitsOverflow(arrayBits.size());
        for (Boolean bit : arrayBits) {
            bits.writeBit(bit);
        }
        return this;
    }

    public CellBuilder storeBits(boolean[] arrayBits) {
        checkBitsOverflow(arrayBits.length);
        bits.writeBitArray(arrayBits);
        return this;
    }

    public CellBuilder storeUint(long number, int bitLength) {
        return storeUint(BigInteger.valueOf(number), bitLength);
    }

    public CellBuilder storeUint(int number, int bitLength) {
        return storeUint(BigInteger.valueOf(number), bitLength);
    }

    public CellBuilder storeUint(short number, int bitLength) {
        return storeUint(BigInteger.valueOf(number), bitLength);
    }

    public CellBuilder storeUint(Byte number, int bitLength) {
        return storeUint(BigInteger.valueOf(number), bitLength);
    }

    public CellBuilder storeUint(String number, int bitLength) {
        return storeUint(new BigInteger(number), bitLength);
    }

    public CellBuilder storeUint(BigInteger number, int bitLength) {
        checkBitsOverflow(bitLength);
        checkSign(number);
        bits.writeUint(number, bitLength);
        return this;
    }

    public CellBuilder storeInt(long number, int bitLength) {
        return storeInt(BigInteger.valueOf(number), bitLength);
    }

    public CellBuilder storeInt(int number, int bitLength) {
        return storeInt(BigInteger.valueOf(number), bitLength);
    }

    public CellBuilder storeInt(short number, int bitLength) {
        return storeInt(BigInteger.valueOf(number), bitLength);
    }

    public CellBuilder storeInt(byte number, int bitLength) {
        return storeInt(BigInteger.valueOf(number), bitLength);
    }

    public CellBuilder storeInt(BigInteger number, int bitLength) {
        BigInteger sint = BigInteger.ONE.shiftLeft(bitLength - 1);
        if ((number.compareTo(sint.negate()) >= 0) && (number.compareTo(sint) < 0)) {
            bits.writeInt(number, bitLength);
            return this;
        } else {
            throw new Error("Can't store an Int, because its value allocates more space than provided.");
        }
    }

    public CellBuilder storeBitString(BitString bitString) {
        checkBitsOverflow(bitString.getUsedBits());
        bits.writeBitString(bitString);
//        bits.writeBitStringFromRead(bitString);
        return this;
    }

    public CellBuilder storeString(String str) {
        checkBitsOverflow(str.length() * 8);
        bits.writeString(str);
        return this;
    }

    public CellBuilder storeAddress(Address address) {
        checkBitsOverflow(267);
        bits.writeAddress(address);
        return this;
    }

    public CellBuilder storeBytes(byte[] number) {
        checkBitsOverflow(number.length * 8);
        bits.writeBytes(number);
        return this;
    }

    public CellBuilder storeBytes(int[] number) {
        checkBitsOverflow(number.length * 8);
        bits.writeBytes(number);
        return this;
    }

    public CellBuilder storeBytes(List<Byte> bytes) {
        checkBitsOverflow(bytes.size() * 8);
        for (Byte b : bytes) {
            bits.writeUint8(b);
        }
        return this;
    }

    public CellBuilder storeBytes(byte[] number, int bitLength) {
        checkBitsOverflow(bitLength);
        bits.writeBytes(number);
        return this;
    }


    public CellBuilder storeRef(Cell c) {
        checkRefsOverflow(1);
        refs.add(c);
        return this;
    }

    public CellBuilder storeRefs(List<Cell> cells) {
        checkRefsOverflow(cells.size());
        refs.addAll(cells);
        return this;
    }

    public CellBuilder storeRefs(Cell... cells) {
        checkRefsOverflow(cells.length);
        refs.addAll(Arrays.asList(cells));
        return this;
    }

    public CellBuilder storeSlice(CellSlice cellSlice) {
        checkBitsOverflow(cellSlice.bits.getUsedBits());
        checkRefsOverflow(cellSlice.refs.size());

        storeBitString(cellSlice.bits);

        refs.addAll(cellSlice.refs);
        return this;
    }

    public CellBuilder storeDict(Cell dict) {
        storeSlice(CellSlice.beginParse(dict));
        return this;
    }

    /**
     * Stores up to 2^120-1 nano-coins in Cell
     *
     * @param coins amount in nano-coins
     * @return CellBuilder
     */
    public CellBuilder storeCoins(BigInteger coins) {
        bits.writeCoins(coins);
        return this;
    }

    public int getUsedBits() {
        return bits.getUsedBits();
    }

    public int getFreeBits() {
        return bits.getFreeBits();
    }

    void checkBitsOverflow(int length) {
        if (length > bits.getFreeBits()) {
            throw new Error("Bits overflow. Can't add " + length + " bits. " + bits.getFreeBits() + " bits left.");
        }
    }

    void checkSign(BigInteger i) {
        if (i.signum() < 0) {
            throw new Error("Integer " + i + " must be unsigned");
        }
    }

    void checkRefsOverflow(int count) {
        if (count > (4 - refs.size())) {
            throw new Error("Refs overflow. Can't add " + count + " refs. " + (4 - refs.size()) + " refs left.");
        }
    }
}
