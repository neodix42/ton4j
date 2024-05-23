package org.ton.java.bitstring;

import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.openjdk.jol.info.ClassLayout;
import org.ton.java.address.Address;

import java.math.BigInteger;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.Assert.assertThrows;

@Slf4j
@RunWith(JUnit4.class)
public class TestRealBitString {
    @Test
    public void testRealBitStringUsedFree() {
        RealBitString bitString = new RealBitString(10);
        bitString.writeBit(true);
        assertThat(bitString.getFreeBits()).isEqualTo(9);
        assertThat(bitString.getUsedBits()).isEqualTo(1);
        assertThat(bitString.getUsedBytes()).isEqualTo(1);
    }

    @Test
    public void testRealBitStringCell() {
        RealBitString bitString = new RealBitString(1023);
        bitString.writeUint(BigInteger.TWO, 32);
        assertThat(bitString.toRealBitString()).isEqualTo("00000000000000000000000000000010");
        assertThat(bitString.toHex()).isEqualTo("00000002");
    }

    @Test
    public void testRealBitStringOutput() {
        RealBitString bitString = new RealBitString(8);
        bitString.writeUint(BigInteger.valueOf(7), 3);
        assertThat(bitString.toRealBitString()).isEqualTo("111");
        assertThat(bitString.toHex()).isEqualTo("F_");

        bitString = new RealBitString(16);
        bitString.writeUint(BigInteger.valueOf(255), 8);
        assertThat(bitString.toRealBitString()).isEqualTo("11111111");
        assertThat(bitString.toHex()).isEqualTo("FF");

        bitString = new RealBitString(32);
        bitString.writeInt(BigInteger.valueOf(20), 6);
        assertThat(bitString.toRealBitString()).isEqualTo("010100");
        assertThat(bitString.toHex()).isEqualTo("52_");
    }

    @Test
    public void testRealBitStringReadUints() {
        RealBitString bitString = new RealBitString(128);
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
    public void testRealBitStringReadInts() {
        RealBitString bitString = new RealBitString(128);
        bitString.writeInt(BigInteger.valueOf(20), 8);
        bitString.writeInt(BigInteger.valueOf(400), 16);
        bitString.writeInt(BigInteger.valueOf(600000), 32);
        bitString.writeInt(new BigInteger("9000000000000"), 64);
        System.out.println(bitString);

        System.out.println(ClassLayout.parseInstance(bitString).toPrintable());

        assertThat(bitString.readInt8().toString(10)).isEqualTo("20");
        assertThat(bitString.readInt16().toString(10)).isEqualTo("400");
        assertThat(bitString.readInt32().toString(10)).isEqualTo("600000");
        assertThat(bitString.readInt64().toString(10)).isEqualTo("9000000000000");
    }

    @Test
    public void testRealBitStringWriteInt() {
        RealBitString bitString = new RealBitString(32);
        bitString.writeInt(BigInteger.valueOf(200), 9);
        assertThat(bitString.toRealBitString()).isEqualTo("011001000");
        assertThat(bitString.toHex()).isEqualTo("644_");

        RealBitString bitStringMax64 = new RealBitString(64); // Long.MAX_VALUE, 8 bytes
        bitStringMax64.writeInt(new BigInteger("9223372036854775807"), 64);
        assertThat(bitStringMax64.toRealBitString()).isEqualTo("0111111111111111111111111111111111111111111111111111111111111111");

        RealBitString bitStringMax128 = new RealBitString(128);
        bitStringMax128.writeInt(new BigInteger("92233720368547758070"), 128);
        assertThat(bitStringMax128.toRealBitString()).isEqualTo("00000000000000000000000000000000000000000000000000000000000001001111111111111111111111111111111111111111111111111111111111110110");

        RealBitString bitStringMaxA = new RealBitString(128);
        bitStringMaxA.writeInt(new BigInteger("99999999999999999999999999999999999999"), 128);
        assertThat(bitStringMaxA.toRealBitString()).isEqualTo("01001011001110110100110010101000010110101000011011000100011110100000100110001010001000100011111111111111111111111111111111111111");

        BigInteger i = bitStringMaxA.readInt(128);
        assertThat(i.toString(16).toUpperCase()).isEqualTo("4B3B4CA85A86C47A098A223FFFFFFFFF");

        RealBitString bitStringMaxB = new RealBitString(256);
        bitStringMaxB.writeInt(new BigInteger("9999999999999999999999999999999999999999999999999999999999"), 256);
        assertThat(bitStringMaxB.toHex()).isEqualTo("000000000000000197D4DF19D605767337E9F14D3EEC8920E3FFFFFFFFFFFFFF");
    }

    @Test
    public void testRealBitStringWriteIntNegative() {
        RealBitString bitString = new RealBitString(32);
        bitString.writeInt(new BigInteger("-20"), 8);
        BigInteger r = bitString.readInt(8);
        assertThat(r.longValue()).isEqualTo(-20);

        RealBitString bitStringMaxC = new RealBitString(256);
        bitStringMaxC.writeInt(new BigInteger("-9999999999999999999999999999999999999999999999999999999999"), 256);
        assertThat(bitStringMaxC.toHex()).isEqualTo("FFFFFFFFFFFFFFFE682B20E629FA898CC8160EB2C11376DF1C00000000000001");
    }

    @Test
    public void testRealBitStringWriteUints() {
        RealBitString bitString = new RealBitString(16);
        bitString.writeUint(BigInteger.valueOf(255), 8);
        assertThat(bitString.toRealBitString()).isEqualTo("11111111");
        assertThat(bitString.toHex()).isEqualTo("FF");

        bitString = new RealBitString(64);
        bitString.writeUint(BigInteger.valueOf(Long.MAX_VALUE), 64);
        assertThat(bitString.toRealBitString()).isEqualTo("0111111111111111111111111111111111111111111111111111111111111111");
        assertThat(bitString.toHex()).isEqualTo("7FFFFFFFFFFFFFFF");

        bitString = new RealBitString(128);
        bitString.writeUint(15, 4);
    }

    @Test
    public void testRealBitStringAddress() {
        Address address01 = Address.of("0QAs9VlT6S776tq3unJcP5Ogsj-ELLunLXuOb1EKcOQi4-QO");
        assertThat(address01.toString(true, true, false)).isEqualTo("0QAs9VlT6S776tq3unJcP5Ogsj-ELLunLXuOb1EKcOQi4-QO");
        RealBitString bitString = new RealBitString(34 * 8); // whole address is 36 bytes. tag[1]+wc[1]+addr[32]+crc[2]
        bitString.writeAddress(address01);
        assertThat(bitString.toHex()).isEqualTo("80059EAB2A7D25DF7D5B56F74E4B87F2741647F0859774E5AF71CDEA214E1C845C7_");
    }

    @Test
    public void testRealBitString8() {
        RealBitString bitString = new RealBitString(8);
        bitString.writeUint8((byte) 7);
        assertThat(bitString.toHex()).isEqualTo("07");
    }

    @Test
    public void testRealBitStringStr() {
        RealBitString bitString = new RealBitString(8);
        bitString.writeString("A");
        assertThat(bitString.toHex()).isEqualTo("41");

        bitString = new RealBitString(16);
        bitString.writeString("A");
        assertThat(bitString.toHex()).isEqualTo("41");
    }

    @Test
    public void testRealBitStringCoins() {
        RealBitString bitString = new RealBitString(16);
        bitString.writeCoins(BigInteger.TEN);
        assertThat(bitString.toHex()).isEqualTo("10A");

        bitString = new RealBitString(32);
        bitString.writeCoins(BigInteger.TEN);
        assertThat(bitString.toHex()).isEqualTo("10A");

        assertThrows(Error.class, () -> {
            RealBitString bitStringA = new RealBitString(32);
            bitStringA.writeCoins(BigInteger.TEN.negate());

            RealBitString bitString128 = new RealBitString(128);
            BigInteger coins = BigInteger.TWO.pow(121).subtract(BigInteger.ONE); // too big amount, max 2^120-1
            bitString128.writeCoins(coins);
        });

        RealBitString bitString128 = new RealBitString(129);
        BigInteger coins = BigInteger.TWO.pow(120).subtract(BigInteger.ONE);
        bitString128.writeCoins(coins);
    }

    @Test
    public void testReverseArrayInt() {
        int l = 0x01020304;
        log.info("reversed {}", Integer.toHexString(Integer.reverseBytes(l)));
        assertThat(Integer.toHexString(Integer.reverseBytes(l))).isEqualTo("4030201");
    }

    @Test
    public void testReverseArrayLong() {
        long l = 0x0102030405060708L;
        log.info("reversed {}", Long.toHexString(Long.reverseBytes(l)));
        assertThat(Long.toHexString(Long.reverseBytes(l))).isEqualTo("807060504030201");
    }

    /*
        @Test
        public void testRealBitStringByteArrayPositive() {
            RealBitString bitString9 = new RealBitString(new byte[]{7, 7, 7, 7});
            assertThat(bitString9.toRealBitString()).isEqualTo("00000111000001110000011100000111");
            assertThat(Utils.bytesToHex(bitString9.toByteArray())).isEqualTo("07070707");
            assertThat(bitString9.toHex()).isEqualTo("07070707");
        }

        @Test
        public void testRealBitStringByteArrayPositiveUnsigned() {
            RealBitString bitString9 = new RealBitString(new byte[]{-126, 7, 7, 7}); // -126 = 130 unsigned
            assertThat(bitString9.toRealBitString()).isEqualTo("10000010000001110000011100000111");
            assertThat(Utils.bytesToHex(bitString9.toUnsignedByteArray())).isEqualTo("82070707");
            assertThat(bitString9.toHex()).isEqualTo("82070707");
        }

        @Test
        public void testRealBitStringByteArray2() {
            RealBitString bitString9 = new RealBitString(new byte[]{-128, 0, 0, 0});
            assertThat(bitString9.toRealBitString()).isEqualTo("10000000000000000000000000000000");
            assertThat(Utils.bytesToHex(bitString9.toByteArray())).isEqualTo("80000000");
            assertThat(bitString9.toHex()).isEqualTo("80000000");
        }

        @Test
        public void testRealBitStringByteArray3() {
            RealBitString bitString9 = new RealBitString(new byte[]{-128, 0, 0, 32});
            assertThat(bitString9.toRealBitString()).isEqualTo("10000000000000000000000000100000");
            assertThat(Utils.bytesToHex(bitString9.toByteArray())).isEqualTo("80000020");
            assertThat(bitString9.toHex()).isEqualTo("80000020");
        }
    */
    @Test
    public void testRealBitStringAll() {
        RealBitString bitString = new RealBitString(1023);
        bitString.writeInt(BigInteger.valueOf(-200), 16);
        bitString.writeUint(BigInteger.valueOf(200), 9);
        bitString.writeCoins(BigInteger.TEN);
        bitString.writeString("A");
        Address address = Address.of("0QAs9VlT6S776tq3unJcP5Ogsj-ELLunLXuOb1EKcOQi4-QO");
        bitString.writeAddress(address);
    }
}
