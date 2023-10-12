package org.ton.java.cell;

import org.ton.java.utils.Utils;

import java.util.Arrays;

public class ByteReader {
    int[] data;
    public int pos;

    public long getDataSize() {
        return data.length;
    }

    public ByteReader(byte[] data) {
        this.data = Utils.signedBytesToUnsigned(data);
    }

    public ByteReader(int[] data) {
        this.data = Arrays.copyOfRange(data, 0, data.length);
    }

    public int[] readBytes(long num) {
        pos += num;
        int[] ret = Arrays.copyOfRange(data, 0, (int) num);
        data = Arrays.copyOfRange(data, (int) num, data.length);
        return ret;
    }

    public int[] preReadBytes(long num) {
        return Arrays.copyOfRange(data, 0, (int) num);
    }

    public int[] readBytes() {
        pos += data.length;
        int[] ret = Arrays.copyOfRange(data, 0, data.length);
        data = new int[0];
        return ret;
    }

    /**
     * Read all bytes without
     *
     * @return int[]
     */
    public int[] preReadBytes() {
        return Arrays.copyOfRange(data, 0, data.length);
    }

    public int[] readBytesPos(long num) {
        int[] ret = Arrays.copyOfRange(data, pos, (int) num);
        pos += num;
        return ret;
    }

    public int[] readBytesReversed(long num) {
        return Utils.reverseIntArray(readBytes(num));
    }

    public byte[] readSignedBytes(long num) {
        pos += num;
        byte[] ret = Arrays.copyOfRange(Utils.unsignedBytesToSigned(data), 0, (int) num);
        data = Arrays.copyOfRange(data, (int) num, data.length);
        return ret;
    }

    public byte[] preReadSignedBytes(long num) {
        return Arrays.copyOfRange(Utils.unsignedBytesToSigned(data), 0, (int) num);
    }

    public byte[] readSignedBytesReversed(long num) {
        return Utils.reverseByteArray(readSignedBytes(num));
    }

    public byte[] preReadSignedBytesReversed(long num) {
        return Utils.reverseByteArray(preReadSignedBytes(num));
    }

    public short readShortBigEndian() {
        return Utils.bytesToShort(readSignedBytesReversed(2));
    }

    public short preReadShortBigEndian() {
        return Utils.bytesToShort(preReadSignedBytesReversed(2));
    }

    public int readIntBigEndian() {
        return Utils.bytesToInt(readSignedBytesReversed(4));
    }

    public int preReadIntBigEndian() {
        return Utils.bytesToInt(preReadSignedBytesReversed(4));
    }

    public long readLongBigEndian() {
        return Utils.bytesToLong(readSignedBytesReversed(8));
    }

    public long preReadLongBigEndian() {
        return Utils.bytesToLong(preReadSignedBytesReversed(8));
    }

    public int readByte() {
        pos++;
        int[] ret = Arrays.copyOfRange(data, 0, 1);
        data = Arrays.copyOfRange(data, 1, data.length);
        return ret[0];
    }

    public int preReadByte() {
        return Arrays.copyOfRange(data, 0, 1)[0];
    }

    public byte readSignedByte() {
        pos++;
        int[] ret = Arrays.copyOfRange(data, 0, 1);
        data = Arrays.copyOfRange(data, 1, data.length);
        return (byte) ret[0];
    }

    public byte preReadSignedByte() {
        return (byte) Arrays.copyOfRange(data, 0, 1)[0];
    }
}
