package org.ton.java.utils;

import static java.util.Objects.isNull;

import com.iwebpp.crypto.TweetNaclFast;
import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.platform.win32.Kernel32;
import com.sun.jna.platform.win32.WinNT;
import java.io.*;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Base64;
import java.util.Locale;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;

@Slf4j
public class Utils {
  private static final String HEXES = "0123456789ABCDEF";
  private static final long BLN1 = 1000000000L;
  private static final BigInteger BI_BLN1 = BigInteger.valueOf(BLN1);
  private static final BigDecimal BD_BLN1 = BigDecimal.valueOf(BLN1);

  public enum OS {
    WINDOWS,
    WINDOWS_ARM,
    LINUX,
    LINUX_ARM,
    MAC,
    MAC_ARM64,
    UNKNOWN
  }

  /** uses POLY 0x1EDC6F41 */
  public static Long getCRC32ChecksumAsLong(byte[] bytes) {
    CRC32C crc32c = new CRC32C();
    crc32c.update(bytes, 0, bytes.length);
    return crc32c.getValue() & 0x00000000ffffffffL;
  }

  public static String getCRC32ChecksumAsHex(byte[] bytes) {
    return BigInteger.valueOf(getCRC32ChecksumAsLong(bytes)).toString(16);
  }

  public static byte[] getCRC32ChecksumAsBytes(byte[] bytes) {
    return long4BytesToBytes(getCRC32ChecksumAsLong(bytes));
  }

  public static byte[] getCRC32ChecksumAsBytesReversed(byte[] bytes) {
    byte[] b = long4BytesToBytes(getCRC32ChecksumAsLong(bytes));

    byte[] reversed = new byte[4];
    reversed[0] = b[3];
    reversed[1] = b[2];
    reversed[2] = b[1];
    reversed[3] = b[0];

    return reversed;
  }

  /**
   * Long to signed bytes
   *
   * @param l value
   * @return array of unsigned bytes
   */
  public static byte[] long4BytesToBytes(long l) {
    byte[] result = new byte[4];
    for (int i = 3; i >= 0; i--) {
      result[i] = (byte) (l & 0xFF);
      l >>= 8;
    }
    return result;
  }

  public static int[] longToBytes(long l) {
    int[] result = new int[8];
    for (int i = 7; i >= 0; i--) {
      result[i] = (int) l & 0xFF;
      l >>= 8;
    }
    return result;
  }

  public static long bytesToLong(final byte[] b) {
    long result = 0;
    for (int i = 0; i < 8; i++) {
      result <<= 8;
      result |= (b[i] & 0xFF);
    }
    return result;
  }

  public static int bytesToInt(final byte[] b) {
    int result = 0;
    for (int i = 0; i < 4; i++) {
      result <<= 8;
      result |= (b[i] & 0xFF);
    }
    return result;
  }

  public static int bytesToIntX(final byte[] b) {
    int result = 0;
    for (byte value : b) {
      result <<= 8;
      result |= value & 0XFF;
    }
    return result;
  }

  public static short bytesToShort(final byte[] b) {
    short result = 0;
    for (int i = 0; i < 2; i++) {
      result <<= 8;
      result |= (short) (b[i] & 0xFF);
    }
    return result;
  }

  public static long intsToLong(final int[] b) {
    long result = 0;
    for (int i = 0; i < 8; i++) {
      result <<= 8;
      result |= b[i];
    }
    return result;
  }

  public static int intsToInt(final int[] b) {
    int result = 0;
    for (int i = 0; i < 4; i++) {
      result <<= 8;
      result |= b[i];
    }
    return result;
  }

  public static short intsToShort(final int[] b) {
    short result = 0;
    for (int i = 0; i < 2; i++) {
      result <<= 8;
      result |= (short) b[i];
    }
    return result;
  }

  public static int[] intToIntArray(int l) {
    return new int[] {l};
  }

  public static byte[] intToByteArray(int value) {
    return new byte[] {(byte) (value >>> 8), (byte) value};
  }

  // CRC-16/XMODEM
  public static int getCRC16ChecksumAsInt(byte[] bytes) {
    int crc = 0x0000;
    int polynomial = 0x1021;

    for (byte b : bytes) {
      for (int i = 0; i < 8; i++) {
        boolean bit = ((b >> (7 - i) & 1) == 1);
        boolean c15 = ((crc >> 15 & 1) == 1);
        crc <<= 1;
        if (c15 ^ bit) crc ^= polynomial;
      }
    }

    crc &= 0xffff;
    return crc;
  }

  public static int calculateMethodId(String methodName) {
    int l = Utils.getCRC16ChecksumAsInt(methodName.getBytes());
    l = (l & 0xffff) | 0x10000;
    return l;
  }

  public static String getCRC16ChecksumAsHex(byte[] bytes) {
    return bytesToHex(getCRC16ChecksumAsBytes(bytes));
  }

  public static byte[] getCRC16ChecksumAsBytes(byte[] bytes) {
    return intToByteArray(getCRC16ChecksumAsInt(bytes));
  }

  public static String sha256(final String base) {
    try {
      final MessageDigest digest = MessageDigest.getInstance("SHA-256");
      final byte[] hash = digest.digest(base.getBytes(StandardCharsets.UTF_8));
      final StringBuilder hexString = new StringBuilder();
      for (byte b : hash) {
        final String hex = Integer.toHexString(0xff & b);
        if (hex.length() == 1) hexString.append('0');
        hexString.append(hex);
      }
      return hexString.toString();
    } catch (Exception ex) {
      throw new RuntimeException(ex);
    }
  }

  public static String sha256(int[] bytes) {
    byte[] converted = new byte[bytes.length];
    for (int i = 0; i < bytes.length; i++) {
      converted[i] = (byte) (bytes[i] & 0xff);
    }
    return sha256(converted);
  }

  public static byte[] unsignedBytesToSigned(int[] bytes) {
    byte[] converted = new byte[bytes.length];
    for (int i = 0; i < bytes.length; i++) {
      converted[i] = (byte) (bytes[i] & 0xff);
    }
    return converted;
  }

  public static int[] signedBytesToUnsigned(byte[] bytes) {
    int[] converted = new int[bytes.length];
    for (int i = 0; i < bytes.length; i++) {
      converted[i] = Byte.toUnsignedInt(bytes[i]);
    }
    return converted;
  }

  public static byte[] sha256AsArray(byte[] bytes) {
    try {
      final MessageDigest digest = MessageDigest.getInstance("SHA-256");
      return digest.digest(bytes);
    } catch (NoSuchAlgorithmException ex) {
      throw new RuntimeException(ex);
    }
  }

  public static byte[] sha1AsArray(byte[] bytes) {
    try {
      final MessageDigest digest = MessageDigest.getInstance("SHA-1");
      return digest.digest(bytes);
    } catch (NoSuchAlgorithmException ex) {
      throw new RuntimeException(ex);
    }
  }

  public static byte[] md5AsArray(byte[] bytes) {
    try {
      final MessageDigest digest = MessageDigest.getInstance("MD5");
      return digest.digest(bytes);
    } catch (NoSuchAlgorithmException ex) {
      throw new RuntimeException(ex);
    }
  }

  public static String md5(byte[] bytes) {
    try {
      final MessageDigest digest = MessageDigest.getInstance("MD5");
      final byte[] hash = digest.digest(bytes);
      return Utils.bytesToHex(hash);
    } catch (NoSuchAlgorithmException ex) {
      throw new RuntimeException(ex);
    }
  }

  public static String sha256(byte[] bytes) {
    try {
      final MessageDigest digest = MessageDigest.getInstance("SHA-256");
      final byte[] hash = digest.digest(bytes);
      return Utils.bytesToHex(hash);
    } catch (NoSuchAlgorithmException ex) {
      throw new RuntimeException(ex);
    }
  }

  public static String sha1(byte[] bytes) {
    try {
      final MessageDigest digest = MessageDigest.getInstance("SHA-1");
      final byte[] hash = digest.digest(bytes);
      return Utils.bytesToHex(hash);
    } catch (NoSuchAlgorithmException ex) {
      throw new RuntimeException(ex);
    }
  }

  public static String bitsToDec(boolean[] bits) {
    StringBuilder s = new StringBuilder();
    for (boolean b : bits) {
      s.append(b ? '1' : '0');
    }
    return new BigInteger(s.toString(), 2).toString(10);
  }

  public static String bitsToHex(boolean[] bits) {
    StringBuilder s = new StringBuilder();
    for (boolean b : bits) {
      s.append(b ? '1' : '0');
    }
    return new BigInteger(s.toString(), 2).toString(16);
  }

  public static String bytesToBitString(byte[] raw) {
    String hex = Utils.bytesToHex(signedBytesToUnsigned(raw));
    BigInteger bi = new BigInteger(hex, 16);
    return bi.toString(2);
  }

  public static String bytesToBitString(int[] raw) {
    String hex = Utils.bytesToHex(raw);
    BigInteger bi = new BigInteger(hex, 16);
    return bi.toString(2);
  }

  public static String bytesToHex(byte[] raw) {
    final StringBuilder hex = new StringBuilder(2 * raw.length);
    for (final byte b : raw) {
      hex.append(HEXES.charAt((b & 0xF0) >> 4)).append(HEXES.charAt((b & 0x0F)));
    }
    return hex.toString().toLowerCase();
  }

  public static String bytesToHex(int[] raw) {
    final StringBuilder hex = new StringBuilder(2 * raw.length);
    for (final int b : raw) {
      hex.append(HEXES.charAt((b & 0xF0) >> 4)).append(HEXES.charAt((b & 0x0F)));
    }
    return hex.toString().toLowerCase();
  }

  public static String base64UrlSafeToHexString(String base64) {
    byte[] decoded = Base64.getUrlDecoder().decode(base64);
    return bytesToHex(decoded);
  }

  public static String base64ToHexString(String base64) {
    byte[] decoded = Base64.getDecoder().decode(base64);
    return bytesToHex(decoded);
  }

  public static String hexStringToBase64UrlSafe(String hex) throws DecoderException {
    byte[] decodedHex = Hex.decodeHex(hex);
    return new String(Base64.getUrlEncoder().encode(decodedHex));
  }

  public static String hexStringToBase64(String hex) throws DecoderException {
    byte[] decodedHex = Hex.decodeHex(hex);
    return new String(Base64.getEncoder().encode(decodedHex));
  }

  public static String base64ToBitString(String base64) {
    byte[] decode = Base64.getDecoder().decode(base64);
    return new BigInteger(1, decode).toString(2);
  }

  public static String bytesToBase64(byte[] bytes) {
    return Base64.getEncoder().encodeToString(bytes);
  }

  public static String bytesToBase64(int[] bytes) {
    return Base64.getEncoder().encodeToString(Utils.unsignedBytesToSigned(bytes));
  }

  public static String bytesToBase64SafeUrl(byte[] bytes) {
    return Base64.getUrlEncoder().encodeToString(bytes);
  }

  public static byte[] base64ToBytes(String base64) {
    return Base64.getDecoder().decode(base64.getBytes(StandardCharsets.UTF_8));
  }

  public static int[] base64ToUnsignedBytes(String base64) {
    return Utils.signedBytesToUnsigned(
        Base64.getDecoder().decode(base64.getBytes(StandardCharsets.UTF_8)));
  }

  public static byte[] base64ToSignedBytes(String base64) {
    return Base64.getDecoder().decode(base64.getBytes(StandardCharsets.UTF_8));
  }

  public static byte[] base64SafeUrlToBytes(String base64) {
    return Base64.getUrlDecoder().decode(base64.getBytes(StandardCharsets.UTF_8));
  }

  public static String base64ToString(String base64) {
    return new String(Base64.getDecoder().decode(base64.getBytes(StandardCharsets.UTF_8)));
  }

  public static String stringToBase64(String str) {
    return Base64.getEncoder().encodeToString(str.getBytes(StandardCharsets.UTF_8));
  }

  public static String bitStringToHex(String binary) {
    int toPad = (binary.length() % 8) == 0 ? 0 : 8 - (binary.length() % 8);
    final StringBuilder bits = new StringBuilder(binary);
    if (toPad != 0) {
      for (int i = 0; i < toPad; i++) {
        bits.append('0');
      }
    }
    return new BigInteger(bits.toString(), 2).toString(16);
  }

  public static String bitStringToBase64(String binary) throws DecoderException {
    int toPad = (binary.length() % 8) == 0 ? 0 : 8 - (binary.length() % 8);
    final StringBuilder bits = new StringBuilder(binary);
    if (toPad != 0) {
      for (int i = 0; i < toPad; i++) {
        bits.append('0');
      }
    }
    String hex = new BigInteger(bits.toString(), 2).toString(16);
    byte[] decodedHex = Hex.decodeHex(hex);
    return new String(Base64.getEncoder().encode(decodedHex));
  }

  public static String repeat(String str, int count) {
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < count; i++) {
      sb.append(str);
    }
    return sb.toString();
  }

  public static String bitStringToBase64UrlSafe(String binary) throws DecoderException {
    int toPad = (binary.length() % 8) == 0 ? 0 : 8 - (binary.length() % 8);
    final StringBuilder bits = new StringBuilder(binary);
    if (toPad != 0) {
      for (int i = 0; i < toPad; i++) {
        bits.append('0');
      }
    }
    String hex = new BigInteger(bits.toString(), 2).toString(16);
    byte[] decodedHex = Hex.decodeHex(hex);
    return new String(Base64.getUrlEncoder().encode(decodedHex));
  }

  public static int[] bitStringToIntArray(String bitString) {
    if (bitString.isEmpty()) {
      return new int[0];
    }
    int sz = bitString.length();
    int[] result = new int[(sz + 7) / 8];

    for (int i = 0; i < sz; i++) {
      if (bitString.charAt(i) == '1') {
        result[(i / 8)] |= 1 << (7 - (i % 8));
      } else {
        result[(i / 8)] &= ~(1 << (7 - (i % 8)));
      }
    }

    return result;
  }

  public static byte[] bitStringToByteArray(String bitString) {
    if (bitString.isEmpty()) {
      return new byte[0];
    }
    int sz = bitString.length();
    byte[] result = new byte[(sz + 7) / 8];

    for (int i = 0; i < sz; i++) {
      if (bitString.charAt(i) == '1') {
        result[(i / 8)] |= (byte) (1 << (7 - (i % 8)));
      } else {
        result[(i / 8)] &= (byte) ~(1 << (7 - (i % 8)));
      }
    }

    return result;
  }

  public static byte[] concatBytes(byte[] a, byte[] b) {
    byte[] c = new byte[a.length + b.length];
    System.arraycopy(a, 0, c, 0, a.length);
    System.arraycopy(b, 0, c, a.length, b.length);
    return c;
  }

  public static int[] concatBytes(int[] a, int[] b) {
    int[] c = new int[a.length + b.length];
    System.arraycopy(a, 0, c, 0, a.length);
    System.arraycopy(b, 0, c, a.length, b.length);
    return c;
  }

  public static int[] append(int[] dst, int[] with) {
    System.arraycopy(with, 0, dst, dst.length, with.length);
    return dst;
  }

  public static byte[] append(byte[] dst, byte[] with) {
    System.arraycopy(with, 0, dst, dst.length, with.length);
    return dst;
  }

  public static byte[] appendByteArray(byte[] originalArray, byte[] appendArray) {
    // Create a new array with size equal to the sum of both input arrays
    byte[] resultArray = Arrays.copyOf(originalArray, originalArray.length + appendArray.length);

    // Copy the appendArray into the new array starting from the end of originalArray
    System.arraycopy(appendArray, 0, resultArray, originalArray.length, appendArray.length);

    return resultArray;
  }

  public static int[] copy(int[] dst, int destPos, int[] src, int srcPos) {
    System.arraycopy(src, srcPos, dst, destPos, src.length);
    return dst;
  }

  public static byte[] copy(byte[] dst, int destPos, byte[] src, int srcPos) {
    System.arraycopy(src, srcPos, dst, destPos, src.length);
    return dst;
  }

  public static byte[] slice(byte[] src, int from, int size) {
    byte[] resultArray = new byte[size];
    System.arraycopy(src, from, resultArray, 0, size);
    return resultArray;
  }

  public static int dynInt(int[] data) {
    int[] tmp = new int[8];
    Utils.copy(tmp, 8 - data.length, data, 0);

    return Integer.valueOf(Utils.bytesToHex(tmp), 16);
  }

  public static int dynInt(byte[] data) {
    byte[] tmp = new byte[8];
    Utils.copy(tmp, 8 - data.length, data, 0);

    return Integer.valueOf(Utils.bytesToHex(tmp), 16);
  }

  public static byte[] dynamicIntBytes(BigInteger val, int sz) {
    byte[] tmp = new byte[8];
    byte[] valArray = val.toByteArray(); // test just return val.toByteArray()
    for (int i = 8 - valArray.length, j = 0; i < 8; i++, j++) {
      tmp[i] = valArray[j];
    }
    byte[] result = new byte[sz];
    System.arraycopy(tmp, 8 - sz, result, 0, sz);
    return result;
  }

  public static int log2Ceil(int val) {
    return Integer.SIZE - Integer.numberOfLeadingZeros(val - 1);
  }

  public static byte[] hexToSignedBytes(String hex) {
    return hexStringToByteArray(hex);
  }

  public static int[] hexToUnsignedBytes(String hex) {
    return hexStringToIntArray(hex);
  }

  public static int[] hexToInts(String hex) {
    return hexStringToIntArray(hex);
  }

  private static byte[] hexStringToByteArray(String s) {
    int len = s.length();
    byte[] data = new byte[len / 2];
    for (int i = 0; i < len; i += 2) {
      data[i / 2] =
          (byte) ((Character.digit(s.charAt(i), 16) << 4) + Character.digit(s.charAt(i + 1), 16));
    }
    return data;
  }

  private static int[] hexStringToIntArray(String s) {
    int[] result = new int[s.length() / 2];
    for (int i = 0; i < s.length(); i += 2) {
      result[i / 2] = Integer.parseInt(s.substring(i, i + 2), 16);
    }
    return result;
  }

  /**
   * Signature algorithm, Implements ed25519.
   *
   * @return TweetNaclFast.Signature.KeyPair, where keyPair.getPublicKey() - 32 bytes and
   *     keyPair.getPrivateKey - 64 bytes
   */
  public static TweetNaclFast.Signature.KeyPair generateSignatureKeyPair() {
    return TweetNaclFast.Signature.keyPair();
  }

  /**
   * Box algorithm, Public-key authenticated encryption
   *
   * @return TweetNaclFast.Box.KeyPair, where keyPair.getPublicKey() and keyPair.getPrivateKey.
   */
  public static TweetNaclFast.Box.KeyPair generateKeyPair() {
    return TweetNaclFast.Box.keyPair();
  }

  /**
   * @param secretKey 32 bytes secret key
   * @return TweetNaclFast.Signature.KeyPair, where keyPair.getPublicKey() - 32 bytes and
   *     keyPair.getPrivateKey - 64 bytes
   */
  public static TweetNaclFast.Signature.KeyPair generateSignatureKeyPairFromSeed(byte[] secretKey) {
    return TweetNaclFast.Signature.keyPair_fromSeed(secretKey);
  }

  /**
   * @param secretKey 32 bytes secret key
   * @return TweetNaclFast.Box.KeyPair, where keyPair.getPublicKey() - 32 bytes and
   *     keyPair.getPrivateKey - 32 bytes
   */
  public static TweetNaclFast.Box.KeyPair generateKeyPairFromSecretKey(byte[] secretKey) {
    return TweetNaclFast.Box.keyPair_fromSecretKey(secretKey);
  }

  /**
   * If 32 bytes secret key is provided, then signature is generated out of it and its secret key is
   * used.
   *
   * @param prvKey 32 or 64 bytes secret key.
   */
  public static TweetNaclFast.Signature getSignature(byte[] pubKey, byte[] prvKey) {
    TweetNaclFast.Signature signature;
    if (prvKey.length == 64) {
      signature = new TweetNaclFast.Signature(pubKey, prvKey);
    } else {
      TweetNaclFast.Signature.KeyPair keyPair = generateSignatureKeyPairFromSeed(prvKey);
      signature = new TweetNaclFast.Signature(pubKey, keyPair.getSecretKey());
    }
    return signature;
  }

  /**
   * Signs data
   *
   * @param pubKey 32 bytes pubKey
   * @param prvKey 32 or 64 bytes prvKey
   * @param data data to sign
   * @return byte[] signature
   */
  public static byte[] signData(byte[] pubKey, byte[] prvKey, byte[] data) {
    TweetNaclFast.Signature signature;
    if (prvKey.length == 64) {
      signature = new TweetNaclFast.Signature(pubKey, prvKey);
    } else {
      TweetNaclFast.Signature.KeyPair keyPair = generateSignatureKeyPairFromSeed(prvKey);
      signature = new TweetNaclFast.Signature(pubKey, keyPair.getSecretKey());
    }
    return signature.detached(data);
  }

  public static String toUTC(long timestamp) {
    return DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        .format(LocalDateTime.ofEpochSecond(timestamp, 0, ZoneOffset.UTC));
  }

  public static String toUTCTimeOnly(long timestamp) {
    return DateTimeFormatter.ofPattern("HH:mm:ss")
        .format(LocalDateTime.ofEpochSecond(timestamp, 0, ZoneOffset.UTC));
  }

  public static OS getOS() {

    String operSys = System.getProperty("os.name").toLowerCase(Locale.ENGLISH);
    String operArch = System.getProperty("os.arch").toLowerCase(Locale.ENGLISH);

    if (operSys.contains("win")) {
      if ((operArch.contains("arm")) || (operArch.contains("aarch"))) {
        return OS.WINDOWS_ARM;
      } else {
        return OS.WINDOWS;
      }
    } else if (operSys.contains("nix") || operSys.contains("nux") || operSys.contains("aix")) {
      if ((operArch.contains("arm")) || (operArch.contains("aarch"))) {
        return OS.LINUX_ARM;
      } else {
        return OS.LINUX;
      }
    } else if (operSys.contains("mac")) {
      if (operArch.contains("arm")
          || operArch.contains("aarch")
          || operArch.contains("m1")
          || operArch.contains("m2")
          || operArch.contains("m3")
          || operArch.contains("m4")) {
        return OS.MAC_ARM64;
      } else {
        return OS.MAC;
      }
    } else {
      return OS.UNKNOWN;
    }
  }

  public static String streamToString(InputStream is) {
    try {
      BufferedReader br = new BufferedReader(new InputStreamReader(is));
      return br.lines().collect(Collectors.joining());
    } catch (Exception e) {
      return null;
    }
  }

  public static BigInteger toNano(double toncoins, Integer precision) {
    return BigDecimal.valueOf(toncoins).multiply(BigDecimal.TEN.pow(precision)).toBigInteger();
  }

  public static BigInteger toNano(BigDecimal toncoins, Integer precision) {
    return toncoins.multiply(BigDecimal.TEN.pow(precision)).toBigInteger();
  }

  public static BigInteger toNano(String toncoins, Integer precision) {
    return new BigDecimal(toncoins).multiply(BigDecimal.TEN.pow(precision)).toBigInteger();
  }

  public static BigInteger toNano(long toncoins, Integer precision) {
    return new BigDecimal(toncoins).multiply(BigDecimal.TEN.pow(precision)).toBigInteger();
  }

  public static BigDecimal fromNano(BigInteger nanoCoins, Integer precision) {
    return new BigDecimal(nanoCoins)
        .divide(BigDecimal.TEN.pow(precision), precision, RoundingMode.FLOOR);
  }

  public static BigDecimal fromNano(BigDecimal nanoCoins, Integer precision) {
    return nanoCoins.divide(BigDecimal.TEN.pow(precision), precision, RoundingMode.FLOOR);
  }

  public static BigDecimal fromNano(String nanoCoins, Integer precision) {
    return new BigDecimal(nanoCoins)
        .divide(BigDecimal.TEN.pow(precision), precision, RoundingMode.FLOOR);
  }

  public static BigDecimal fromNano(long nanoCoins, Integer precision) {
    return new BigDecimal(nanoCoins)
        .divide(BigDecimal.TEN.pow(precision), precision, RoundingMode.FLOOR);
  }

  public static BigInteger toNano(long toncoins) {
    checkToncoinsOverflow(BigInteger.valueOf(toncoins).multiply(BI_BLN1));
    return BigInteger.valueOf(toncoins).multiply(BI_BLN1);
  }

  public static BigInteger toNano(String toncoins) {
    checkToncoinsOverflow(new BigDecimal(toncoins).multiply(BD_BLN1).toBigInteger());

    if (toncoins.matches("^\\d*\\.\\d+|\\d+\\.\\d*$")) {
      return new BigDecimal(toncoins).multiply(BigDecimal.valueOf(BLN1)).toBigInteger();
    } else {
      return new BigInteger(toncoins).multiply(BigInteger.valueOf(BLN1));
    }
  }

  public static BigInteger toNano(double toncoins) {
    checkToncoinsOverflow(
        BigDecimal.valueOf(toncoins).multiply(BigDecimal.valueOf(BLN1)).toBigInteger());
    if (BigDecimal.valueOf(toncoins).scale() > 9) {
      throw new Error("Round the number to 9 decimals first");
    }
    return BigDecimal.valueOf(toncoins).multiply(BigDecimal.valueOf(BLN1)).toBigInteger();
  }

  public static BigInteger toNano(float toncoins) {
    checkToncoinsOverflow(
        BigDecimal.valueOf(toncoins).multiply(BigDecimal.valueOf(BLN1)).toBigInteger());
    if (BigDecimal.valueOf(toncoins).scale() > 9) {
      throw new Error("Round the number to 9 decimals first");
    }
    return BigDecimal.valueOf(toncoins).multiply(BigDecimal.valueOf(BLN1)).toBigInteger();
  }

  public static BigInteger toNano(BigDecimal toncoins) {
    checkToncoinsOverflow(toncoins.multiply(BigDecimal.valueOf(BLN1)).toBigInteger());
    if (toncoins.scale() > 9) {
      throw new Error("Round the number to 9 decimals first");
    }
    return toncoins.multiply(BigDecimal.valueOf(BLN1)).toBigInteger();
  }

  public static BigDecimal fromNano(BigInteger nanoCoins) {
    checkToncoinsOverflow(nanoCoins);
    return new BigDecimal(nanoCoins).divide(BigDecimal.valueOf(BLN1), 9, RoundingMode.HALF_UP);
  }

  public static BigDecimal fromNano(String nanoCoins) {
    checkToncoinsOverflow(new BigInteger(nanoCoins));
    return new BigDecimal(nanoCoins).divide(BigDecimal.valueOf(BLN1), 9, RoundingMode.HALF_UP);
  }

  public static BigDecimal fromNano(long nanoCoins) {
    checkToncoinsOverflow(BigInteger.valueOf(nanoCoins));
    return new BigDecimal(nanoCoins).divide(BigDecimal.valueOf(BLN1), 9, RoundingMode.HALF_UP);
  }

  public static String formatCoins(BigDecimal toncoins) {
    checkToncoinsOverflow(toncoins.multiply(BigDecimal.valueOf(BLN1)).toBigInteger());
    if (toncoins.scale() > 9) {
      throw new Error("Round the number to 9 decimals first");
    }
    return String.format("%,.9f", toncoins.multiply(BigDecimal.valueOf(BLN1)));
  }

  public static String formatCoins(String toncoins) {
    BigInteger nano = toNano(toncoins);
    return formatNanoValue(nano);
  }

  public static String formatCoins(BigDecimal toncoins, int scale) {
    BigInteger nano = toNano(toncoins);
    return formatNanoValue(nano, scale);
  }

  public static String formatCoins(String toncoins, int scale) {
    BigInteger nano = toNano(toncoins);
    return formatNanoValue(nano, scale);
  }

  public static String formatNanoValue(String nanoCoins) {
    checkToncoinsOverflow(new BigInteger(nanoCoins));
    return String.format(
        "%,.9f",
        new BigDecimal(nanoCoins).divide(BigDecimal.valueOf(BLN1), 9, RoundingMode.HALF_UP));
  }

  public static String formatNanoValue(long nanoCoins) {
    checkToncoinsOverflow(BigInteger.valueOf(nanoCoins));
    return String.format(
        "%,.9f",
        new BigDecimal(nanoCoins).divide(BigDecimal.valueOf(BLN1), 9, RoundingMode.HALF_UP));
  }

  public static String formatNanoValue(BigInteger nanoCoins) {
    checkToncoinsOverflow(nanoCoins);
    return String.format(
        "%,.9f",
        new BigDecimal(nanoCoins).divide(BigDecimal.valueOf(BLN1), 9, RoundingMode.HALF_UP));
  }

  public static String formatNanoValueZero(BigInteger nanoCoins) {
    if (isNull(nanoCoins)) {
      return "N/A";
    }
    checkToncoinsOverflow(nanoCoins);
    if (nanoCoins.compareTo(BigInteger.ZERO) == 0) {
      return "0";
    } else {
      return String.format(
          "%,.9f",
          new BigDecimal(nanoCoins).divide(BigDecimal.valueOf(BLN1), 9, RoundingMode.HALF_UP));
    }
  }

  public static String formatNanoValue(String nanoCoins, int scale) {
    checkToncoinsOverflow(new BigInteger(nanoCoins));
    return String.format(
        "%,." + scale + "f",
        new BigDecimal(nanoCoins).divide(BigDecimal.valueOf(BLN1), scale, RoundingMode.HALF_UP));
  }

  public static String formatNanoValue(String nanoCoins, int scale, RoundingMode roundingMode) {
    checkToncoinsOverflow(new BigInteger(nanoCoins));
    return String.format(
        "%,." + scale + "f",
        new BigDecimal(nanoCoins).divide(BigDecimal.valueOf(BLN1), scale, roundingMode));
  }

  public static String formatJettonValue(String jettons, int decimals, int scale) {
    return String.format(
        "%,." + scale + "f",
        new BigDecimal(jettons).divide(BigDecimal.valueOf(Math.pow(10, decimals))),
        scale,
        RoundingMode.HALF_UP);
  }

  public static String formatJettonValue(BigInteger jettons, int decimals, int scale) {
    return String.format(
        "%,." + scale + "f",
        new BigDecimal(jettons).divide(BigDecimal.valueOf(Math.pow(10, decimals))),
        scale,
        RoundingMode.HALF_UP);
  }

  public static String formatNanoValue(BigInteger nanoCoins, int scale) {
    checkToncoinsOverflow(nanoCoins);
    return String.format(
        "%,." + scale + "f",
        new BigDecimal(nanoCoins).divide(BigDecimal.valueOf(BLN1), scale, RoundingMode.HALF_UP));
  }

  public static void sleep(long seconds) {
    try {
      TimeUnit.SECONDS.sleep(seconds);
    } catch (Throwable e) {
      log.info(e.getMessage());
    }
  }

  public static void sleep(long seconds, String text) {
    try {
      log.info(String.format("pause %s seconds, %s", seconds, text));
      TimeUnit.SECONDS.sleep(seconds);
    } catch (Throwable e) {
      log.info(e.getMessage());
    }
  }

  public static int ip2int(String address) {
    String[] parts = address.split(Pattern.quote("."));

    if (parts.length != 4) {
      throw new Error("Invalid IP address format.");
    }

    int result = 0;
    for (String part : parts) {
      result = result << 8;
      result |= Integer.parseInt(part);
    }
    return result;
  }

  public static String int2ip(long ip) {
    if ((ip < 0) && (ip + Math.pow(2, 32) > Math.pow(2, 31))) {
      ip = (long) (ip + Math.pow(2, 32));
    }
    return ((ip >> 24) & 0xFF)
        + "."
        + ((ip >> 16) & 0xFF)
        + "."
        + ((ip >> 8) & 0xFF)
        + "."
        + (ip & 0xFF);
  }

  public static int[] reverseIntArray(int[] in) {
    int i = 0, j = in.length - 1;
    while (i < j) {
      int tmp = in[i];
      in[i] = in[j];
      in[j] = tmp;
      i++;
      j--;
    }
    return in;
  }

  public static byte[] reverseByteArray(byte[] in) {
    int i = 0, j = in.length - 1;
    while (i < j) {
      byte tmp = in[i];
      in[i] = in[j];
      in[j] = tmp;
      i++;
      j--;
    }
    return in;
  }

  public static long unsignedIntToLong(int x) {
    // Integer.toUnsignedLong()
    return x & 0x00000000ffffffffL;
  }

  public static int unsignedShortToInt(short x) {
    // Short.toUnsignedInt()
    return x & 0x0000ffff;
  }

  public static int unsignedByteToInt(byte x) {
    return x & 0x00ff;
  }

  private static void checkToncoinsOverflow(BigInteger amount) {
    int bytesSize = (int) Math.ceil((amount.bitLength() / (double) 8));
    if (bytesSize >= 16) {
      throw new Error("Value is too big. Maximum value 2^120-1");
    }
  }

  public static String generateString(int length, String character) {
    return RandomStringUtils.random(length, character);
  }

  public static byte[] leftPadBytes(byte[] bits, int sz, char c) {
    if (sz <= bits.length) {
      return bits;
    }

    int diff = sz - bits.length;
    byte[] b = new byte[sz];
    Arrays.fill(b, 0, diff, (byte) c);
    System.arraycopy(bits, 0, b, diff, bits.length);

    return b;
  }

  public static byte[] rightPadBytes(byte[] bits, int sz, char c) {
    if (sz <= bits.length) {
      return bits;
    }

    byte[] b = new byte[sz];
    System.arraycopy(bits, 0, b, 0, bits.length);
    Arrays.fill(b, bits.length, sz, (byte) c);

    return b;
  }

  public static int log2(int val) {
    return (int) Math.ceil(Math.log(val) / Math.log(2));
  }

  public static int[] uintToBytes(int l) {
    return new int[] {l};
  }

  public static byte[] byteToBytes(byte l) {
    return new byte[] {l};
  }

  public static boolean compareBytes(byte[] a, byte[] b) {
    return Arrays.equals(a, b);
  }

  public static String detectAbsolutePath(String appName, boolean library) {
    try {
      if (library) {
        appName = appName + "." + getLibraryExtension();
      }
      ProcessBuilder pb;
      if ((Utils.getOS() == Utils.OS.WINDOWS) || (Utils.getOS() == OS.WINDOWS_ARM)) {
        pb = new ProcessBuilder("where", appName).redirectErrorStream(true);
      } else {
        pb = new ProcessBuilder("which", appName).redirectErrorStream(true);
      }
      Process p = pb.start();
      p.waitFor(1, TimeUnit.SECONDS);
      String output =
          new BufferedReader(new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8))
              .lines()
              .collect(Collectors.joining("\n"));
      String[] paths = output.split("\n");
      if (paths.length == 1) {
        return paths[0];
      } else {
        for (String path : paths) {
          if (path.contains("ton")) {
            return StringUtils.trim(path);
          }
        }
      }
      return null;
    } catch (Exception e) {
      throw new Error(
          "Cannot detect absolute path to executable " + appName + ", " + e.getMessage());
    }
  }

  public static String getLibraryExtension() {
    if ((Utils.getOS() == Utils.OS.WINDOWS) || (Utils.getOS() == OS.WINDOWS_ARM)) {
      return "dll";
    } else if ((Utils.getOS() == OS.MAC) || (Utils.getOS() == OS.MAC_ARM64)) {
      return "dylib";
    } else {
      return "so";
    }
  }

  public static String getArtifactExtension(String artifactName) {
    if (artifactName.contains("emulator") || artifactName.contains("tonlib")) {
      if ((Utils.getOS() == Utils.OS.WINDOWS) || (Utils.getOS() == OS.WINDOWS_ARM)) {
        return ".dll";
      } else if ((Utils.getOS() == OS.MAC) || (Utils.getOS() == OS.MAC_ARM64)) {
        return ".dylib";
      } else {
        return ".so";
      }
    } else {
      if ((Utils.getOS() == Utils.OS.WINDOWS) || (Utils.getOS() == OS.WINDOWS_ARM)) {
        return ".exe";
      } else {
        return "";
      }
    }
  }

  public static int getRandomInt() {
    return new Random().nextInt();
  }

  public static long getRandomLong() {
    return new Random().nextLong();
  }

  public interface CStdLib extends Library {
    int dup(int oldfd); // Duplicate a file descriptor

    int dup2(int oldfd, int newfd); // Duplicate a file descriptor to a specified descriptor

    int close(int fd); // Close a file descriptor
  }

  // Redirect native output on Windows
  static WinNT.HANDLE originalOut;
  static WinNT.HANDLE originalErr;
  static int originalStdoutFD;
  static int originalStderrFD;
  static CStdLib cStdLib;

  public static void disableNativeOutput(int verbosityLevel) {
    if (verbosityLevel == 4) {
      return;
    }

    try {
      if ((Utils.getOS() == Utils.OS.WINDOWS) || (Utils.getOS() == Utils.OS.WINDOWS_ARM)) {
        // Redirect native output on Windows
        originalOut = Kernel32.INSTANCE.GetStdHandle(Kernel32.STD_OUTPUT_HANDLE);
        originalErr = Kernel32.INSTANCE.GetStdHandle(Kernel32.STD_ERROR_HANDLE);

        try {
          FileOutputStream nulStream = new FileOutputStream("NUL");

          WinNT.HANDLE hNul =
              Kernel32.INSTANCE.CreateFile(
                  "NUL",
                  Kernel32.GENERIC_WRITE,
                  Kernel32.FILE_SHARE_WRITE,
                  null,
                  Kernel32.OPEN_EXISTING,
                  0,
                  null);

          // Redirect stdout and stderr to NUL
          Kernel32.INSTANCE.SetStdHandle(Kernel32.STD_OUTPUT_HANDLE, hNul);
          Kernel32.INSTANCE.SetStdHandle(Kernel32.STD_ERROR_HANDLE, hNul);

          // Close the handle to NUL
          Kernel32.INSTANCE.CloseHandle(hNul);
        } catch (IOException e) {
          // throw new RuntimeException(e);
        }
      } else if ((Utils.getOS() == Utils.OS.LINUX) || (Utils.getOS() == Utils.OS.LINUX_ARM)) {
        try {
          // Load the native library
          cStdLib = Native.load("c", CStdLib.class);

          // Save original stdout and stderr file descriptors
          originalStdoutFD = cStdLib.dup(1);
          originalStderrFD = cStdLib.dup(2);

          // Redirect stdout and stderr to /dev/null
          try (FileOutputStream devNull = new FileOutputStream("/dev/null")) {
            // Get the file descriptor for /dev/null
            FileDescriptor fd = devNull.getFD();

            // Get the file descriptor integer value by accessing the private field via
            // reflection
            // Retrieve the field that holds the actual fd (in a private field)
            Field fdField = FileDescriptor.class.getDeclaredField("fd");
            fdField.setAccessible(true);
            int devNullFD = (int) fdField.get(fd);

            cStdLib.dup2(devNullFD, 1);
            cStdLib.dup2(devNullFD, 2);

          } catch (IOException | NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException(e);
          }
        } catch (Exception e) {
          // System.out.println("error here " + e.getMessage());
        }
      } else if ((Utils.getOS() == Utils.OS.MAC) || (Utils.getOS() == Utils.OS.MAC_ARM64)) {
        // Load the native library
        CStdLib cStdLib = Native.load("c", CStdLib.class);

        // Redirect stdout and stderr to /dev/null
        try (FileOutputStream devNull = new FileOutputStream("/dev/null")) {
          // Get the file descriptor for /dev/null
          FileDescriptor fd = devNull.getFD();

          // Get the file descriptor integer value by accessing the private field via     reflection
          // Retrieve the field that holds the actual fd (in a private field)
          Field fdField = FileDescriptor.class.getDeclaredField("fd");
          fdField.setAccessible(true);
          int devNullFD = (int) fdField.get(fd);

          // Duplicate and redirect stdout and stderr
          int stdoutFD = 1; // File descriptor for stdout
          int stderrFD = 2; // File descriptor for stderr
          cStdLib.dup2(devNullFD, stdoutFD);
          cStdLib.dup2(devNullFD, stderrFD);

        } catch (IOException | NoSuchFieldException | IllegalAccessException e) {
          throw new RuntimeException(e);
        }
      }
    } catch (Exception e) {
      // System.err.println("cannot disable native stdout");
    }
  }

  public static void enableNativeOutput(int verbosityLevel) {
    if (verbosityLevel == 4) {
      return;
    }
    try {
      if ((Utils.getOS() == Utils.OS.WINDOWS) || (Utils.getOS() == Utils.OS.WINDOWS_ARM)) {
        Kernel32.INSTANCE.SetStdHandle(Kernel32.STD_OUTPUT_HANDLE, originalOut);
        Kernel32.INSTANCE.SetStdHandle(Kernel32.STD_ERROR_HANDLE, originalErr);
      } else if ((Utils.getOS() == Utils.OS.LINUX) || (Utils.getOS() == Utils.OS.LINUX_ARM)) {
        cStdLib.dup2(originalStdoutFD, 1);
        cStdLib.dup2(originalStderrFD, 2);
      } else if ((Utils.getOS() == Utils.OS.MAC) || (Utils.getOS() == Utils.OS.MAC_ARM64)) {
        cStdLib.dup2(originalStdoutFD, 1);
        cStdLib.dup2(originalStderrFD, 2);
      }
    } catch (Exception e) {
      // System.err.println("cannot enable native stdout");
    }
    //    System.out.println("enable");
  }

  public static String convertShardIdentToShard(BigInteger shardPrefix, int prefixBits) {
    if (isNull(shardPrefix)) {
      throw new Error("Shard prefix is null, should be in range 0..60");
    }
    if (shardPrefix.compareTo(BigInteger.valueOf(60)) > 0) {
      return shardPrefix.toString(16);
    }
    return BigInteger.valueOf(2)
        .multiply(shardPrefix)
        .add(BigInteger.ONE)
        .shiftLeft(63 - prefixBits)
        .toString(16);
  }

  public static BigInteger longToUnsignedBigInteger(long num) {
    BigInteger b = BigInteger.valueOf(num);
    if (b.compareTo(BigInteger.ZERO) < 0) b = b.add(BigInteger.ONE.shiftLeft(64));
    return b;
  }

  public static BigInteger longToUnsignedBigInteger(String num) {
    BigInteger b = new BigInteger(num);
    if (b.compareTo(BigInteger.ZERO) < 0) b = b.add(BigInteger.ONE.shiftLeft(64));
    return b;
  }

  public static String getResourceAbsoluteDirectory(ClassLoader cl, String resource) {
    try {
      URL res = cl.getResource(resource);
      if (isNull(res)) {
        throw new Error("Cannot get integrated resource " + resource);
      }
      return Paths.get(res.toURI()).toFile().getAbsolutePath();
    } catch (Exception e) {
      throw new Error("Cannot get absolute directory of resource " + resource);
    }
  }

  public static long now() {
    return Instant.now().getEpochSecond();
  }

  /**
   * download file at address, if address is not a http link returns the address
   *
   * @param linkToFile String
   * @return absolute path to downloaded file or linkToFile if it is not a http url.
   */
  public static String getLocalOrDownload(String linkToFile) {
    if (linkToFile.contains("http") && linkToFile.contains("://")) {
      try {
        URL url = new URL(linkToFile);
        String filename = FilenameUtils.getName(url.getPath());

        File tmpFile = new File(filename);
        if (!tmpFile.exists()) {
          FileUtils.copyURLToFile(url, tmpFile);
          tmpFile.setExecutable(true);

        } else {
          log.info("{} already downloaded", filename);
        }
        return tmpFile.getAbsolutePath();

      } catch (Exception e) {
        log.error(e.getMessage());
        throw new Error("Cannot download file. Error " + e.getMessage());
      }
    } else {
      return linkToFile;
    }
  }

  public static String getLiteClientGithubUrl() {
    return getArtifactGithubUrl("lite-client", "");
  }

  public static String getEmulatorGithubUrl() {
    return getArtifactGithubUrl("libemulator", "");
  }

  public static String getTonlibGithubUrl() {
    return getArtifactGithubUrl("tonlibjson", "");
  }

  public static String getFuncGithubUrl() {
    return getArtifactGithubUrl("func", "");
  }

  public static String getTolkGithubUrl() {
    return getArtifactGithubUrl("tolk", "");
  }

  public static String getFiftGithubUrl() {
    return getArtifactGithubUrl("fift", "");
  }

  public static String getArtifactGithubUrl(String artifactName, String release) {
    return getArtifactGithubUrl(artifactName, release, "ton-blockchain", "ton");
  }

  public static String getArtifactGithubUrl(
      String artifactName, String release, String githubUsername, String githubRepository) {
    String baseUrl;
    if (StringUtils.isNotEmpty(release) && !release.contains("latest")) {

      baseUrl =
          "https://github.com/"
              + githubUsername
              + "/"
              + githubRepository
              + "/releases/download/"
              + release
              + "/";
    } else {
      baseUrl =
          "https://github.com/"
              + githubUsername
              + "/"
              + githubRepository
              + "/releases/latest/download/";
    }

    if ((getOS() == Utils.OS.WINDOWS) || (Utils.getOS() == OS.WINDOWS_ARM)) {
      return baseUrl + artifactName + getArtifactExtension(artifactName);
    } else if (Utils.getOS() == OS.MAC) {
      return baseUrl + artifactName + "-mac-x86-64" + getArtifactExtension(artifactName);
    } else if (Utils.getOS() == OS.MAC_ARM64) {
      return baseUrl + artifactName + "-mac-arm64" + getArtifactExtension(artifactName);
    } else if (Utils.getOS() == OS.LINUX) {
      return baseUrl + artifactName + "-linux-x86_64" + getArtifactExtension(artifactName);
    } else if (Utils.getOS() == OS.LINUX_ARM) {
      return baseUrl + artifactName + "-linux-arm64" + getArtifactExtension(artifactName);
    } else {
      throw new Error("unknown requested OS");
    }
  }
}
