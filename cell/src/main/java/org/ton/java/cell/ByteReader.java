package org.ton.java.cell;

import org.ton.java.utils.Utils;

import java.util.Arrays;

public class ByteReader {
    int[] data;

    ByteReader(byte[] data) {
        this.data = Utils.signedBytesToUnsigned(data);
    }

    ByteReader(int[] data) {
        this.data = Arrays.copyOfRange(data, 0, data.length);
    }

    int[] readBytes(long num) {
        int[] ret = Arrays.copyOfRange(data, 0, (int) num);
        data = Arrays.copyOfRange(data, (int) num, data.length);
        return ret;
    }

    byte[] readSignedBytes(long num) {
        byte[] ret = Arrays.copyOfRange(Utils.unsignedBytesToSigned(data), 0, (int) num);
        data = Arrays.copyOfRange(data, (int) num, data.length);
        return ret;
    }

    int readByte() {
        int[] ret = Arrays.copyOfRange(data, 0, 1);
        data = Arrays.copyOfRange(data, 1, data.length);
        return ret[0];
    }

    byte readSignedByte() {
        int[] ret = Arrays.copyOfRange(data, 0, 1);
        data = Arrays.copyOfRange(data, 1, data.length);
        return (byte) ret[0];
    }
}
