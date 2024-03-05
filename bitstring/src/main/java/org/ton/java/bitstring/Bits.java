package org.ton.java.bitstring;

import java.math.BigInteger;

public interface Bits<T> {
    //T get();

    void writeBit(T b);

    T readBit();

    String toHex();

    int getFreeBits();

    int getUsedBits();

    void writeUint(BigInteger number, int bitLength);

    BigInteger readUint(int bitLength);
}
