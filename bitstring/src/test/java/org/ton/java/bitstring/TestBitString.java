package org.ton.java.bitstring;

import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.ton.java.address.Address;
import org.ton.java.utils.Utils;

import java.math.BigInteger;
import java.util.Arrays;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.Assert.assertThrows;

@Slf4j
@RunWith(JUnit4.class)
public class TestBitString {
    @Test
    public void testBitStringUsedFree() {
        BitString bitString = new BitString(10);
        bitString.writeBit(true);
        assertThat(bitString.getFreeBits()).isEqualTo(9);
        assertThat(bitString.getUsedBits()).isEqualTo(1);
        assertThat(bitString.getUsedBytes()).isEqualTo(1);
    }

    @Test
    public void testBitStringCell() {
        BitString bitString = new BitString(1023);
        bitString.writeUint(BigInteger.TWO, 32);
        assertThat(bitString.toBitString()).isEqualTo("00000000000000000000000000000010");
        assertThat(bitString.toHex()).isEqualTo("00000002");
    }

    @Test
    public void testBitStringOutput() {
        BitString bitString = new BitString(8);
        bitString.writeUint(BigInteger.valueOf(7), 3);
        assertThat(bitString.toBitString()).isEqualTo("111");
        assertThat(bitString.toHex()).isEqualTo("F_");

        bitString = new BitString(16);
        bitString.writeUint(BigInteger.valueOf(255), 8);
        assertThat(bitString.toBitString()).isEqualTo("11111111");
        assertThat(bitString.toHex()).isEqualTo("FF");

        bitString = new BitString(32);
        bitString.writeInt(BigInteger.valueOf(20), 6);
        assertThat(bitString.toBitString()).isEqualTo("010100");
        assertThat(bitString.toHex()).isEqualTo("52_");
    }

    @Test
    public void testBitStringReadUints() {
        BitString bitString = new BitString(128);
        bitString.writeUint(BigInteger.valueOf(200), 8);
        bitString.writeUint(BigInteger.valueOf(400), 16);
        bitString.writeUint(BigInteger.valueOf(600000), 32);
        bitString.writeUint(new BigInteger("9000000000000"), 64);
        assertThat(bitString.readUint8().toString(10)).isEqualTo("200");
        assertThat(bitString.readUint16().toString(10)).isEqualTo("400");
        assertThat(bitString.readUint32().toString(10)).isEqualTo("600000");
        assertThat(bitString.readUint64().toString(10)).isEqualTo("9000000000000");
    }

    @Test
    public void testBitStringReadInts() {
        BitString bitString = new BitString(128);
        bitString.writeInt(BigInteger.valueOf(20), 8);
        bitString.writeInt(BigInteger.valueOf(400), 16);
        bitString.writeInt(BigInteger.valueOf(600000), 32);
        bitString.writeInt(new BigInteger("9000000000000"), 64);
        assertThat(bitString.readInt8().toString(10)).isEqualTo("20");
        assertThat(bitString.readInt16().toString(10)).isEqualTo("400");
        assertThat(bitString.readInt32().toString(10)).isEqualTo("600000");
        assertThat(bitString.readInt64().toString(10)).isEqualTo("9000000000000");
    }

    @Test
    public void testBitStringWriteInt() {
        BitString bitString = new BitString(32);
        bitString.writeInt(BigInteger.valueOf(200), 9);
        assertThat(bitString.toBitString()).isEqualTo("011001000");
        assertThat(bitString.toHex()).isEqualTo("644_");

        BitString bitStringMax64 = new BitString(64); // Long.MAX_VALUE, 8 bytes
        bitStringMax64.writeInt(new BigInteger("9223372036854775807"), 64);
        assertThat(bitStringMax64.toBitString()).isEqualTo("0111111111111111111111111111111111111111111111111111111111111111");

        BitString bitStringMax128 = new BitString(128);
        bitStringMax128.writeInt(new BigInteger("92233720368547758070"), 128);
        assertThat(bitStringMax128.toBitString()).isEqualTo("00000000000000000000000000000000000000000000000000000000000001001111111111111111111111111111111111111111111111111111111111110110");

        BitString bitStringMaxA = new BitString(128);
        bitStringMaxA.writeInt(new BigInteger("99999999999999999999999999999999999999"), 128);
        assertThat(bitStringMaxA.toBitString()).isEqualTo("01001011001110110100110010101000010110101000011011000100011110100000100110001010001000100011111111111111111111111111111111111111");

        BigInteger i = bitStringMaxA.readInt(128);
        assertThat(i.toString(16).toUpperCase()).isEqualTo("4B3B4CA85A86C47A098A223FFFFFFFFF");

        BitString bitStringMaxB = new BitString(256);
        bitStringMaxB.writeInt(new BigInteger("9999999999999999999999999999999999999999999999999999999999"), 256);
        assertThat(bitStringMaxB.toHex()).isEqualTo("000000000000000197D4DF19D605767337E9F14D3EEC8920E3FFFFFFFFFFFFFF");
    }

    @Test
    public void testBitStringWriteIntNegative() {
        BitString bitString = new BitString(32);
        bitString.writeInt(new BigInteger("-20"), 8);
        BigInteger r = bitString.readInt(8);
        assertThat(r.longValue()).isEqualTo(-20);

        BitString bitStringMaxC = new BitString(256);
        bitStringMaxC.writeInt(new BigInteger("-9999999999999999999999999999999999999999999999999999999999"), 256);
        assertThat(bitStringMaxC.toHex()).isEqualTo("FFFFFFFFFFFFFFFE682B20E629FA898CC8160EB2C11376DF1C00000000000001");
    }

    @Test
    public void testBitStringWriteUints() {
        BitString bitString = new BitString(16);
        bitString.writeUint(BigInteger.valueOf(255), 8);
        assertThat(bitString.toBitString()).isEqualTo("11111111");
        assertThat(bitString.toHex()).isEqualTo("FF");

        bitString = new BitString(64);
        bitString.writeUint(BigInteger.valueOf(Long.MAX_VALUE), 64);
        assertThat(bitString.toBitString()).isEqualTo("0111111111111111111111111111111111111111111111111111111111111111");
        assertThat(bitString.toHex()).isEqualTo("7FFFFFFFFFFFFFFF");

        bitString = new BitString(128);
        bitString.writeUint(15, 4);
    }

    @Test
    public void testBitStringAddress() {
        Address address01 = Address.of("0QAs9VlT6S776tq3unJcP5Ogsj-ELLunLXuOb1EKcOQi4-QO");
        assertThat(address01.toString()).isEqualTo("0QAs9VlT6S776tq3unJcP5Ogsj-ELLunLXuOb1EKcOQi4-QO");
        BitString bitString = new BitString(34 * 8); // whole address is 36 bytes. tag[1]+wc[1]+addr[32]+crc[2]
        bitString.writeAddress(address01);
        assertThat(bitString.toHex()).isEqualTo("80059EAB2A7D25DF7D5B56F74E4B87F2741647F0859774E5AF71CDEA214E1C845C7_");
    }

    @Test
    public void testBitString8() {
        BitString bitString = new BitString(8);
        bitString.writeUint8((byte) 7);
        assertThat(bitString.toHex()).isEqualTo("07");
    }

    @Test
    public void testBitStringStr() {
        BitString bitString = new BitString(8);
        bitString.writeString("A");
        assertThat(bitString.toHex()).isEqualTo("41");

        bitString = new BitString(16);
        bitString.writeString("A");
        assertThat(bitString.toHex()).isEqualTo("41");
    }

    @Test
    public void testBitStringCoins() {
        BitString bitString = new BitString(16);
        bitString.writeCoins(BigInteger.TEN);
        assertThat(bitString.toHex()).isEqualTo("10A");

        bitString = new BitString(32);
        bitString.writeCoins(BigInteger.TEN);
        assertThat(bitString.toHex()).isEqualTo("10A");

        assertThrows(Error.class, () -> {
            BitString bitStringA = new BitString(32);
            bitStringA.writeCoins(BigInteger.TEN.negate());

            BitString bitString128 = new BitString(128);
            BigInteger coins = BigInteger.TWO.pow(121).subtract(BigInteger.ONE); // too big amount, max 2^120-1
            bitString128.writeCoins(coins);
        });

        BitString bitString128 = new BitString(129);
        BigInteger coins = BigInteger.TWO.pow(120).subtract(BigInteger.ONE);
        bitString128.writeCoins(coins);
    }

    @Test
    public void testBitStringGetTopUppedArray() {
        BitString bitString0 = new BitString(8);
        bitString0.writeUint(BigInteger.valueOf(200), 8);
        int[] b0 = bitString0.getTopUppedArray();
        int[] result0 = {200}; // (-56 & 0xff) = 200
        assertThat(b0).isEqualTo(result0);

        BitString bitString1 = new BitString(32);
        bitString1.writeUint(BigInteger.valueOf(200), 32);
        int[] b1 = bitString1.getTopUppedArray();
        int[] result1 = {0, 0, 0, 200};
        assertThat(b1).isEqualTo(result1);

        BitString bitString2 = new BitString(32);
        bitString2.writeCoins(BigInteger.TEN);
        int[] b2 = bitString2.getTopUppedArray();
        int[] result2 = {16, 168};
        assertThat(b2).isEqualTo(result2);

        BitString bitString3 = new BitString(1023);
        bitString3.writeCoins(BigInteger.TEN);
        bitString3.writeUint(333L, 64);
        int[] b3 = bitString3.getTopUppedArray();
        int[] result3 = {16, 160, 0, 0, 0, 0, 0, 0, 20, 216};
        assertThat(b3).isEqualTo(result3);
    }

    @Test
    public void testBitStringSetTopUppedArray() {
        BitString bitString0 = new BitString(8);
        bitString0.writeUint(BigInteger.valueOf(200), 8);
        System.out.println(bitString0);
        bitString0.setTopUppedArray(Utils.signedBytesToUnsigned(new byte[]{16, -96, 0, 0, 0, 0, 0, 0, 20, -40}), false);
        System.out.println(bitString0);
        assertThat(bitString0.toString()).isEqualTo("0001000010100000000000000000000000000000000000000000000000000000000101001101");

        BitString bitString1 = new BitString(8);
        bitString1.writeUint(BigInteger.valueOf(200), 8);
        System.out.println(bitString1);
        bitString1.setTopUppedArray(new int[]{16}, false);
        System.out.println(bitString1);
        assertThat(bitString1.toString()).isEqualTo("000");

        BitString bitString2 = new BitString(8);
        bitString2.writeUint(BigInteger.valueOf(200), 8);
        System.out.println(bitString2);
        bitString2.setTopUppedArray(new int[]{16}, true);
        System.out.println(bitString2);
        assertThat(bitString2.toString()).isEqualTo("00010000");

        BitString bitString3 = new BitString(1023);
        bitString3.writeUint(BigInteger.valueOf(200), 8);
        bitString3.writeCoins(Utils.toNano(200));
        System.out.println(bitString3);
        bitString3.setTopUppedArray(new int[]{16}, true);
        System.out.println(bitString3);
        assertThat(bitString3.toString()).isEqualTo("00010000");

        BitString bitString4 = new BitString(1023);
        bitString4.writeUint(BigInteger.valueOf(200), 8);
        bitString4.writeCoins(Utils.toNano(200));
        System.out.println(bitString4);
        bitString4.setTopUppedArray(new int[]{16}, false);
        System.out.println(bitString4);
        assertThat(bitString4.toString()).isEqualTo("000");

        BitString bitString5 = new BitString(1023);
        bitString5.writeUint(BigInteger.valueOf(200), 8);
        bitString5.writeCoins(Utils.toNano(200));
        System.out.println(bitString5);
        bitString5.setTopUppedArray(Utils.signedBytesToUnsigned(new byte[]{16, 1, 0, 0, 13, -8, 15}), false);
        System.out.println(bitString5);
        assertThat(bitString5.toString()).isEqualTo("0001000000000001000000000000000000001101111110000000111");

        BitString bitString6 = new BitString(1023);
        bitString6.setTopUppedArray(Arrays.copyOfRange(Utils.signedBytesToUnsigned(new byte[]{0, 1, 2, 0, 1, 85, 0, 63, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, -109}), 0, 1), true);
        System.out.println(bitString6);
        assertThat(bitString6.toString()).isEqualTo("00000000");

        BitString bitString7 = new BitString(1023);
        bitString7.writeUint(BigInteger.valueOf(200), 8);
        bitString7.setTopUppedArray(Arrays.copyOfRange(Utils.signedBytesToUnsigned(new byte[]{0, 1, 2, 0, 1, 85, 0, 63, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, -109}), 0, 1), true);
        System.out.println(bitString7);
        assertThat(bitString7.toString()).isEqualTo("00000000");

        BitString bitString8 = new BitString(1023);
        bitString8.setTopUppedArray(Arrays.copyOfRange(Utils.signedBytesToUnsigned(new byte[]{0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, -109}), 0, 32), false);
        System.out.println(bitString8);
        assertThat(bitString8.toString()).isEqualTo("000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000001001001");
    }

    @Test
    public void testBitStringByteArrayPositive() {
        BitString bitString9 = new BitString(new byte[]{7, 7, 7, 7});

        System.out.println(bitString9.toBitString());

        assertThat(bitString9.toBitString()).isEqualTo("00000111000001110000011100000111");
        assertThat(Utils.bytesToHex(bitString9.toByteArray())).isEqualTo("07070707");
        assertThat(bitString9.toHex()).isEqualTo("07070707");
    }

    @Test
    public void testBitStringByteArrayPositiveUnsigned() {
        BitString bitString9 = new BitString(new byte[]{-126, 7, 7, 7}); // -126 = 130 unsigned

        System.out.println(bitString9.toBitString());

        assertThat(bitString9.toBitString()).isEqualTo("10000010000001110000011100000111");
        assertThat(Utils.bytesToHex(bitString9.toUnsignedByteArray())).isEqualTo("82070707");
        assertThat(bitString9.toHex()).isEqualTo("82070707");
    }

    @Test
    public void testBitStringByteArray2() {
        BitString bitString9 = new BitString(new byte[]{-128, 0, 0, 0});

        System.out.println(bitString9.toBitString());

//        assertThat(bitString9.toBitString()).isEqualTo("-10000000000000000000000000000000");
        assertThat(bitString9.toBitString()).isEqualTo("10000000000000000000000000000000");
        assertThat(Utils.bytesToHex(bitString9.toByteArray())).isEqualTo("80000000");
        assertThat(bitString9.toHex()).isEqualTo("80000000");
    }

    @Test
    public void testBitStringByteArray3() {
        BitString bitString9 = new BitString(new byte[]{-128, 0, 0, 32});

        System.out.println(bitString9.toBitString());

        assertThat(bitString9.toBitString()).isEqualTo("10000000000000000000000000100000");
        assertThat(Utils.bytesToHex(bitString9.toByteArray())).isEqualTo("80000020");
        assertThat(bitString9.toHex()).isEqualTo("80000020");
    }

    @Test
    public void testBitStringAll() {
        BitString bitString = new BitString(1023);
        bitString.writeInt(BigInteger.valueOf(-200), 16);
        bitString.writeUint(BigInteger.valueOf(200), 9);
        bitString.writeCoins(BigInteger.TEN);
        bitString.writeString("A");
        Address address = Address.of("0QAs9VlT6S776tq3unJcP5Ogsj-ELLunLXuOb1EKcOQi4-QO");
        bitString.writeAddress(address);
    }
}
