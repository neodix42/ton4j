package org.ton.ton4j.cell;

import org.ton.ton4j.utils.Utils;

import java.util.Arrays;

public class UnsignedByteReader {
    byte[] data;
    public int pos;

    public long getDataSize() {
        return data.length;
    }

    public UnsignedByteReader(int[] data) {
        this.data = Utils.unsignedBytesToSigned(data);
    }

    public UnsignedByteReader(byte[] data) {
        this.data = Arrays.copyOfRange(data, 0, data.length);
    }

    public byte[] readBytes(long num) {
        pos += num;
        byte[] ret = Arrays.copyOfRange(data, 0, (int) num);
        data = Arrays.copyOfRange(data, (int) num, data.length);
        return ret;
    }

    public byte[] preReadBytes(long num) {
        return Arrays.copyOfRange(data, 0, (int) num);
    }

    public byte[] readBytes() {
        pos += data.length;
        byte[] ret = Arrays.copyOfRange(data, 0, data.length);
        data = new byte[0];
        return ret;
    }

    /**
     * Read all bytes without
     *
     * @return byte[]
     */
    public byte[] preReadBytes() {
        return Arrays.copyOfRange(data, 0, data.length);
    }

    public byte[] readBytesPos(long num) {
        byte[] ret = Arrays.copyOfRange(data, pos, (int) num);
        pos += num;
        return ret;
    }

    public byte[] readBytesReversed(long num) {
        return Utils.reverseByteArray(readBytes(num));
    }

    public byte[] readSignedBytes(long num) {
        pos += num;
        byte[] ret = Arrays.copyOfRange(data, 0, (int) num);
        data = Arrays.copyOfRange(data, (int) num, data.length);
        return ret;
    }

    public byte[] preReadSignedBytes(long num) {
        return Arrays.copyOfRange(data, 0, (int) num);
    }

    public byte[] readSignedBytesReversed(long num) {
        return Utils.reverseByteArray(readSignedBytes(num));
    }

    public byte[] preReadSignedBytesReversed(long num) {
        return Utils.reverseByteArray(preReadSignedBytes(num));
    }

    public short readShortLittleEndian() {
        return Utils.bytesToShort(readSignedBytesReversed(2));
    }

    public short preReadShortLittleEndian() {
        return Utils.bytesToShort(preReadSignedBytesReversed(2));
    }

    public int readIntLittleEndian() {
        return Utils.bytesToInt(readSignedBytesReversed(4));
    }

    public int preReadIntLittleEndian() {
        return Utils.bytesToInt(preReadSignedBytesReversed(4));
    }

    public long readLongLittleEndian() {
        return Utils.bytesToLong(readSignedBytesReversed(8));
    }

    public long preReadLongLittleEndian() {
        return Utils.bytesToLong(preReadSignedBytesReversed(8));
    }

    public int readByte() {
        pos++;
        byte[] ret = Arrays.copyOfRange(data, 0, 1);
        data = Arrays.copyOfRange(data, 1, data.length);
        return ret[0];
    }

    public int preReadByte() {
        return Arrays.copyOfRange(data, 0, 1)[0];
    }

    public byte readSignedByte() {
        pos++;
        byte[] ret = Arrays.copyOfRange(data, 0, 1);
        data = Arrays.copyOfRange(data, 1, data.length);
        return (byte) ret[0];
    }

    public byte preReadSignedByte() {
        return (byte) Arrays.copyOfRange(data, 0, 1)[0];
    }
}
