package org.ton.java.utils;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.DecoderException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.math.BigDecimal;
import java.math.BigInteger;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.Assert.assertThrows;
import static org.ton.java.utils.Utils.getCRC32ChecksumAsLong;

@RunWith(JUnit4.class)
@Slf4j
public class TestUtils {

    private static final long BLN1 = 1000000000L;

    @Test
    public void testBytesToHex() {
        byte[] a = {(byte) 255};
        assertThat(Utils.bytesToHex(a)).isEqualTo("ff");

        byte[] b = {10};
        assertThat(Utils.bytesToHex(b)).isEqualTo("0a");

        byte[] c = {10, 11};
        assertThat(Utils.bytesToHex(c)).isEqualTo("0a0b");

        byte[] d = {10, 11};
        assertThat(Utils.bytesToHex(Utils.reverseByteArray(d))).isEqualTo("0b0a");

        int[] e = {233, 244};
        assertThat(Utils.bytesToHex(Utils.reverseIntArray(e))).isEqualTo("f4e9");

        int[] f = {233, 244};
        assertThat(Utils.bytesToHex(f)).isEqualTo("e9f4");
    }

    @Test
    public void testHexToBytes() {
        byte[] a = "AA".getBytes();
        byte[] b = Utils.hexToSignedBytes("AA");

        assertThat(b[0] & 0xFF).isEqualTo(170);
    }

    @Test
    public void testCrc32cAsBytes2() {
        byte[] boc = Utils.hexToSignedBytes("b5ee9c724101010100620000c0ff0020dd2082014c97ba9730ed44d0d70b1fe0a4f2608308d71820d31fd31fd31ff82313bbf263ed44d0d31fd31fd3ffd15132baf2a15144baf2a204f901541055f910f2a3f8009320d74a96d307d402fb00e8d101a4c8cb1fcb1fcbffc9ed54");
        byte[] crc32 = Utils.getCRC32ChecksumAsBytesReversed(boc);
        String hexCrc32 = Utils.bytesToHex(crc32);
        assertThat(hexCrc32).isEqualTo("3fbe6ee0"); //ok with online, but reversed
    }

    @Test
    public void testCrc32cAsBytes3() {
        byte[] boc = Utils.hexToSignedBytes("B5EE9C72410203010001000002DF880059EAB2A7D25DF7D5B56F74E4B87F2741647F0859774E5AF71CDEA214E1C845C6119529DEF4481C60CD81087FC7B058797AFDCEBCC1BE127EE2C4707C1E1C0F3D12F955EC3DE1C63E714876A931F6C6F13E6980284238AA9F94B0EC5859B37C4DE1E5353462FFFFFFFFE000000010010200C0FF0020DD2082014C97BA9730ED44D0D70B1FE0A4F2608308D71820D31FD31FD31FF82313BBF263ED44D0D31FD31FD3FFD15132BAF2A15144BAF2A204F901541055F910F2A3F8009320D74A96D307D402FB00E8D101A4C8CB1FCB1FCBFFC9ED5400500000000029A9A31782A0B2543D06FEC0AAC952E9EC738BE56AB1B6027FC0C1AA817AE14B4D1ED2FB");
        byte[] crc32 = Utils.getCRC32ChecksumAsBytesReversed(boc);
        String hexCrc32 = Utils.bytesToHex(crc32);
        assertThat(hexCrc32).isEqualTo("56ad484d"); //ok with online, but reversed
    }

    @Test
    public void testCrc32cAsHexReversed() {
        byte[] crc32 = Utils.getCRC32ChecksumAsBytesReversed("ABC".getBytes());
        String hexCrc32 = Utils.bytesToHex(crc32);
        assertThat(hexCrc32).isEqualTo("7fa93988");
    }

    @Test
    public void testCrc32AsHex() {
        byte[] data = "ABC".getBytes();
        Long checksum = getCRC32ChecksumAsLong(data);
        System.out.printf("CRC32C Checksum: 0x%08X\n", checksum);

        String crc32 = Utils.getCRC32ChecksumAsHex("ABC".getBytes());
        assertThat(crc32).isEqualTo("8839a97f");
    }

    @Test
    public void testCrc32AsLong() {
        Long crc32 = getCRC32ChecksumAsLong("ABC".getBytes());
        assertThat(crc32).isEqualTo(2285480319L);
    }

    @Test
    public void testCrc32AsUnsignedBytes() {
        byte[] crc32 = Utils.getCRC32ChecksumAsBytes("ABC".getBytes());
        int[] crc32int = Utils.signedBytesToUnsigned(crc32);
        int[] result = {136, 57, 169, 127};
        assertThat(crc32int[0]).isEqualTo(result[0]);
        assertThat(crc32int[1]).isEqualTo(result[1]);
        assertThat(crc32int[2]).isEqualTo(result[2]);
        assertThat(crc32int[3]).isEqualTo(result[3]);
    }

    @Test
    public void testCrc32AsSignedBytes() {
        byte[] crc32 = Utils.getCRC32ChecksumAsBytes("ABC".getBytes());
        byte[] result = {-120, 57, -87, 127};
        assertThat(crc32[0]).isEqualTo(result[0]);
        assertThat(crc32[1]).isEqualTo(result[1]);
        assertThat(crc32[2]).isEqualTo(result[2]);
        assertThat(crc32[3]).isEqualTo(result[3]);
    }

    @Test
    public void testLongToBytes() {
        long a = 0x1111111111111111L;
        int[] longBytes = Utils.longToBytes(a);
        int[] result = {17, 17, 17, 17, 17, 17, 17, 17};
        assertThat(longBytes[0]).isEqualTo(result[0]);
        assertThat(longBytes[1]).isEqualTo(result[1]);
        assertThat(longBytes[2]).isEqualTo(result[2]);
        assertThat(longBytes[3]).isEqualTo(result[3]);
        assertThat(longBytes[4]).isEqualTo(result[4]);
        assertThat(longBytes[5]).isEqualTo(result[5]);
        assertThat(longBytes[6]).isEqualTo(result[6]);
        assertThat(longBytes[7]).isEqualTo(result[7]);
    }

    @Test
    public void testCrc16AsHex() {
        byte[] a = "ABC".getBytes();
        String crc16 = Utils.getCRC16ChecksumAsHex(a);
        assertThat(crc16).isEqualTo("3994");
    }

    @Test
    public void testCrc16AsInt() {
        byte[] a = "ABC".getBytes();
        int crc16 = Utils.getCRC16ChecksumAsInt(a);
        assertThat(crc16).isEqualTo(14740);
    }

    @Test
    public void testCrc16AsBytes() {
        byte[] a = "ABC".getBytes();
        byte[] crc16 = Utils.getCRC16ChecksumAsBytes(a);
        byte[] result = {57, -108};
        assertThat(crc16).isEqualTo(result);
    }

    @Test
    public void testCrc16Sanity() {
        assertThat(Utils.getCRC16ChecksumAsBytes("".getBytes())).isEqualTo(new byte[]{0, 0});
        assertThat(Utils.getCRC16ChecksumAsBytes("123456789".getBytes())).isEqualTo(new byte[]{0x31, (byte) 0xC3});
        assertThat(Utils.getCRC16ChecksumAsBytes("abc".getBytes())).isEqualTo(new byte[]{(byte) 0x9D, (byte) 0xD6});
        assertThat(Utils.getCRC16ChecksumAsBytes("ABC".getBytes())).isEqualTo(new byte[]{(byte) 0x39, (byte) 0x94});
        assertThat(Utils.getCRC16ChecksumAsBytes("This is a string".getBytes())).isEqualTo(new byte[]{(byte) 0x21, (byte) 0xE3});
    }

    @Test
    public void testBase64ToBinaryString() {
        String base64 = "ECAw";
        String binaryStr = Utils.base64ToBitString(base64);
        assertThat(binaryStr).isEqualTo("100000010000000110000");
    }

    @Test
    public void testBinaryStringToBase64() throws DecoderException {

        String binary = "1010101111001101"; //ABCD
        String base64 = Utils.bitStringToBase64(binary);
        assertThat(base64).isEqualTo("q80=");

        String binary2 = "1010101111001101111110111111111100000111111111100001111110111";
        String base642 = Utils.bitStringToBase64(binary2);
        assertThat(base642).isEqualTo("q837/wf+H7g=");
        String base642UrlSafe = Utils.bitStringToBase64UrlSafe(binary2);
        assertThat(base642UrlSafe).isEqualTo("q837_wf-H7g=");

        String binary3 = "10101011110011011"; //not odd length, pad it
        String base643 = Utils.bitStringToBase64(binary3);
        assertThat(base643).isEqualTo("q82A");

        String binary4 = "1010101111001101111110111111111100000111111111100001111110111101010111100110111111011111111110000011111111110000111111011110101011110011011111101111111111000001111111111000011111101111010101111001101111110111111111111111000111000000111001011010101010101010101111100000"; //not odd length, pad it
        String base644 = Utils.bitStringToBase64(binary4);
        assertThat(base644).isEqualTo("q837/wf+H71eb9/4P/D96vN+/8H/h+9Xm/f/8cDlqqq+AA==");

        String binary5 = "hello world"; //not odd length, pad it
        String base645 = Utils.bytesToBase64(binary5.getBytes());
        assertThat(base645).isEqualTo("aGVsbG8gd29ybGQ=");
    }

    @Test
    public void testHexStringToBase64() throws DecoderException {
        String hex = "ABCD";
        String base64 = Utils.hexStringToBase64(hex);
        assertThat(base64).isEqualTo("q80=");

        String hex2 = "d1002cf55953e92efbeadab7ba725c3f93a0b23f842cbba72d7b8e6f510a70e422e3e40e";
        String base642 = Utils.hexStringToBase64(hex2);
        assertThat(base642).isEqualTo("0QAs9VlT6S776tq3unJcP5Ogsj+ELLunLXuOb1EKcOQi4+QO");
    }

    @Test
    public void testHex() {
        assertThat(Utils.bytesToHex("Hello, World!".getBytes())).isEqualTo("48656c6c6f2c20576f726c6421");
        assertThat(Utils.bytesToHex("TON".getBytes())).isEqualTo("544f4e");
        assertThat(Utils.bytesToHex("TON is a fully decentralized layer-1 blockchain designed by Telegram to onboard billions of users. It boasts ultra-fast transactions, tiny fees, easy-to-use apps, and is environmentally friendly.".getBytes())).isEqualTo("544f4e20697320612066756c6c7920646563656e7472616c697a6564206c617965722d3120626c6f636b636861696e2064657369676e65642062792054656c656772616d20746f206f6e626f6172642062696c6c696f6e73206f662075736572732e20497420626f6173747320756c7472612d66617374207472616e73616374696f6e732c2074696e7920666565732c20656173792d746f2d75736520617070732c20616e6420697320656e7669726f6e6d656e74616c6c7920667269656e646c792e");
    }

    @Test
    public void testBase64ToHexString() {
        String base64 = "aGVsbG8gZXZlcnlvbmUgdGhpcyBpcyBoIHRvIHRoZSB1c2t5IGh1c2t5IGhlcmU";
        String binaryStr = Utils.base64ToHexString(base64);
        assertThat(binaryStr).isEqualTo("68656c6c6f2065766572796f6e652074686973206973206820746f207468652075736b79206875736b792068657265");
    }

    @Test
    public void testBase64ToHexString2() {
        String base64 = "0QAs9VlT6S776tq3unJcP5Ogsj-ELLunLXuOb1EKcOQi4-QO";
        String binaryStr = Utils.base64UrlSafeToHexString(base64);
        assertThat(binaryStr).isEqualTo("d1002cf55953e92efbeadab7ba725c3f93a0b23f842cbba72d7b8e6f510a70e422e3e40e");
    }

    @Test
    public void testBase64ToHexString3() {
        String base64 = "0QAs9VlT6S776tq3unJcP5Ogsj+ELLunLXuOb1EKcOQi4+QO";
        String binaryStr = Utils.base64ToHexString(base64);
        assertThat(binaryStr).isEqualTo("d1002cf55953e92efbeadab7ba725c3f93a0b23f842cbba72d7b8e6f510a70e422e3e40e");
    }

    @Test
    public void testBase64ToBytes() {
        String base64 = "aGVsbG8gZXZlcnlvbmUgdGhpcyBpcyBoIHRvIHRoZSB1c2t5IGh1c2t5IGhlcmU";
        byte[] data = Utils.base64ToBytes(base64);
        assertThat(data).isEqualTo("hello everyone this is h to the usky husky here".getBytes());
    }

    @Test
    public void testBase64ToString() {
        String base64 = "aGVsbG8gZXZlcnlvbmUgdGhpcyBpcyBoIHRvIHRoZSB1c2t5IGh1c2t5IGhlcmU=";
        String string = Utils.base64ToString(base64);
        assertThat(string).isEqualTo("hello everyone this is h to the usky husky here");
    }

    @Test
    public void testSignedBytesToBitString() {
        byte[] a = {-65};
        assertThat(Utils.bytesToBitString(a)).isEqualTo("10111111");

        byte[] b = {-65, -66};
        assertThat(Utils.bytesToBitString(b)).isEqualTo("1011111110111110");
    }

    @Test
    public void testUnsignedBytesToBitString() {
        byte[] a = {65};
        assertThat(Utils.bytesToBitString(a)).isEqualTo("1000001");

        byte[] b = {65, 66};
        assertThat(Utils.bytesToBitString(b)).isEqualTo("100000101000010");
    }

    @Test
    public void testConcatBytes() {
        byte[] a = {10, 20, 30};
        byte[] b = {40, 50, 60};
        byte[] c = {10, 20, 30, 40, 50, 60};
        assertThat(Utils.concatBytes(a, b)).isEqualTo(c);
    }

    @Test
    public void testCeil() {
        BigInteger amount = BigInteger.TEN;
        double a = Math.ceil(amount.toString(16).length() / (double) 2);
        assertThat(a).isEqualTo(1);

        double c = Math.ceil(7 / (double) 8);
        assertThat(c).isEqualTo(1);
    }

    @Test
    public void testNano() {
        BigInteger i1 = Utils.toNano(2);
        assertThat(i1).isEqualTo(BigInteger.valueOf(2000000000L));

        BigInteger i2 = Utils.toNano(2.2);
        assertThat(i2).isEqualTo(BigInteger.valueOf(2200000000L));

        BigInteger i3 = Utils.toNano(2684354.2684513200);
        assertThat(i3).isEqualTo(BigInteger.valueOf(2684354268451320L));

        BigInteger i4 = Utils.toNano(5L * BLN1);

        assertThat(i4).isEqualTo(BigInteger.valueOf(5 * BLN1 * BLN1));
        assertThat(i4.toString()).isEqualTo("5000000000000000000");

        BigInteger i7 = Utils.toNano("2");
        assertThat(i7).isEqualTo(BigInteger.valueOf(2000000000L));

        BigInteger i8 = Utils.toNano("2.3");
        assertThat(i8).isEqualTo(BigInteger.valueOf(2300000000L));

        BigInteger i9 = Utils.toNano("2684354.2684513200");
        assertThat(i9).isEqualTo(BigInteger.valueOf(2684354268451320L));

        BigDecimal d1 = Utils.fromNano(2684354268451321234L);
        assertThat(d1).isEqualTo(new BigDecimal("2684354268.451321234"));

        log.info(Utils.formatNanoValue("2684354268451321234"));

        BigDecimal d2 = Utils.fromNano(968434L);
        assertThat(d2).isEqualTo(new BigDecimal("0.000968434"));
    }

    @Test
    public void testNanoFail() {
        assertThrows(Error.class, () -> {
            BigInteger i5 = Utils.toNano(2684354.26845132123456789); // picks only 9 digits after decimal point
            assertThat(i5).isEqualTo(BigInteger.valueOf(2684354268451321L));

            BigInteger i6 = Utils.toNano(BigDecimal.valueOf(2684354.26845132123456789)); // picks only 9 digits after decimal point
            assertThat(i6).isEqualTo(BigInteger.valueOf(2684354268451321L));
        });
    }

    @Test
    public void testReverseLong() {
        long l = 0x27e7c64a;
        log.info("reversed " + Long.toHexString(Long.reverseBytes(l)));
    }

}
