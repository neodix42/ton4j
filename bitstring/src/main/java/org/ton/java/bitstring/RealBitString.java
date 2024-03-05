package org.ton.java.bitstring;

import java.math.BigInteger;

/**
 * Implements BitString where each bit occupies only one bit in memory.
 * Todo NOT FINISHED.
 */
public class RealBitString implements Bits<Byte> {

    byte[] array;

    private static int MAX_LENGTH = 1023;

    private int initialLength;

    @Override
    public void writeBit(Byte b) {

    }

    @Override
    public Byte readBit() {
        return null;
    }

    @Override
    public String toHex() {
        return null;
    }

    @Override
    public int getFreeBits() {
        return 0;
    }

    @Override
    public int getUsedBits() {
        return 0;
    }

    @Override
    public void writeUint(BigInteger number, int bitLength) {

    }

    @Override
    public BigInteger readUint(int bitLength) {
        return null;
    }
}
