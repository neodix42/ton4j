package org.ton.ton4j.utils;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;

/** Utility class for encoding and decoding operations */
public final class EncodingUtils {

  private EncodingUtils() {
    // Private constructor to prevent instantiation
  }

  /**
   * Convert a Base64 string to a hex string
   *
   * @param base64 The Base64 string
   * @return The hex string
   */
  public static String base64UrlSafeToHexString(String base64) {
    if (base64 == null || base64.isEmpty()) {
      return "";
    }
    byte[] decoded = Base64.getUrlDecoder().decode(base64);
    return ByteUtils.bytesToHex(decoded);
  }

  /**
   * Convert a Base64 string to a hex string
   *
   * @param base64 The Base64 string
   * @return The hex string
   */
  public static String base64ToHexString(String base64) {
    if (base64 == null || base64.isEmpty()) {
      return "";
    }
    byte[] decoded = Base64.getDecoder().decode(base64);
    return ByteUtils.bytesToHex(decoded);
  }

  /**
   * Convert a hex string to a Base64 URL-safe string
   *
   * @param hex The hex string
   * @return The Base64 URL-safe string
   * @throws DecoderException If the hex string is invalid
   */
  public static String hexStringToBase64UrlSafe(String hex) throws DecoderException {
    if (hex == null || hex.isEmpty()) {
      return "";
    }
    byte[] decodedHex = Hex.decodeHex(hex);
    return new String(Base64.getUrlEncoder().encode(decodedHex));
  }

  /**
   * Convert a hex string to a Base64 string
   *
   * @param hex The hex string
   * @return The Base64 string
   * @throws DecoderException If the hex string is invalid
   */
  public static String hexStringToBase64(String hex) throws DecoderException {
    if (hex == null || hex.isEmpty()) {
      return "";
    }
    byte[] decodedHex = Hex.decodeHex(hex);
    return new String(Base64.getEncoder().encode(decodedHex));
  }

  /**
   * Convert a Base64 string to a bit string
   *
   * @param base64 The Base64 string
   * @return The bit string
   */
  public static String base64ToBitString(String base64) {
    if (base64 == null || base64.isEmpty()) {
      return "";
    }
    byte[] decode = Base64.getDecoder().decode(base64);
    return new BigInteger(1, decode).toString(2);
  }

  /**
   * Convert bytes to a Base64 string
   *
   * @param bytes The bytes
   * @return The Base64 string
   */
  public static String bytesToBase64(byte[] bytes) {
    if (bytes == null) {
      return "";
    }
    return Base64.getEncoder().encodeToString(bytes);
  }

  /**
   * Convert unsigned bytes to a Base64 string
   *
   * @param bytes The unsigned bytes
   * @return The Base64 string
   */
  public static String bytesToBase64(int[] bytes) {
    if (bytes == null) {
      return "";
    }
    return Base64.getEncoder().encodeToString(ByteUtils.unsignedBytesToSigned(bytes));
  }

  /**
   * Convert bytes to a Base64 URL-safe string
   *
   * @param bytes The bytes
   * @return The Base64 URL-safe string
   */
  public static String bytesToBase64SafeUrl(byte[] bytes) {
    if (bytes == null || bytes.length == 0) {
      return "";
    }
    return Base64.getUrlEncoder().encodeToString(bytes);
  }

  /**
   * Convert a Base64 string to bytes
   *
   * @param base64 The Base64 string
   * @return The bytes
   */
  public static byte[] base64ToBytes(String base64) {
    if (base64 == null || base64.isEmpty()) {
      return new byte[0];
    }
    return Base64.getDecoder().decode(base64.getBytes(StandardCharsets.UTF_8));
  }

  /**
   * Convert a Base64 string to unsigned bytes
   *
   * @param base64 The Base64 string
   * @return The unsigned bytes
   */
  public static int[] base64ToUnsignedBytes(String base64) {
    if (base64 == null || base64.isEmpty()) {
      return new int[0];
    }
    return ByteUtils.signedBytesToUnsigned(
        Base64.getDecoder().decode(base64.getBytes(StandardCharsets.UTF_8)));
  }

  /**
   * Convert a Base64 string to signed bytes
   *
   * @param base64 The Base64 string
   * @return The signed bytes
   */
  public static byte[] base64ToSignedBytes(String base64) {
    if (base64 == null || base64.isEmpty()) {
      return new byte[0];
    }
    return Base64.getDecoder().decode(base64.getBytes(StandardCharsets.UTF_8));
  }

  /**
   * Convert a Base64 URL-safe string to bytes
   *
   * @param base64 The Base64 URL-safe string
   * @return The bytes
   */
  public static byte[] base64SafeUrlToBytes(String base64) {
    if (base64 == null || base64.isEmpty()) {
      return new byte[0];
    }
    return Base64.getUrlDecoder().decode(base64.getBytes(StandardCharsets.UTF_8));
  }

  /**
   * Convert a Base64 string to a string
   *
   * @param base64 The Base64 string
   * @return The string
   */
  public static String base64ToString(String base64) {
    if (base64 == null || base64.isEmpty()) {
      return "";
    }
    return new String(Base64.getDecoder().decode(base64.getBytes(StandardCharsets.UTF_8)));
  }

  /**
   * Convert a string to a Base64 string
   *
   * @param str The string
   * @return The Base64 string
   */
  public static String stringToBase64(String str) {
    if (str == null || str.isEmpty()) {
      return "";
    }
    return Base64.getEncoder().encodeToString(str.getBytes(StandardCharsets.UTF_8));
  }

  /**
   * Convert a bit string to a hex string
   *
   * @param binary The bit string
   * @return The hex string
   */
  public static String bitStringToHex(String binary) {
    if (binary == null || binary.isEmpty()) {
      return "";
    }
    int toPad = (binary.length() % 8) == 0 ? 0 : 8 - (binary.length() % 8);
    final StringBuilder bits = new StringBuilder(binary);
    if (toPad != 0) {
      for (int i = 0; i < toPad; i++) {
        bits.append('0');
      }
    }
    return new BigInteger(bits.toString(), 2).toString(16);
  }

  /**
   * Convert a bit string to a Base64 string
   *
   * @param binary The bit string
   * @return The Base64 string
   * @throws DecoderException If the bit string is invalid
   */
  public static String bitStringToBase64(String binary) throws DecoderException {
    if (binary == null || binary.isEmpty()) {
      return "";
    }
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

  /**
   * Repeat a string a specified number of times
   *
   * @param str The string to repeat
   * @param count The number of times to repeat
   * @return The repeated string
   */
  public static String repeat(String str, int count) {
    if (str == null || count <= 0) {
      return "";
    }
    StringBuilder sb = new StringBuilder(str.length() * count);
    for (int i = 0; i < count; i++) {
      sb.append(str);
    }
    return sb.toString();
  }

  /**
   * Convert a bit string to a Base64 URL-safe string
   *
   * @param binary The bit string
   * @return The Base64 URL-safe string
   * @throws DecoderException If the bit string is invalid
   */
  public static String bitStringToBase64UrlSafe(String binary) throws DecoderException {
    if (binary == null || binary.isEmpty()) {
      return "";
    }
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

  /**
   * Convert a bit string to an int array
   *
   * @param bitString The bit string
   * @return The int array
   */
  public static int[] bitStringToIntArray(String bitString) {
    if (bitString == null || bitString.isEmpty()) {
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

  /**
   * Convert a bit string to a byte array
   *
   * @param bitString The bit string
   * @return The byte array
   */
  public static byte[] bitStringToByteArray(String bitString) {
    if (bitString == null || bitString.isEmpty()) {
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

  /**
   * Convert bits to a decimal string
   *
   * @param bits The bits
   * @return The decimal string
   */
  public static String bitsToDec(boolean[] bits) {
    if (bits == null || bits.length == 0) {
      return "";
    }
    StringBuilder s = new StringBuilder();
    for (boolean b : bits) {
      s.append(b ? '1' : '0');
    }
    return new BigInteger(s.toString(), 2).toString(10);
  }

  /**
   * Convert bits to a hex string
   *
   * @param bits The bits
   * @return The hex string
   */
  public static String bitsToHex(boolean[] bits) {
    if (bits == null || bits.length == 0) {
      return "";
    }
    StringBuilder s = new StringBuilder();
    for (boolean b : bits) {
      s.append(b ? '1' : '0');
    }
    return new BigInteger(s.toString(), 2).toString(16);
  }

  /**
   * Convert bytes to a bit string
   *
   * @param raw The bytes
   * @return The bit string
   */
  public static String bytesToBitString(byte[] raw) {
    if (raw == null || raw.length == 0) {
      return "";
    }
    String hex = ByteUtils.bytesToHex(ByteUtils.signedBytesToUnsigned(raw));
    BigInteger bi = new BigInteger(hex, 16);
    return bi.toString(2);
  }

  /**
   * Convert unsigned bytes to a bit string
   *
   * @param raw The unsigned bytes
   * @return The bit string
   */
  public static String bytesToBitString(int[] raw) {
    if (raw == null || raw.length == 0) {
      return "";
    }
    String hex = ByteUtils.bytesToHex(raw);
    BigInteger bi = new BigInteger(hex, 16);
    return bi.toString(2);
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
}
