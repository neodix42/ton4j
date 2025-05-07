package org.ton.java.utils;

import com.iwebpp.crypto.TweetNaclFast;
import java.io.*;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.Arrays;
import lombok.extern.slf4j.Slf4j;

/**
 * Facade class that delegates to specialized utility classes. This class provides a unified
 * interface to all utility methods.
 */
@Slf4j
public class Utils {

  // Prevent instantiation
  private Utils() {}

  // ===== CRC32Utils Delegations =====

  public static Long getCRC32ChecksumAsLong(byte[] bytes) {
    return CRC32Utils.getCRC32ChecksumAsLong(bytes);
  }

  public static String getCRC32ChecksumAsHex(byte[] bytes) {
    return CRC32Utils.getCRC32ChecksumAsHex(bytes);
  }

  public static byte[] getCRC32ChecksumAsBytes(byte[] bytes) {
    return CRC32Utils.getCRC32ChecksumAsBytes(bytes);
  }

  public static byte[] getCRC32ChecksumAsBytesReversed(byte[] bytes) {
    return CRC32Utils.getCRC32ChecksumAsBytesReversed(bytes);
  }

  // ===== CRC16Utils Delegations =====

  public static int getCRC16ChecksumAsInt(byte[] bytes) {
    return CRC16Utils.getCRC16ChecksumAsInt(bytes);
  }

  public static int calculateMethodId(String methodName) {
    return CRC16Utils.calculateMethodId(methodName);
  }

  public static String getCRC16ChecksumAsHex(byte[] bytes) {
    return CRC16Utils.getCRC16ChecksumAsHex(bytes);
  }

  public static byte[] getCRC16ChecksumAsBytes(byte[] bytes) {
    return CRC16Utils.getCRC16ChecksumAsBytes(bytes);
  }

  // ===== ByteUtils Delegations =====

  public static byte[] long4BytesToBytes(long l) {
    return ByteUtils.long4BytesToBytes(l);
  }

  public static int[] longToBytes(long l) {
    return ByteUtils.longToBytes(l);
  }

  public static long bytesToLong(final byte[] b) {
    return ByteUtils.bytesToLong(b);
  }

  public static int bytesToInt(final byte[] b) {
    return ByteUtils.bytesToInt(b);
  }

  public static int bytesToIntX(final byte[] b) {
    return ByteUtils.bytesToIntX(b);
  }

  public static short bytesToShort(final byte[] b) {
    return ByteUtils.bytesToShort(b);
  }

  public static long intsToLong(final int[] b) {
    return ByteUtils.intsToLong(b);
  }

  public static int intsToInt(final int[] b) {
    return ByteUtils.intsToInt(b);
  }

  public static short intsToShort(final int[] b) {
    return ByteUtils.intsToShort(b);
  }

  public static int[] intToIntArray(int l) {
    return ByteUtils.intToIntArray(l);
  }

  public static byte[] intToByteArray(int value) {
    return ByteUtils.intToByteArray(value);
  }
  
  /**
   * Optimized version of intToByteArray that writes directly to an existing buffer
   * This avoids creating a new byte array for each conversion
   * 
   * @param value The integer value to convert
   * @param buffer The buffer to write to
   */
  public static void intToByteArrayOptimized(int value, byte[] buffer) {
    ByteUtils.intToByteArrayOptimized(value, buffer);
  }

  public static byte[] concatBytes(byte[] a, byte[] b) {
    return ByteUtils.concatBytes(a, b);
  }

  public static byte[] slice(byte[] src, int from, int size) {
    return ByteUtils.slice(src, from, size);
  }

  public static byte[] unsignedBytesToSigned(int[] bytes) {
    return ByteUtils.unsignedBytesToSigned(bytes);
  }

  public static int[] signedBytesToUnsigned(byte[] bytes) {
    return ByteUtils.signedBytesToUnsigned(bytes);
  }

  public static String bytesToHex(byte[] raw) {
    return ByteUtils.bytesToHex(raw);
  }

  public static String bytesToHex(int[] raw) {
    return ByteUtils.bytesToHex(raw);
  }

  public static byte[] hexToSignedBytes(String hex) {
    return ByteUtils.hexToSignedBytes(hex);
  }

  // ===== HashUtils Delegations =====

  public static String sha256(final String base) {
    return HashUtils.sha256(base);
  }

  public static String sha256(int[] bytes) {
    return HashUtils.sha256(bytes);
  }

  public static byte[] sha256AsArray(byte[] bytes) {
    return HashUtils.sha256AsArray(bytes);
  }

  public static byte[] sha1AsArray(byte[] bytes) {
    return HashUtils.sha1AsArray(bytes);
  }

  public static byte[] md5AsArray(byte[] bytes) {
    return HashUtils.md5AsArray(bytes);
  }

  public static String md5(byte[] bytes) {
    return HashUtils.md5(bytes);
  }

  public static String sha256(byte[] bytes) {
    return HashUtils.sha256(bytes);
  }

  public static String sha1(byte[] bytes) {
    return HashUtils.sha1(bytes);
  }

  // ===== EncodingUtils Delegations =====

  public static String bitsToDec(boolean[] bits) {
    return EncodingUtils.bitsToDec(bits);
  }

  public static String bitsToHex(boolean[] bits) {
    return EncodingUtils.bitsToHex(bits);
  }

  public static String bytesToBitString(byte[] raw) {
    return EncodingUtils.bytesToBitString(raw);
  }

  public static String bytesToBitString(int[] raw) {
    return EncodingUtils.bytesToBitString(raw);
  }

  public static String base64UrlSafeToHexString(String base64) {
    return EncodingUtils.base64UrlSafeToHexString(base64);
  }

  public static String base64ToHexString(String base64) {
    return EncodingUtils.base64ToHexString(base64);
  }

  public static String hexStringToBase64UrlSafe(String hex)
      throws org.apache.commons.codec.DecoderException {
    return EncodingUtils.hexStringToBase64UrlSafe(hex);
  }

  public static String hexStringToBase64(String hex)
      throws org.apache.commons.codec.DecoderException {
    return EncodingUtils.hexStringToBase64(hex);
  }

  public static String base64ToBitString(String base64) {
    return EncodingUtils.base64ToBitString(base64);
  }

  public static String bytesToBase64(byte[] bytes) {
    return EncodingUtils.bytesToBase64(bytes);
  }

  public static String bytesToBase64(int[] bytes) {
    return EncodingUtils.bytesToBase64(bytes);
  }

  public static String bytesToBase64SafeUrl(byte[] bytes) {
    return EncodingUtils.bytesToBase64SafeUrl(bytes);
  }

  public static byte[] base64ToBytes(String base64) {
    return EncodingUtils.base64ToBytes(base64);
  }

  public static int[] base64ToUnsignedBytes(String base64) {
    return EncodingUtils.base64ToUnsignedBytes(base64);
  }

  public static byte[] base64ToSignedBytes(String base64) {
    return EncodingUtils.base64ToSignedBytes(base64);
  }

  public static byte[] base64SafeUrlToBytes(String base64) {
    return EncodingUtils.base64SafeUrlToBytes(base64);
  }

  public static String base64ToString(String base64) {
    return EncodingUtils.base64ToString(base64);
  }

  public static String stringToBase64(String str) {
    return EncodingUtils.stringToBase64(str);
  }

  public static String bitStringToHex(String binary) {
    return EncodingUtils.bitStringToHex(binary);
  }

  public static String bitStringToBase64(String binary)
      throws org.apache.commons.codec.DecoderException {
    return EncodingUtils.bitStringToBase64(binary);
  }

  public static String repeat(String str, int count) {
    return EncodingUtils.repeat(str, count);
  }

  public static String bitStringToBase64UrlSafe(String binary)
      throws org.apache.commons.codec.DecoderException {
    return EncodingUtils.bitStringToBase64UrlSafe(binary);
  }

  public static int[] bitStringToIntArray(String bitString) {
    return EncodingUtils.bitStringToIntArray(bitString);
  }

  public static byte[] bitStringToByteArray(String bitString) {
    return EncodingUtils.bitStringToByteArray(bitString);
  }

  // ===== CryptoUtils Delegations =====

  public static TweetNaclFast.Signature.KeyPair generateSignatureKeyPair() {
    return CryptoUtils.generateSignatureKeyPair();
  }

  public static TweetNaclFast.Box.KeyPair generateKeyPair() {
    return CryptoUtils.generateKeyPair();
  }

  public static TweetNaclFast.Signature.KeyPair generateSignatureKeyPairFromSeed(byte[] secretKey) {
    return CryptoUtils.generateSignatureKeyPairFromSeed(secretKey);
  }

  public static TweetNaclFast.Box.KeyPair generateKeyPairFromSecretKey(byte[] secretKey) {
    return CryptoUtils.generateKeyPairFromSecretKey(secretKey);
  }

  public static TweetNaclFast.Signature getSignature(byte[] pubKey, byte[] prvKey) {
    return CryptoUtils.getSignature(pubKey, prvKey);
  }

  public static TweetNaclFast.Signature.KeyPair keyPairFromHex(String hex) {
    return CryptoUtils.keyPairFromHex(hex);
  }

  public static byte[] signData(byte[] pubKey, byte[] prvKey, byte[] data) {
    return CryptoUtils.signData(pubKey, prvKey, data);
  }

  public static Secp256k1KeyPair getSecp256k1FromPrivateKey(String privateKeyHex) {
    return CryptoUtils.getSecp256k1FromPrivateKey(privateKeyHex);
  }

  public static Secp256k1KeyPair generateSecp256k1SignatureKeyPair() {
    return CryptoUtils.generateSecp256k1SignatureKeyPair();
  }

  public static byte[] generatePrivateKey() {
    return CryptoUtils.generatePrivateKey();
  }

  public static byte[] getPublicKey(byte[] privateKey) {
    return CryptoUtils.getPublicKey(privateKey);
  }

  public static SignatureWithRecovery signDataSecp256k1(
      byte[] data, byte[] privateKey, byte[] publicKey) {
    return CryptoUtils.signDataSecp256k1(data, privateKey, publicKey);
  }

  public static byte getRecoveryId(byte[] sigR, byte[] sigS, byte[] message, byte[] publicKey) {
    return CryptoUtils.getRecoveryId(sigR, sigS, message, publicKey);
  }

  public static byte[] recoverPublicKey(byte[] sigR, byte[] sigS, byte[] sigV, byte[] message) {
    return CryptoUtils.recoverPublicKey(sigR, sigS, sigV, message);
  }

  // ===== TimeUtils Delegations =====

  public static String toUTC(long timestamp) {
    return TimeUtils.toUTC(timestamp);
  }

  public static String toUTCTimeOnly(long timestamp) {
    return TimeUtils.toUTCTimeOnly(timestamp);
  }

  public static long now() {
    return TimeUtils.now();
  }

  public static void sleep(long seconds) {
    TimeUtils.sleep(seconds);
  }

  public static void sleepMs(long milliseconds) {
    TimeUtils.sleepMs(milliseconds);
  }

  public static void sleep(long seconds, String text) {
    TimeUtils.sleep(seconds, text);
  }

  public static void sleepMs(long milliseconds, String text) {
    TimeUtils.sleepMs(milliseconds, text);
  }

  // ===== NetworkUtils Delegations =====

  /** Enum for operating system types Kept for backward compatibility */
  public enum OS {
    WINDOWS,
    WINDOWS_ARM,
    LINUX,
    LINUX_ARM,
    MAC,
    MAC_ARM64,
    UNKNOWN
  }

  /** Convert NetworkUtils.OS to Utils.OS for backward compatibility */
  private static OS convertOS(NetworkUtils.OS os) {
    switch (os) {
      case WINDOWS:
        return OS.WINDOWS;
      case WINDOWS_ARM:
        return OS.WINDOWS_ARM;
      case LINUX:
        return OS.LINUX;
      case LINUX_ARM:
        return OS.LINUX_ARM;
      case MAC:
        return OS.MAC;
      case MAC_ARM64:
        return OS.MAC_ARM64;
      case UNKNOWN:
      default:
        return OS.UNKNOWN;
    }
  }

  /**
   * Detect the operating system
   *
   * @return The detected operating system
   */
  public static OS getOS() {
    return convertOS(NetworkUtils.getOS());
  }

  public static int ip2int(String address) {
    return NetworkUtils.ip2int(address);
  }

  public static String int2ip(long ip) {
    return NetworkUtils.int2ip(ip);
  }

  public static void disableNativeOutput(int verbosityLevel) {
    NetworkUtils.disableNativeOutput(verbosityLevel);
  }

  public static void enableNativeOutput(int verbosityLevel) {
    NetworkUtils.enableNativeOutput(verbosityLevel);
  }

  // ===== FileUtils Delegations =====

  public static String streamToString(InputStream is) {
    return FileUtils.streamToString(is);
  }

  public static String getResourceAbsoluteDirectory(ClassLoader cl, String resource) {
    return FileUtils.getResourceAbsoluteDirectory(cl, resource);
  }

  public static String getLocalOrDownload(String linkToFile) {
    return FileUtils.getLocalOrDownload(linkToFile);
  }

  public static String getLibraryExtension() {
    return FileUtils.getLibraryExtension();
  }

  public static String getArtifactExtension(String artifactName) {
    return FileUtils.getArtifactExtension(artifactName);
  }

  public static String detectAbsolutePath(String appName, boolean library) {
    return FileUtils.detectAbsolutePath(appName, library);
  }

  public static String getLiteClientGithubUrl() {
    return FileUtils.getLiteClientGithubUrl();
  }

  public static String getEmulatorGithubUrl() {
    return FileUtils.getEmulatorGithubUrl();
  }

  public static String getTonlibGithubUrl() {
    return FileUtils.getTonlibGithubUrl();
  }

  public static String getFuncGithubUrl() {
    return FileUtils.getFuncGithubUrl();
  }

  public static String getTolkGithubUrl() {
    return FileUtils.getTolkGithubUrl();
  }

  public static String getFiftGithubUrl() {
    return FileUtils.getFiftGithubUrl();
  }

  public static String getArtifactGithubUrl(String artifactName, String release) {
    return FileUtils.getArtifactGithubUrl(artifactName, release);
  }

  public static String getArtifactGithubUrl(
      String artifactName, String release, String githubUsername, String githubRepository) {
    return FileUtils.getArtifactGithubUrl(artifactName, release, githubUsername, githubRepository);
  }

  public static String getGlobalConfigUrlMainnet() {
    return FileUtils.getGlobalConfigUrlMainnet();
  }

  public static String getGlobalConfigUrlTestnet() {
    return FileUtils.getGlobalConfigUrlTestnet();
  }

  public static String getGlobalConfigUrlMainnetGithub() {
    return FileUtils.getGlobalConfigUrlMainnetGithub();
  }

  public static String getGlobalConfigUrlTestnetGithub() {
    return FileUtils.getGlobalConfigUrlTestnetGithub();
  }

  // ===== TonUtils Delegations =====

  public static BigInteger toNano(double toncoins, Integer precision) {
    return TonUtils.toNano(toncoins, precision);
  }

  public static BigInteger toNano(BigDecimal toncoins, Integer precision) {
    return TonUtils.toNano(toncoins, precision);
  }

  public static BigInteger toNano(String toncoins, Integer precision) {
    return TonUtils.toNano(toncoins, precision);
  }

  public static BigInteger toNano(long toncoins, Integer precision) {
    return TonUtils.toNano(toncoins, precision);
  }

  public static BigDecimal fromNano(BigInteger nanoCoins, Integer precision) {
    return TonUtils.fromNano(nanoCoins, precision);
  }

  public static BigDecimal fromNano(BigDecimal nanoCoins, Integer precision) {
    return TonUtils.fromNano(nanoCoins, precision);
  }

  public static BigDecimal fromNano(String nanoCoins, Integer precision) {
    return TonUtils.fromNano(nanoCoins, precision);
  }

  public static BigDecimal fromNano(long nanoCoins, Integer precision) {
    return TonUtils.fromNano(nanoCoins, precision);
  }

  public static BigInteger toNano(long toncoins) {
    return TonUtils.toNano(toncoins);
  }

  public static BigInteger toNano(String toncoins) {
    return TonUtils.toNano(toncoins);
  }

  public static BigInteger toNano(double toncoins) {
    return TonUtils.toNano(toncoins);
  }

  public static BigInteger toNano(float toncoins) {
    return TonUtils.toNano(toncoins);
  }

  public static BigInteger toNano(BigDecimal toncoins) {
    return TonUtils.toNano(toncoins);
  }

  public static BigDecimal fromNano(BigInteger nanoCoins) {
    return TonUtils.fromNano(nanoCoins);
  }

  public static BigDecimal fromNano(String nanoCoins) {
    return TonUtils.fromNano(nanoCoins);
  }

  public static BigDecimal fromNano(long nanoCoins) {
    return TonUtils.fromNano(nanoCoins);
  }

  public static String formatCoins(BigDecimal toncoins) {
    return TonUtils.formatCoins(toncoins);
  }

  public static String formatCoins(String toncoins) {
    return TonUtils.formatCoins(toncoins);
  }

  public static String formatCoins(BigDecimal toncoins, int scale) {
    return TonUtils.formatCoins(toncoins, scale);
  }

  public static String formatCoins(String toncoins, int scale) {
    return TonUtils.formatCoins(toncoins, scale);
  }

  public static String formatNanoValue(String nanoCoins) {
    return TonUtils.formatNanoValue(nanoCoins);
  }

  public static String formatNanoValue(long nanoCoins) {
    return TonUtils.formatNanoValue(nanoCoins);
  }

  public static String formatNanoValue(BigInteger nanoCoins) {
    return TonUtils.formatNanoValue(nanoCoins);
  }

  public static String formatNanoValueZero(BigInteger nanoCoins) {
    return TonUtils.formatNanoValueZero(nanoCoins);
  }

  public static String formatNanoValue(String nanoCoins, int scale) {
    return TonUtils.formatNanoValue(nanoCoins, scale);
  }

  public static String formatNanoValue(String nanoCoins, int scale, RoundingMode roundingMode) {
    return TonUtils.formatNanoValue(nanoCoins, scale, roundingMode);
  }

  public static String formatJettonValue(String jettons, int decimals, int scale) {
    return TonUtils.formatJettonValue(jettons, decimals, scale);
  }

  public static String formatJettonValue(BigInteger jettons, int decimals, int scale) {
    return TonUtils.formatJettonValue(jettons, decimals, scale);
  }

  public static String formatNanoValue(BigInteger nanoCoins, int scale) {
    return TonUtils.formatNanoValue(nanoCoins, scale);
  }

  public static String convertShardIdentToShard(BigInteger shardPrefix, int prefixBits) {
    return TonUtils.convertShardIdentToShard(shardPrefix, prefixBits);
  }

  public static BigInteger longToUnsignedBigInteger(long num) {
    return TonUtils.longToUnsignedBigInteger(num);
  }

  public static BigInteger longToUnsignedBigInteger(String num) {
    return TonUtils.longToUnsignedBigInteger(num);
  }

  public static String generateRandomAddress(long workchain) {
    return TonUtils.generateRandomAddress(workchain);
  }

  public static BigInteger signedBytesArrayToBigInteger(int[] intArray) {
    return TonUtils.signedBytesArrayToBigInteger(intArray);
  }

  // ===== AnsiColors Delegations =====

  public static final String ANSI_RESET = AnsiColors.ANSI_RESET;
  public static final String ANSI_BLACK = AnsiColors.ANSI_BLACK;
  public static final String ANSI_RED = AnsiColors.ANSI_RED;
  public static final String ANSI_GREEN = AnsiColors.ANSI_GREEN;
  public static final String ANSI_YELLOW = AnsiColors.ANSI_YELLOW;
  public static final String ANSI_BLUE = AnsiColors.ANSI_BLUE;
  public static final String ANSI_PURPLE = AnsiColors.ANSI_PURPLE;
  public static final String ANSI_CYAN = AnsiColors.ANSI_CYAN;
  public static final String ANSI_WHITE = AnsiColors.ANSI_WHITE;

  public static final String ANSI_BLACK_BACKGROUND = AnsiColors.ANSI_BLACK_BACKGROUND;
  public static final String ANSI_RED_BACKGROUND = AnsiColors.ANSI_RED_BACKGROUND;
  public static final String ANSI_GREEN_BACKGROUND = AnsiColors.ANSI_GREEN_BACKGROUND;
  public static final String ANSI_YELLOW_BACKGROUND = AnsiColors.ANSI_YELLOW_BACKGROUND;
  public static final String ANSI_BLUE_BACKGROUND = AnsiColors.ANSI_BLUE_BACKGROUND;
  public static final String ANSI_PURPLE_BACKGROUND = AnsiColors.ANSI_PURPLE_BACKGROUND;
  public static final String ANSI_CYAN_BACKGROUND = AnsiColors.ANSI_CYAN_BACKGROUND;
  public static final String ANSI_WHITE_BACKGROUND = AnsiColors.ANSI_WHITE_BACKGROUND;

  // ===== Additional Utility Methods =====

  public static int[] reverseIntArray(int[] in) {
    int[] result = Arrays.copyOf(in, in.length);
    int i = 0, j = result.length - 1;
    while (i < j) {
      int tmp = result[i];
      result[i] = result[j];
      result[j] = tmp;
      i++;
      j--;
    }
    return result;
  }

  public static byte[] reverseByteArray(byte[] in) {
    byte[] result = Arrays.copyOf(in, in.length);
    int i = 0, j = result.length - 1;
    while (i < j) {
      byte tmp = result[i];
      result[i] = result[j];
      result[j] = tmp;
      i++;
      j--;
    }
    return result;
  }

  public static long unsignedIntToLong(int x) {
    return x & 0x00000000ffffffffL;
  }

  public static int unsignedShortToInt(short x) {
    return x & 0x0000ffff;
  }

  public static int unsignedByteToInt(byte x) {
    return x & 0x00ff;
  }

  public static String generateString(int length, String character) {
    return org.apache.commons.lang3.RandomStringUtils.random(length, character);
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

  public static int log2Ceil(int val) {
    return Integer.SIZE - Integer.numberOfLeadingZeros(val - 1);
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

  public static int getRandomInt() {
    return new java.util.Random().nextInt();
  }

  public static long getRandomLong() {
    return new java.util.Random().nextLong();
  }

  public static int dynInt(int[] data) {
    int[] tmp = new int[8];
    System.arraycopy(data, 0, tmp, 8 - data.length, data.length);
    return Integer.valueOf(bytesToHex(tmp), 16);
  }

  public static int dynInt(byte[] data) {
    byte[] tmp = new byte[8];
    System.arraycopy(data, 0, tmp, 8 - data.length, data.length);
    return Integer.valueOf(bytesToHex(tmp), 16);
  }

  public static byte[] dynamicIntBytes(BigInteger val, int sz) {
    byte[] tmp = new byte[8];
    byte[] valArray = val.toByteArray();
    for (int i = 8 - valArray.length, j = 0; i < 8; i++, j++) {
      tmp[i] = valArray[j];
    }
    byte[] result = new byte[sz];
    System.arraycopy(tmp, 8 - sz, result, 0, sz);
    return result;
  }

  public static int[] concatBytes(int[] a, int[] b) {
    int[] c = new int[a.length + b.length];
    System.arraycopy(a, 0, c, 0, a.length);
    System.arraycopy(b, 0, c, a.length, b.length);
    return c;
  }

  public static int[] append(int[] dst, int[] with) {
    int[] result = Arrays.copyOf(dst, dst.length + with.length);
    System.arraycopy(with, 0, result, dst.length, with.length);
    return result;
  }

  public static byte[] append(byte[] dst, byte[] with) {
    byte[] result = Arrays.copyOf(dst, dst.length + with.length);
    System.arraycopy(with, 0, result, dst.length, with.length);
    return result;
  }

  public static byte[] appendByteArray(byte[] originalArray, byte[] appendArray) {
    byte[] resultArray = Arrays.copyOf(originalArray, originalArray.length + appendArray.length);
    System.arraycopy(appendArray, 0, resultArray, originalArray.length, appendArray.length);
    return resultArray;
  }

  public static int[] copy(int[] dst, int destPos, int[] src, int srcPos) {
    int[] result = Arrays.copyOf(dst, dst.length);
    System.arraycopy(src, srcPos, result, destPos, src.length);
    return result;
  }

  public static byte[] copy(byte[] dst, int destPos, byte[] src, int srcPos) {
    byte[] result = Arrays.copyOf(dst, dst.length);
    System.arraycopy(src, srcPos, result, destPos, src.length);
    return result;
  }

  public static int[] hexToUnsignedBytes(String hex) {
    return EncodingUtils.hexToUnsignedBytes(hex);
  }

  public static int[] hexToInts(String hex) {
    return EncodingUtils.hexToInts(hex);
  }
}
