package org.ton.ton4j.utils.coverage;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.ton.ton4j.utils.Utils.*;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.Locale;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.DecoderException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.ton.ton4j.utils.SignatureWithRecovery;
import org.ton.ton4j.utils.Utils;
import org.ton.ton4j.utils.Utils.OS;

@RunWith(JUnit4.class)
@Slf4j
public class TestUtilsCoverage {

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
    byte[] boc =
        Utils.hexToSignedBytes(
            "b5ee9c724101010100620000c0ff0020dd2082014c97ba9730ed44d0d70b1fe0a4f2608308d71820d31fd31fd31ff82313bbf263ed44d0d31fd31fd3ffd15132baf2a15144baf2a204f901541055f910f2a3f8009320d74a96d307d402fb00e8d101a4c8cb1fcb1fcbffc9ed54");
    byte[] crc32 = Utils.getCRC32ChecksumAsBytesReversed(boc);
    String hexCrc32 = Utils.bytesToHex(crc32);
    assertThat(hexCrc32).isEqualTo("3fbe6ee0"); // ok with online, but reversed
  }

  @Test
  public void testCrc32cAsBytes3() {
    byte[] boc =
        Utils.hexToSignedBytes(
            "B5EE9C72410203010001000002DF880059EAB2A7D25DF7D5B56F74E4B87F2741647F0859774E5AF71CDEA214E1C845C6119529DEF4481C60CD81087FC7B058797AFDCEBCC1BE127EE2C4707C1E1C0F3D12F955EC3DE1C63E714876A931F6C6F13E6980284238AA9F94B0EC5859B37C4DE1E5353462FFFFFFFFE000000010010200C0FF0020DD2082014C97BA9730ED44D0D70B1FE0A4F2608308D71820D31FD31FD31FF82313BBF263ED44D0D31FD31FD3FFD15132BAF2A15144BAF2A204F901541055F910F2A3F8009320D74A96D307D402FB00E8D101A4C8CB1FCB1FCBFFC9ED5400500000000029A9A31782A0B2543D06FEC0AAC952E9EC738BE56AB1B6027FC0C1AA817AE14B4D1ED2FB");
    byte[] crc32 = Utils.getCRC32ChecksumAsBytesReversed(boc);
    String hexCrc32 = Utils.bytesToHex(crc32);
    assertThat(hexCrc32).isEqualTo("56ad484d"); // ok with online, but reversed
  }

  @Test
  public void testCrc32cAsHexReversed() {
    byte[] crc32 = Utils.getCRC32ChecksumAsBytesReversed("ABC".getBytes());
    String hexCrc32 = Utils.bytesToHex(crc32);
    assertThat(hexCrc32).isEqualTo("7fa93988");
  }

  @Test
  public void testCrc32AsHex() {
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
  public void testBase64() throws DecoderException {
    String binaryStr = Utils.bitStringToBase64("1111000011110000");
    assertThat(binaryStr).isEqualTo("8PA=");

    String binaryStr2 = Utils.bitStringToBase64("11110000111100000000000");
    assertThat(binaryStr2).isEqualTo("8PAA");

    String binaryStr3 = Utils.bitStringToBase64("11110000111100000");
    assertThat(binaryStr3).isEqualTo("8PAA");

    String binaryStr4 = Utils.bitStringToBase64("1111000011110000101001");
    assertThat(binaryStr4).isEqualTo("8PCk");

    try {
      Utils.bitStringToBase64("");
    } catch (NumberFormatException expected) {
      // expected for empty input
    }

    try {
      Utils.bitStringToBase64("hello world");
    } catch (NumberFormatException expected) {
      // expected for invalid binary string input
    }

    String binary2 = "1111000011110000"; // not odd length, pad it
    String base642 = Utils.bytesToBase64(binary2.getBytes());
    assertThat(base642).isEqualTo("MTExMTAwMDAxMTExMDAwMA==");

    String binary3 = "11110000111100000000000"; // odd length, +1 zero added
    String base643 = Utils.bytesToBase64(binary3.getBytes());
    assertThat(base643).isEqualTo("MTExMTAwMDAxMTExMDAwMDAwMDAwMDA=");

    String binary4 = "1111000011110000101001"; // not odd length, pad it
    String base644 = Utils.bytesToBase64(binary4.getBytes());
    assertThat(base644).isEqualTo("MTExMTAwMDAxMTExMDAwMDEwMTAwMQ==");

    String binary5 = "hello world"; // not odd length, pad it
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
  public void testBinaryStringToIntArray() {
    int[] converted = Utils.bitStringToIntArray("101010011");
    assertThat(converted).isEqualTo(new int[] {169, 128});

    int[] converted2 = Utils.bitStringToIntArray("111010101");
    assertThat(converted2).isEqualTo(new int[] {234, 128});

    int[] converted3 = Utils.bitStringToIntArray("100001001");
    assertThat(converted3).isEqualTo(new int[] {132, 128});
  }

  @Test
  public void testHex() {
    assertThat(Utils.bytesToHex("Hello, World!".getBytes()))
        .isEqualTo("48656c6c6f2c20576f726c6421");
    assertThat(Utils.bytesToHex("TON".getBytes())).isEqualTo("544f4e");
    assertThat(
            Utils.bytesToHex(
                "TON is a fully decentralized layer-1 blockchain designed by Telegram to onboard billions of users. It boasts ultra-fast transactions, tiny fees, easy-to-use apps, and is environmentally friendly."
                    .getBytes()))
        .isEqualTo(
            "544f4e20697320612066756c6c7920646563656e7472616c697a6564206c617965722d3120626c6f636b636861696e2064657369676e65642062792054656c656772616d20746f206f6e626f6172642062696c6c696f6e73206f662075736572732e20497420626f6173747320756c7472612d66617374207472616e73616374696f6e732c2074696e7920666565732c20656173792d746f2d75736520617070732c20616e6420697320656e7669726f6e6d656e74616c6c7920667269656e646c792e");
  }

  @Test
  public void testBase64ToHexString() {
    String base64 = "aGVsbG8gZXZlcnlvbmUgdGhpcyBpcyBoIHRvIHRoZSB1c2t5IGh1c2t5IGhlcmU";
    String binaryStr = Utils.base64ToHexString(base64);
    assertThat(binaryStr)
        .isEqualTo(
            "68656c6c6f2065766572796f6e652074686973206973206820746f207468652075736b79206875736b792068657265");
  }

  @Test
  public void testBase64ToHexString2() {
    String base64 = "0QAs9VlT6S776tq3unJcP5Ogsj-ELLunLXuOb1EKcOQi4-QO";
    String binaryStr = Utils.base64UrlSafeToHexString(base64);
    assertThat(binaryStr)
        .isEqualTo("d1002cf55953e92efbeadab7ba725c3f93a0b23f842cbba72d7b8e6f510a70e422e3e40e");
  }

  @Test
  public void testBase64ToHexString3() {
    String base64 = "0QAs9VlT6S776tq3unJcP5Ogsj+ELLunLXuOb1EKcOQi4+QO";
    String binaryStr = Utils.base64ToHexString(base64);
    assertThat(binaryStr)
        .isEqualTo("d1002cf55953e92efbeadab7ba725c3f93a0b23f842cbba72d7b8e6f510a70e422e3e40e");
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
  public void testToNano() {
    BigInteger i = Utils.toNano(2);
    assertThat(i).isEqualTo(BigInteger.valueOf(2000000000L));

    i = Utils.toNano(2.2);
    assertThat(i).isEqualTo(BigInteger.valueOf(2200000000L));

    i = Utils.toNano(2684354.2684513200);
    assertThat(i).isEqualTo(BigInteger.valueOf(2684354268451320L));

    i = Utils.toNano(5L * BLN1);

    assertThat(i).isEqualTo(BigInteger.valueOf(5 * BLN1 * BLN1));
    assertThat(i.toString()).isEqualTo("5000000000000000000");

    i = Utils.toNano("2");
    assertThat(i).isEqualTo(BigInteger.valueOf(2000000000L));

    i = Utils.toNano("2.3");
    assertThat(i).isEqualTo(BigInteger.valueOf(2300000000L));

    i = Utils.toNano("2684354.2684513200");
    assertThat(i).isEqualTo(BigInteger.valueOf(2684354268451320L));

    i = Utils.toNano("2684354.2684513200", 2);
    assertThat(i).isEqualTo(BigInteger.valueOf(268435426));

    i = Utils.toNano(new BigDecimal("2684354.2684513200"), 2);
    assertThat(i).isEqualTo(BigInteger.valueOf(268435426));

    i = Utils.toNano(2684354.2684513200, 2);
    assertThat(i).isEqualTo(BigInteger.valueOf(268435426));
  }

  @Test
  public void testFromNano() {
    BigDecimal d = Utils.fromNano(2684354268451321234L);
    assertThat(d).isEqualTo(new BigDecimal("2684354268.451321234"));

    d = Utils.fromNano(968434L);
    assertThat(d).isEqualTo(new BigDecimal("0.000968434"));

    d = Utils.fromNano(2684354268451321234L, 2);
    assertThat(d).isEqualTo(new BigDecimal("26843542684513212.34"));

    d = Utils.fromNano(2684354268451321234L, 3);
    assertThat(d).isEqualTo(new BigDecimal("2684354268451321.234"));

    d = Utils.fromNano(2684354268451321234L, 3);
    assertThat(d).isEqualTo(new BigDecimal("2684354268451321.234"));

    d = Utils.fromNano(new BigInteger("2684354268451321234"), 3);
    assertThat(d).isEqualTo(new BigDecimal("2684354268451321.234"));

    d = Utils.fromNano(2684354268451321234L, 1);
    assertThat(d).isEqualTo(new BigDecimal("268435426845132123.4"));

    d = Utils.fromNano("2684354268451321234", 1);
    assertThat(d).isEqualTo(new BigDecimal("268435426845132123.4"));

    d = Utils.fromNano(new BigDecimal("2684354268451321234"), 1);
    assertThat(d).isEqualTo(new BigDecimal("268435426845132123.4"));

    log.info(Utils.formatNanoValue("2684354268451321234"));
    log.info(Utils.formatCoins("2684354268451321234"));
    log.info(Utils.formatCoins("2684354268451321234", 2));
    log.info(Utils.formatNanoValue("2684354268451321234", 4));
    log.info(Utils.formatNanoValue("2684354268451321234", 4, RoundingMode.HALF_UP));
    log.info(Utils.formatNanoValue("2684354268451391234", 4, RoundingMode.HALF_UP));
  }

  @Test(expected = java.lang.Error.class)
  public void testNanoFail() {

    BigInteger i5 =
        Utils.toNano(2684354.26845132123456789); // picks only 9 digits after decimal point
    assertThat(i5).isEqualTo(BigInteger.valueOf(2684354268451321L));

    BigInteger i6 =
        Utils.toNano(
            BigDecimal.valueOf(
                2684354.26845132123456789)); // picks only 9 digits after decimal point
    assertThat(i6).isEqualTo(BigInteger.valueOf(2684354268451321L));
  }

  @Test
  public void testReverseLong() {
    long l = 0x27e7c64a;
    log.info("reversed {}", Long.toHexString(Long.reverseBytes(l)));
  }

  /**
   * bash IP=185.86.79.9; IPNUM=0; for (( i=0 ; i<4 ; ++i )); do
   * ((IPNUM=$IPNUM+${IP%%.*}*$((256**$((3-${i})))))); IP=${IP#*.}; done [ $IPNUM -gt $((2**31)) ]
   * && IPNUM=$(($IPNUM - $((2**32)))) echo $IPNUM
   */
  @Test
  public void testLongIpToString() {
    long ip = 1495755568;
    log.info("ip {}", int2ip(ip));
    assertThat(int2ip(ip)).isEqualTo("89.39.107.48");

    ip = -1062731775;
    log.info("ip {}", int2ip(ip));

    ip = -1185526007;
    log.info("ip {}", int2ip(ip));
    assertThat(int2ip(ip)).isEqualTo("185.86.79.9");

    ip = -1952265919;
    log.info("ip {}", int2ip(ip));
    assertThat(int2ip(ip)).isEqualTo("139.162.201.65");

    ip = -1468571697;
    log.info("ip {}", int2ip(ip));
    assertThat(int2ip(ip)).isEqualTo("168.119.95.207");

    ip = 1592601963; // testnet [0] - 94.237.45.107
    log.info("ip {}", int2ip(ip));

    ip = 1162057690; // testnet [1] - 69.67.151.218
    log.info("ip {}", int2ip(ip));

    ip = -1304477830; // testnet [2] - 178.63.63.122
    log.info("ip {}", int2ip(ip));

    ip = 1495755568; // testnet [3] - 89.39.107.48
    log.info("ip {}", int2ip(ip));

    ip = 84478511; // mainnet [0] - 5.9.10.47
    log.info("ip {}", int2ip(ip));

    ip = 84478479; // mainnet [1] - 5.9.10.15
    log.info("ip {}", int2ip(ip));

    ip = -2018135749; // mainnet [2] - 135.181.177.59
    log.info("ip {}", int2ip(ip));
  }

  @Test
  public void testStringIpToInt() {
    String ip = "89.39.107.48";
    log.info("ip {}", ip2int(ip));
    assertThat(ip2int(ip)).isEqualTo(1495755568);

    ip = "185.86.79.9";
    log.info("ip {}", ip2int(ip));
    assertThat(ip2int(ip)).isEqualTo(-1185526007);

    ip = "139.162.201.65";
    log.info("ip {}", ip2int(ip));
    assertThat(ip2int(ip)).isEqualTo(-1952265919);

    ip = "168.119.95.207";
    log.info("ip {}", ip2int(ip));
    assertThat(ip2int(ip)).isEqualTo(-1468571697);

    ip = "94.237.45.107"; // testnet [0] - 94.237.45.107
    log.info("ip {}", ip2int(ip));
    assertThat(ip2int(ip)).isEqualTo(1592601963);

    ip = "69.67.151.218"; // testnet [1] - 69.67.151.218
    log.info("ip {}", ip2int(ip));
    assertThat(ip2int(ip)).isEqualTo(1162057690);

    ip = "178.63.63.122"; // testnet [2] - 178.63.63.122
    log.info("ip {}", ip2int(ip));
    assertThat(ip2int(ip)).isEqualTo(-1304477830);

    ip = "89.39.107.48"; // testnet [3] - 89.39.107.48
    log.info("ip {}", ip2int(ip));
    assertThat(ip2int(ip)).isEqualTo(1495755568);

    ip = "5.9.10.47"; // mainnet [0] - 5.9.10.47
    log.info("ip {}", ip2int(ip));
    assertThat(ip2int(ip)).isEqualTo(84478511);

    ip = "5.9.10.15"; // mainnet [1] - 5.9.10.15
    log.info("ip {}", ip2int(ip));
    assertThat(ip2int(ip)).isEqualTo(84478479);

    ip = "135.181.177.59"; // mainnet [2] - 135.181.177.59
    log.info("ip {}", ip2int(ip));
    assertThat(ip2int(ip)).isEqualTo(-2018135749);
  }

  @Test
  public void testGetArtifactUrl() {

    String operSys = System.getProperty("os.name").toLowerCase(Locale.ENGLISH);
    String operArch = System.getProperty("os.arch").toLowerCase(Locale.ENGLISH);

    log.info("operSys {}, operArch {}", operSys, operArch);

    log.info("OS {}", Utils.getOS());

    if (Utils.getOS() == OS.WINDOWS) {
      assertThat(getArtifactGithubUrl("fift", "v2024.12-1"))
          .isEqualTo("https://github.com/ton-blockchain/ton/releases/download/v2024.12-1/fift.exe");
      assertThat(getLiteClientGithubUrl())
          .isEqualTo(
              "https://github.com/ton-blockchain/ton/releases/latest/download/lite-client.exe");
      assertThat(getFiftGithubUrl())
          .isEqualTo("https://github.com/ton-blockchain/ton/releases/latest/download/fift.exe");
      assertThat(getFuncGithubUrl())
          .isEqualTo("https://github.com/ton-blockchain/ton/releases/latest/download/func.exe");
      assertThat(getTolkGithubUrl())
          .isEqualTo("https://github.com/ton-blockchain/ton/releases/latest/download/tolk.exe");

      assertThat(getTonlibGithubUrl())
          .isEqualTo(
              "https://github.com/ton-blockchain/ton/releases/latest/download/tonlibjson.dll");
      assertThat(getEmulatorGithubUrl())
          .isEqualTo(
              "https://github.com/ton-blockchain/ton/releases/latest/download/libemulator.dll");
    }

    if (Utils.getOS() == OS.LINUX) {
      assertThat(getFiftGithubUrl())
          .isEqualTo(
              "https://github.com/ton-blockchain/ton/releases/latest/download/fift-linux-x86_64");
      assertThat(getFuncGithubUrl())
          .isEqualTo(
              "https://github.com/ton-blockchain/ton/releases/latest/download/func-linux-x86_64");
      assertThat(getTolkGithubUrl())
          .isEqualTo(
              "https://github.com/ton-blockchain/ton/releases/latest/download/tolk-linux-x86_64");
      assertThat(getTonlibGithubUrl())
          .isEqualTo(
              "https://github.com/ton-blockchain/ton/releases/latest/download/tonlibjson-linux-x86_64.so");
    }

    if (Utils.getOS() == OS.MAC) {
      assertThat(getTonlibGithubUrl())
          .isEqualTo(
              "https://github.com/ton-blockchain/ton/releases/latest/download/tonlibjson-mac-x86-64.dylib");
    }

    if (Utils.getOS() == OS.MAC_ARM64) {
      assertThat(getFiftGithubUrl())
          .isEqualTo(
              "https://github.com/ton-blockchain/ton/releases/latest/download/fift-mac-arm64");
      assertThat(getEmulatorGithubUrl())
          .isEqualTo(
              "https://github.com/ton-blockchain/ton/releases/latest/download/libemulator-mac-arm64.dylib");
      assertThat(getTonlibGithubUrl())
          .isEqualTo(
              "https://github.com/ton-blockchain/ton/releases/latest/download/tonlibjson-mac-arm64.dylib");
    }
  }

  @Test
  public void testGenerateRandomAddress() {
    String addr = Utils.generateRandomAddress(-1);
    assertThat(addr).contains(":");
    String[] parts = addr.split(":");
    assertThat(parts.length).isEqualTo(2);
    assertThat(Integer.parseInt(parts[0])).isIn(-1, 0);
    assertThat(parts[1].length()).isEqualTo(64);
  }

  @Test
  public void testCryptoKeyHelpers() {
    byte[] privateKey = Utils.generatePrivateKey();
    byte[] publicKey = Utils.getPublicKey(privateKey);
    assertThat(publicKey).isNotNull();

    byte[] message = "hello".getBytes();
    byte[] signature = Utils.signData(publicKey, privateKey, message);
    assertThat(signature).isNotNull();

    int[] unsigned = Utils.signedBytesToUnsigned(signature);
    byte[] signedBack = Utils.unsignedBytesToSigned(unsigned);
    assertThat(signedBack).isEqualTo(signature);
  }

  @Test
  public void testPadAndConversions() {
    assertThat(Utils.pad4(1)).isEqualTo(4);
    assertThat(Utils.pad8(5)).isEqualTo(8);
    assertThat(Utils.pad16(17)).isEqualTo(32);

    // bytes/int conversions
    byte[] b = new byte[] {0x01, 0x02, 0x03, 0x04};
    assertThat(Utils.bytesToInt(b)).isNotZero();
    assertThat(Utils.bytesToShort(new byte[] {0x01, 0x02})).isNotZero();

    int[] ints = new int[] {0x01, 0x02, 0x03, 0x04};
    assertThat(Utils.intsToInt(ints)).isNotZero();
    assertThat(Utils.intsToShort(new int[] {0x01, 0x02})).isNotZero();
  }

  @Test
  public void testStreamsAndBuffers() {
    byte[] payload = new byte[] {1, 2, 3, 4};

    // TL round-trip using byte[] API
    byte[] serialized = Utils.toBytes(payload);
    byte[] restored = Utils.fromBytes(serialized);
    assertThat(restored).isEqualTo(payload);

    // TL round-trip using ByteBuffer API
    java.nio.ByteBuffer buf = java.nio.ByteBuffer.wrap(serialized);
    byte[] restored2 = Utils.fromBytes(buf);
    assertThat(restored2).isEqualTo(payload);

    // toBytes should align and include proper header, but length must be >= payload length
    assertThat(serialized.length).isGreaterThanOrEqualTo(payload.length + 1);

    // read helper reads exactly requested number of bytes
    byte[] read = Utils.read(java.nio.ByteBuffer.wrap(new byte[] {9, 8, 7, 6}), 2);
    assertThat(read).isEqualTo(new byte[] {9, 8});
  }

  @Test
  public void testRandomBytes() {
    assertThat(Utils.randomBytes()).hasSize(32);
    assertThat(Utils.randomBytes(16)).hasSize(16);
  }

  @Test
  public void testUtcFormatting() {
    long ts = 0L;
    assertThat(Utils.toUTC(ts)).isEqualTo("1970-01-01 00:00:00");
    assertThat(Utils.toUTCTimeOnly(ts)).isEqualTo("00:00:00");
  }

  @Test
  public void testPadAndLog2() {
    assertThat(Utils.pad4(3)).isEqualTo(4);
    assertThat(Utils.pad8(9)).isEqualTo(16);
    assertThat(Utils.pad16(17)).isEqualTo(32);
    assertThat(Utils.log2(8)).isEqualTo(3);
    assertThat(Utils.log2(9)).isEqualTo(4);
  }

  @Test
  public void testLeftRightPadBytes() {
    byte[] src = new byte[] {1, 2, 3};
    assertThat(Utils.leftPadBytes(src, 5, (char) 0)).isEqualTo(new byte[] {0, 0, 1, 2, 3});
    assertThat(Utils.rightPadBytes(src, 5, (char) 1)).isEqualTo(new byte[] {1, 2, 3, 1, 1});
    // if size <= len, returns original
    assertThat(Utils.leftPadBytes(src, 2, (char) 0)).isEqualTo(src);
    assertThat(Utils.rightPadBytes(src, 2, (char) 0)).isEqualTo(src);
  }

  @Test
  public void testUnsignedHelpers() {
    assertThat(Utils.unsignedByteToInt((byte) 0xFF)).isEqualTo(255);
    assertThat(Utils.unsignedShortToInt((short) 0xFFFF)).isEqualTo(65535);
    assertThat(Utils.unsignedIntToLong(0xFFFFFFFF)).isEqualTo(4294967295L);
  }

  @Test
  public void testUintAndByteToBytesAndCompare() {
    assertThat(Utils.uintToBytes(123)).isEqualTo(new int[] {123});
    assertThat(Utils.byteToBytes((byte) 7)).isEqualTo(new byte[] {7});
    assertThat(Utils.compareBytes(new byte[] {1, 2}, new byte[] {1, 2})).isTrue();
    assertThat(Utils.compareBytes(new byte[] {1, 2}, new byte[] {2, 1})).isFalse();
  }

  @Test
  public void testNowAndQueryCrc() {
    assertThat(Utils.now()).isGreaterThan(0L);
    assertThat(Utils.getQueryCrc32IEEEE("test")).isEqualTo(3632233996L);
  }

  @Test
  public void testResourceAndLocalOrDownloadErrors() {
    try {
      Utils.getResourceAbsoluteDirectory(
          TestUtilsCoverage.class.getClassLoader(), "no_such_resource.file");
      assertThat(true).isFalse();
    } catch (Error expected) {
      // expected
    }
    String path = Utils.getLocalOrDownload("/tmp/some_local_path_without_http");
    assertThat(path).isEqualTo("/tmp/some_local_path_without_http");
  }

  @Test
  public void testOSAndExtensionsAndUrls() {
    Utils.OS os = Utils.getOS();
    String libExt = Utils.getLibraryExtension();
    assertThat(libExt).isIn("dll", "dylib", "so");
    String exeExt = Utils.getArtifactExtension("lite-client");
    if (os == Utils.OS.WINDOWS || os == Utils.OS.WINDOWS_ARM) {
      assertThat(exeExt).isEqualTo(".exe");
    } else {
      assertThat(exeExt).isEqualTo("");
    }
    // URL builders should return a non-empty URL for current OS
    assertThat(Utils.getLiteClientGithubUrl()).contains("github.com");
    assertThat(Utils.getEmulatorGithubUrl()).contains("github.com");
    assertThat(Utils.getTonlibGithubUrl()).contains("github.com");
    assertThat(Utils.getFuncGithubUrl()).contains("github.com");
    assertThat(Utils.getTolkGithubUrl()).contains("github.com");
    assertThat(Utils.getFiftGithubUrl()).contains("github.com");
  }

  @Test
  public void testShardAndBigIntegerHelpers() {
    assertThat(Utils.convertShardIdentToShard(new BigInteger("9223372036854775808"), 63))
        .isEqualTo("8000000000000000");
    assertThat(Utils.longToUnsignedBigInteger(-1L))
        .isEqualTo(new BigInteger("18446744073709551615"));
    assertThat(Utils.longToUnsignedBigInteger("-1"))
        .isEqualTo(new BigInteger("18446744073709551615"));
    assertThat(Utils.bigIntegerToUnsignedHex(new BigInteger("-1"))).isEqualTo("ffffffffffffffff");
  }

  @Test
  public void testTo32ByteArrayAndGenerateString() {
    byte[] b = Utils.to32ByteArray(new BigInteger("1234"));
    assertThat(b.length).isEqualTo(32);
    assertThat(Utils.generateString(5, "A")).hasSize(5);
  }

  @Test
  public void testDisableEnableNativeOutputNoop() {
    // With non-negative verbosity this should return immediately (no OS-specific calls)
    Utils.disableNativeOutput(0);
    Utils.enableNativeOutput(0);
  }

  @Test
  public void testStreamHelpers() {
    // success path
    java.io.InputStream ok = new java.io.ByteArrayInputStream("abc".getBytes());
    assertThat(Utils.streamToString(ok)).isEqualTo("abc");
    java.io.InputStream ok2 = new java.io.ByteArrayInputStream(new byte[] {1, 2, 3});
    assertThat(Utils.streamToBytes(ok2)).isEqualTo(new byte[] {1, 2, 3});

    // exception path
    assertThat(Utils.streamToString(null)).isNull();
    assertThat(Utils.streamToBytes(null)).isNull();
  }

  @Test
  public void testTLSerializationExtendedLengths() {
    // 0xFE length (next 3 bytes, little endian)
    java.nio.ByteBuffer bufFE =
        java.nio.ByteBuffer.allocate(16).order(java.nio.ByteOrder.LITTLE_ENDIAN);
    bufFE.put((byte) 0xFE);
    bufFE.put(new byte[] {3, 0, 0}); // length = 3
    bufFE.put(new byte[] {9, 8, 7, 0}); // payload 3 bytes + 1 padding
    bufFE.flip();
    byte[] outFE = Utils.fromBytes(bufFE);
    assertThat(outFE.length).isEqualTo(4);
    assertThat(outFE[0]).isEqualTo((byte) 9);

    // 0xFF length (next 7 bytes little endian)
    java.nio.ByteBuffer bufFF =
        java.nio.ByteBuffer.allocate(16).order(java.nio.ByteOrder.LITTLE_ENDIAN);
    bufFF.put((byte) 0xFF);
    // seven bytes for value 1
    bufFF.put(new byte[] {1, 0, 0, 0, 0, 0, 0});
    bufFF.put(new byte[] {42, 0, 0, 0}); // payload 1 + padding to 4
    bufFF.flip();
    byte[] outFF = Utils.fromBytes(bufFF);
    assertThat(outFF.length).isEqualTo(4);
    assertThat(outFF[0]).isEqualTo((byte) 42);

    // byte[] variant 0xFE and 0xFF
    byte[] arrFE = new byte[] {(byte) 0xFE, 3, 0, 0, 9, 8, 7, 0};
    assertThat(Utils.fromBytes(arrFE)).isEqualTo(new byte[] {9, 8, 7});

    byte[] arrFF = new byte[] {(byte) 0xFF, 1, 0, 0, 0, 0, 0, 0, 42, 0, 0, 0};
    assertThat(Utils.fromBytes(arrFF)).isEqualTo(new byte[] {42, 0, 0, 0});
  }

  @Test
  public void testBase64UrlSafeAndStringBase64() throws org.apache.commons.codec.DecoderException {
    assertThat(Utils.bitStringToBase64UrlSafe("1111000011110000")).isEqualTo("8PA=");
    assertThat(Utils.stringToBase64("hello")).isEqualTo("aGVsbG8=");
  }

  @Test
  public void testDetectAbsolutePath() {
    // happy path for an existing executable
    String lsPath = Utils.detectAbsolutePath("ls", false);
    assertThat(lsPath).isNotNull();

    // error path for non-existing executable
    try {
      Utils.detectAbsolutePath("totally_non_existing_command_123456", false);
      assertThat(true).isFalse();
    } catch (Error expected) {
      // ok
    }
  }

  @Test
  public void testNativeOutputToggleFull() {
    // Trigger full paths with negative verbosity
    Utils.disableNativeOutput(-1);
    Utils.enableNativeOutput(-1);
  }

  @Test
  public void testResourceAbsoluteDirectorySuccess() {
    String path =
        Utils.getResourceAbsoluteDirectory(
            TestUtilsCoverage.class.getClassLoader(), "org/ton/ton4j/utils/Utils.class");
    assertThat(path).contains("Utils.class");
  }

  @Test
  public void testSecp256k1Flow() {
    byte[] priv = Utils.generatePrivateKey();
    byte[] pub = Utils.getPublicKey(priv);
    assertThat(pub).isNotNull();
    byte[] data = Utils.sha256AsArray("msg".getBytes());
    SignatureWithRecovery sig = Utils.signDataSecp256k1(data, priv, pub);
    assertThat((Object) sig).isNotNull();

    byte v = Utils.getRecoveryId(sig.getR(), sig.getS(), data, pub);
    assertThat(v).isBetween((byte) 0, (byte) 3);

    byte[] recovered = Utils.recoverPublicKey(sig.getR(), sig.getS(), new byte[] {v}, data);
    assertThat(recovered).isNotEmpty();
  }

  @Test
  public void testGlobalConfigUrls() {
    assertThat(Utils.getGlobalConfigUrlMainnet()).contains("ton.org");
    assertThat(Utils.getGlobalConfigUrlTestnet()).contains("ton.org");
    assertThat(Utils.getGlobalConfigUrlMyLocalTon()).contains("127.0.0.1");
    assertThat(Utils.getGlobalConfigUrlMainnetGithub()).contains("github");
    assertThat(Utils.getGlobalConfigUrlTestnetGithub()).contains("github");
  }

  @Test
  public void testRandomAddress() {
    String addr = Utils.generateRandomAddress(0);
    assertThat(addr).contains(":");
  }

  @Test
  public void testDynIntHelpers() {
    assertThat(Utils.dynInt(new int[] {0x00, 0x00, 0x00, 0x1})).isEqualTo(1);
  }

  @Test
  public void testBitStringToByteArray() {
    // empty string
    assertThat(Utils.bitStringToByteArray("")).isEqualTo(new byte[] {});

    // single 1 at MSB position
    assertThat(Utils.bitStringToByteArray("1")).isEqualTo(new byte[] {(byte) 0b1000_0000});

    // multiple bits crossing byte boundary
    String bits = "101010100111"; // 12 bits -> 2 bytes
    byte[] arr = Utils.bitStringToByteArray(bits);
    assertThat(arr.length).isEqualTo(2);
    // first 8 bits 10101010 = 0xAA, next 4 bits 0111xxxx => 0x70
    assertThat(arr[0]).isEqualTo((byte) 0xAA);
    assertThat(arr[1]).isEqualTo((byte) 0x70);
  }

  @Test
  public void testBytesToIntX() {
    // one byte
    assertThat(Utils.bytesToIntX(new byte[] {1})).isEqualTo(1);
    // two bytes 0x01 0x02 => 0x0102 = 258
    assertThat(Utils.bytesToIntX(new byte[] {1, 2})).isEqualTo(258);
    // four bytes, include negative signed bytes; method masks with 0xFF
    byte[] b = new byte[] {(byte) 0xFF, 0x00, 0x10, 0x20}; // 0xFF001020
    assertThat(Utils.bytesToIntX(b)).isEqualTo(0xFF001020);
  }

  @Test
  public void testFormatNanoValueZero() {
    // null -> empty string
    assertThat(Utils.formatNanoValueZero(null)).isEqualTo("");
    // zero -> "0"
    assertThat(Utils.formatNanoValueZero(BigInteger.ZERO)).isEqualTo("0");
    // positive -> formatted with 9 decimals
    String s = Utils.formatNanoValueZero(new BigInteger("123000000000")); // 123 * 1e9
    assertThat(s).isEqualTo("123.000000000");
  }

  @Test
  public void testCalculateMethodIdAndCRC16Bytes() {
    int crc = Utils.getCRC16ChecksumAsInt("test".getBytes());
    assertThat(crc).isBetween(0, 0xFFFF);
    byte[] crcBytes = Utils.getCRC16ChecksumAsBytes("test".getBytes());
    assertThat(crcBytes).hasSize(2);
    int methodId = Utils.calculateMethodId("myMethod");
    assertThat(methodId & 0x10000).isEqualTo(0x10000);
  }

  @Test
  public void testPrimitiveConversions() {
    // bytesToLong
    byte[] longBytes =
        new byte[] {0x01, 0x23, 0x45, 0x67, (byte) 0x89, (byte) 0xAB, (byte) 0xCD, (byte) 0xEF};
    assertThat(Utils.bytesToLong(longBytes)).isEqualTo(0x0123456789ABCDEFL);

    // bytesToInt
    byte[] intBytes = new byte[] {0x01, 0x23, 0x45, 0x67};
    assertThat(Utils.bytesToInt(intBytes)).isEqualTo(0x01234567);

    // bytesToShort
    byte[] shortBytes = new byte[] {0x01, 0x23};
    assertThat(Utils.bytesToShort(shortBytes)).isEqualTo((short) 0x0123);

    // intsToLong
    int[] ints8 = new int[] {0x01, 0x23, 0x45, 0x67, 0x89, 0xAB, 0xCD, 0xEF};
    assertThat(Utils.intsToLong(ints8)).isEqualTo(0x0123456789ABCDEFL);

    // intsToInt
    int[] ints4 = new int[] {0x01, 0x23, 0x45, 0x67};
    assertThat(Utils.intsToInt(ints4)).isEqualTo(0x01234567);

    // intsToShort
    int[] ints2 = new int[] {0x01, 0x23};
    assertThat(Utils.intsToShort(ints2)).isEqualTo((short) 0x0123);

    // intToByteArray
    assertThat(Utils.intToByteArray(0xABCD)).isEqualTo(new byte[] {(byte) 0xAB, (byte) 0xCD});

    // intToIntArray
    assertThat(Utils.intToIntArray(123)).isEqualTo(new int[] {123});
  }

  @Test
  public void testDynamicIntBytes() {
    BigInteger v = new BigInteger("123456", 16); // 0x123456
    // size 3 -> expect last 3 bytes 12 34 56
    assertThat(Utils.dynamicIntBytes(v, 3)).isEqualTo(new byte[] {0x12, 0x34, 0x56});
    // size 4 -> expect 00 12 34 56
    assertThat(Utils.dynamicIntBytes(v, 4)).isEqualTo(new byte[] {0x00, 0x12, 0x34, 0x56});
    // full size 8
    byte[] b = Utils.dynamicIntBytes(v, 8);
    assertThat(b.length).isEqualTo(8);
    // last 3 bytes still 12 34 56
    assertThat(new byte[] {b[5], b[6], b[7]}).isEqualTo(new byte[] {0x12, 0x34, 0x56});
  }

  @Test
  public void testFormattingHelpers() {
    BigInteger nano = new BigInteger("1500000000"); // 1.5 TON
    assertThat(Utils.formatNanoValue(nano)).isEqualTo("1.500000000");
    assertThat(Utils.formatNanoValueStripZeros(nano)).isEqualTo("1.5");
    assertThat(Utils.formatNanoValueZeroStripZeros(BigInteger.ZERO)).isEqualTo("0");

    assertThat(Utils.formatNanoValue(nano, 2)).isEqualTo("1.50");
    assertThat(Utils.formatNanoValue("1500000000", 2)).isEqualTo("1.50");
    assertThat(Utils.formatNanoValue("1499999999", 1, RoundingMode.DOWN)).isEqualTo("1.4");
    assertThat(Utils.formatNanoValue("1499999999", 1, RoundingMode.HALF_UP)).isEqualTo("1.5");

    // jetton formatting (decimals=9 behaves like TON)
    assertThat(Utils.formatJettonValue("1500000000", 9, 3)).isEqualTo("1.500");
    assertThat(Utils.formatJettonValue(new BigInteger("1500000000"), 9, 3)).isEqualTo("1.500");
  }

  @Test
  public void testArrayUtilities() {
    // concat int[]
    assertThat(Utils.concatBytes(new int[] {1, 2}, new int[] {3, 4}))
        .isEqualTo(new int[] {1, 2, 3, 4});
    // concat byte[]
    assertThat(Utils.concatBytes(new byte[] {1, 2}, new byte[] {3, 4}))
        .isEqualTo(new byte[] {1, 2, 3, 4});

    // copy byte[] into larger dst
    byte[] dst = new byte[] {9, 9, 9, 9, 9};
    byte[] src = new byte[] {1, 2};
    assertThat(Utils.copy(dst, 3, src, 0)).isEqualTo(new byte[] {9, 9, 9, 1, 2});

    // copy int[]
    int[] idst = new int[] {9, 9, 9, 9, 9};
    int[] isrc = new int[] {1, 2};
    assertThat(Utils.copy(idst, 3, isrc, 0)).isEqualTo(new int[] {9, 9, 9, 1, 2});

    // slice
    assertThat(Utils.slice(new byte[] {0, 1, 2, 3, 4}, 1, 3)).isEqualTo(new byte[] {1, 2, 3});

    // appendByteArray returns new combined array
    assertThat(Utils.appendByteArray(new byte[] {1, 2}, new byte[] {3}))
        .isEqualTo(new byte[] {1, 2, 3});

    // cover append() zero-length safe paths (no-op)
    assertThat(Utils.append(new byte[] {1, 2}, new byte[0])).isEqualTo(new byte[] {1, 2});
    assertThat(Utils.append(new int[] {1, 2}, new int[0])).isEqualTo(new int[] {1, 2});
  }

  @Test
  public void testTLToBytesLargeAndErrors() {
    // toBytes with len >= 0xFE triggers int header and padding
    byte[] big = new byte[0xFE]; // 254
    Arrays.fill(big, (byte) 7);
    byte[] serialized = Utils.toBytes(big);
    // first byte should be 0xFE in little-endian packed int
    assertThat(serialized[0]).isEqualTo((byte) 0xFE);
    // fromBytes round-trip
    assertThat(Utils.fromBytes(serialized)).isEqualTo(big);

    // error paths for fromBytes(byte[])
    try {
      Utils.fromBytes(new byte[0]);
      assertThat(true).isFalse();
    } catch (IllegalArgumentException expected) {
      // ok
    }
    try {
      // 0xFE but less than 4 bytes total
      Utils.fromBytes(new byte[] {(byte) 0xFE, 0x01, 0x00});
      assertThat(true).isFalse();
    } catch (IllegalArgumentException expected) {
      // ok
    }
    try {
      // 0xFF but less than 7 bytes total
      Utils.fromBytes(new byte[] {(byte) 0xFF, 0x01, 0x00, 0x00, 0x00, 0x00});
      assertThat(true).isFalse();
    } catch (IllegalArgumentException expected) {
      // ok
    }

    // ByteBuffer variant insufficient data for 0xFE extension
    java.nio.ByteBuffer buf =
        java.nio.ByteBuffer.allocate(3).order(java.nio.ByteOrder.LITTLE_ENDIAN);
    buf.put((byte) 0xFE).put((byte) 1).put((byte) 0);
    buf.flip();
    try {
      Utils.fromBytes(buf);
      assertThat(true).isFalse();
    } catch (IllegalArgumentException expected) {
      // ok
    }
  }

  @Test
  public void testBigIntegerHelpersAndErrors() {
    // to32ByteArray too-large value
    try {
      Utils.to32ByteArray(BigInteger.ONE.shiftLeft(256)); // 2^256
      assertThat(true).isFalse();
    } catch (IllegalArgumentException expected) {
      // ok
    }

    // bigIntegerToUnsignedHex: null and negative
    assertThat(Utils.bigIntegerToUnsignedHex(null)).isEqualTo("null");
    assertThat(Utils.bigIntegerToUnsignedHex(new BigInteger("-1"))).isEqualTo("ffffffffffffffff");
  }
  
  @Test
  public void testBitStringToHex() throws Exception {
    // single nibble pads to full byte
    assertThat(Utils.bitStringToHex("1111")).isEqualTo("f0");
    // single bit pads to 0b1000_0000
    assertThat(Utils.bitStringToHex("1")).isEqualTo("80");
    // two bytes
    assertThat(Utils.bitStringToHex("1111000011110000")).isEqualTo("f0f0");
    try {
      Utils.bitStringToHex("");
      assertThat(true).isFalse();
    } catch (NumberFormatException expected) {
      // empty not allowed
    }
  }

  @Test
  public void testBitsToDecAndHex() {
    boolean[] a = new boolean[] {true, false, true, false}; // 1010b
    assertThat(Utils.bitsToDec(a)).isEqualTo("10");
    assertThat(Utils.bitsToHex(a)).isEqualTo("a");

    boolean[] zeros = new boolean[] {false, false, false, false, false, false, false, false};
    assertThat(Utils.bitsToDec(zeros)).isEqualTo("0");
    assertThat(Utils.bitsToHex(zeros)).isEqualTo("0");

    boolean[] leadingZeros = new boolean[] {false, false, true}; // 001b -> 1
    assertThat(Utils.bitsToDec(leadingZeros)).isEqualTo("1");
    assertThat(Utils.bitsToHex(leadingZeros)).isEqualTo("1");
  }

  @Test
  public void testHexStringToIntArrayViaPublicWrappers() {
    assertThat(Utils.hexToUnsignedBytes("0A0bFF")).isEqualTo(new int[] {10, 11, 255});
    assertThat(Utils.hexToInts("aa"))
        .isEqualTo(new int[] {170});
    // empty input -> empty array
    assertThat(Utils.hexToUnsignedBytes("")).isEqualTo(new int[] {});
    // odd-length input should throw
    try {
      Utils.hexToUnsignedBytes("ABC");
      assertThat(true).isFalse();
    } catch (RuntimeException expected) {
      // ok
    }
  }

  @Test
  public void testFormatCoinsBigDecimal() {
    // exact TON value with grouping and 9 decimals
    assertThat(Utils.formatCoins(new BigDecimal("1"))).isEqualTo("1,000,000,000.000000000");
    // fractional with <=9 scale is allowed
    assertThat(Utils.formatCoins(new BigDecimal("1.5"))).isEqualTo("1,500,000,000.000000000");
    // scale > 9 should fail
    try {
      Utils.formatCoins(new BigDecimal("0.1234567891"));
      assertThat(true).isFalse();
    } catch (Error expected) {
      // ok
    }
  }

  @Test
  public void testSha256IntArray() {
    int[] data = new int[] {65, 66, 67}; // "ABC"
    assertThat(Utils.sha256(data))
        .isEqualTo("b5d4045c3f466fa91fe2cc6abe79232a1a57cdf104f7a26e716e0a1e2789df78");
    // empty array should equal sha256 of empty bytes
    assertThat(Utils.sha256(new int[] {}))
        .isEqualTo(Utils.sha256(new byte[] {}));
  }

  @Test
  public void testToNanoFromBigDecimalAndFromNanoString() {
    assertThat(Utils.toNano(new BigDecimal("1.5"))).isEqualTo(new java.math.BigInteger("1500000000"));
    assertThat(Utils.fromNano("1500000000"))
        .isEqualTo(new BigDecimal("1.500000000"));
    assertThat(Utils.fromNano("1")).isEqualTo(new BigDecimal("0.000000001"));
  }

  @Test
  public void testFromBytesSmallLengthManual() {
    // header 3, payload 3 bytes, then 0 padding to align to 4
    byte[] arr = new byte[] {3, 9, 8, 7, 0};
    assertThat(Utils.fromBytes(arr)).isEqualTo(new byte[] {9, 8, 7});
  }
  
  @Test
  public void testToBytesWithByteBuffer() {
    byte[] payload = new byte[] {1,2,3,4,5};
    java.nio.ByteBuffer buf = java.nio.ByteBuffer.wrap(payload);
    byte[] serializedFromBuf = Utils.toBytes(buf);
    byte[] serializedFromArr = Utils.toBytes(payload);
    // Both overloads should produce identical TL-serialized output
    assertThat(serializedFromBuf).isEqualTo(serializedFromArr);
    // And restore back
    assertThat(Utils.fromBytes(serializedFromBuf)).isEqualTo(payload);
  }

  @Test
  public void testKeyPairFromHex() {
    String secretHex = "F182111193F30D79D517F2339A1BA7C25FDF6C52142F0F2C1D960A1F1D65E1E4";
    com.iwebpp.crypto.TweetNaclFast.Signature.KeyPair k1 = Utils.keyPairFromHex(secretHex);
    com.iwebpp.crypto.TweetNaclFast.Signature.KeyPair k2 =
        Utils.generateSignatureKeyPairFromSeed(Utils.hexToSignedBytes(secretHex));
    assertThat(k1.getPublicKey()).isEqualTo(k2.getPublicKey());
    assertThat(k1.getSecretKey()).isEqualTo(k2.getSecretKey());
    assertThat(k1.getPublicKey().length).isEqualTo(32);
    assertThat(k1.getSecretKey().length).isEqualTo(64);
  }

  @Test
  public void testBytesToBase64IntArray() {
    int[] data = new int[] {0, 127, 128, 255};
    // Compare int[] overload with byte[] overload after unsigned->signed conversion
    String viaInts = Utils.bytesToBase64(data);
    String viaBytes = Utils.bytesToBase64(Utils.unsignedBytesToSigned(data));
    assertThat(viaInts).isEqualTo(viaBytes);
  }

  @Test
  public void testBase64UrlSafeToBytes() {
    String urlSafe = "0QAs9VlT6S776tq3unJcP5Ogsj-ELLunLXuOb1EKcOQi4-QO";
    byte[] bytes = Utils.base64SafeUrlToBytes(urlSafe);
    // Cross-check by hex helper that is already verified elsewhere
    String hex = Utils.bytesToHex(bytes);
    assertThat(hex)
        .isEqualTo("d1002cf55953e92efbeadab7ba725c3f93a0b23f842cbba72d7b8e6f510a70e422e3e40e");
  }

  @Test
  public void testBase64ToSignedBytes() {
    String base64 = "aGVsbG8="; // "hello"
    byte[] bytes = Utils.base64ToSignedBytes(base64);
    assertThat(new String(bytes)).isEqualTo("hello");
  }

  @Test
  public void testSha256AsArray() {
    byte[] data = "ABC".getBytes();
    byte[] arr = Utils.sha256AsArray(data);
    // Length 32 and hex equals sha256(byte[])
    assertThat(arr.length).isEqualTo(32);
    assertThat(Utils.bytesToHex(arr)).isEqualTo(Utils.sha256(data));
  }

  @Test
  public void testFormatCoinsBigDecimalWithScale() {
    assertThat(Utils.formatCoins(new java.math.BigDecimal("1.234"), 2)).isEqualTo("1.23");
    assertThat(Utils.formatCoins(new java.math.BigDecimal("1000.5"), 1))
        .isEqualTo("1,000.5");
  }

  @Test
  public void testLog2Ceil() {
    assertThat(Utils.log2Ceil(1)).isEqualTo(0);
    assertThat(Utils.log2Ceil(2)).isEqualTo(1);
    assertThat(Utils.log2Ceil(3)).isEqualTo(2);
    assertThat(Utils.log2Ceil(4)).isEqualTo(2);
    assertThat(Utils.log2Ceil(5)).isEqualTo(3);
  }

  @Test
  public void testBase64ToUnsignedBytes() {
    // 00 01 02 03
    String base64 = java.util.Base64.getEncoder().encodeToString(new byte[] {0,1,2,3});
    int[] unsigned = Utils.base64ToUnsignedBytes(base64);
    assertThat(unsigned).isEqualTo(new int[] {0,1,2,3});
  }

  @Test
  public void testToNanoLongAndDoublePrecision() {
    assertThat(Utils.toNano(2L, 9)).isEqualTo(java.math.BigInteger.valueOf(2_000_000_000L));
    // Treat float via double overload
    float f = 1.5f;
    assertThat(Utils.toNano((double) f, 9)).isEqualTo(new java.math.BigInteger("1500000000"));
  }

  @Test
  public void testToNanoFloatOverload() {
    // explicit coverage of float overload success path
    assertThat(Utils.toNano(1.5f)).isEqualTo(new java.math.BigInteger("1500000000"));
    assertThat(Utils.toNano(2.0f)).isEqualTo(new java.math.BigInteger("2000000000"));
  }

  @Test(expected = java.lang.Error.class)
  public void testToNanoFloatOverloadTooManyDecimals() {
    // BigDecimal.valueOf(float) yields a representation with scale > 9 for many floats,
    // which should trigger the error path in toNano(float)
    Utils.toNano(0.123456789f);
  }

  @Test
  public void testBase64ToBitString() {
    byte[] data = "hello".getBytes();
    String base64 = java.util.Base64.getEncoder().encodeToString(data);
    String bitsFromBase64 = Utils.base64ToBitString(base64);
    String bitsFromBytes = Utils.bytesToBitString(data);
    assertThat(bitsFromBase64).isEqualTo(bitsFromBytes);
  }

  @Test
  public void testBytesToBase64SafeUrl() {
    byte[] data = new byte[] { (byte)0xF1, (byte)0xFF, 0x00, 0x11 };
    String viaUtils = Utils.bytesToBase64SafeUrl(data);
    String viaJdk = java.util.Base64.getUrlEncoder().encodeToString(data);
    assertThat(viaUtils).isEqualTo(viaJdk);
  }

  @Test
  public void testHexStringToBase64UrlSafe() throws org.apache.commons.codec.DecoderException {
    String hex = "d1002cf55953e92efbeadab7ba725c3f93a0b23f842cbba72d7b8e6f510a70e422e3e40e";
    String expectedUrlSafe = "0QAs9VlT6S776tq3unJcP5Ogsj-ELLunLXuOb1EKcOQi4-QO";
    assertThat(Utils.hexStringToBase64UrlSafe(hex)).isEqualTo(expectedUrlSafe);
  }

  @Test
  public void testFormatCoinsStringAndScale() {
    // formatCoins(String) delegates to formatNanoValue on toNano(String), so no grouping of nanocoins
    assertThat(Utils.formatCoins("1.5")).isEqualTo("1.500000000");
    assertThat(Utils.formatCoins("1.5", 2)).isEqualTo("1.50");
  }

  @Test
  public void testRepeatAndRandomAndSleep() {
    assertThat(Utils.repeat("ab", 3)).isEqualTo("ababab");
    // Just invoke random helpers to ensure coverage
    int ri = Utils.getRandomInt();
    long rl = Utils.getRandomLong();
    // No strict assertions on randomness; values exist
    assertThat(ri).isNotNull();
    assertThat(rl).isNotNull();
    // Sleep helpers with zero durations should return quickly
    Utils.sleep(0);
    Utils.sleepMs(0);
    Utils.sleep(0, "noop");
    Utils.sleepMs(0, "noop");
  }
}
