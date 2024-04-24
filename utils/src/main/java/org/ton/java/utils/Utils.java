package org.ton.java.utils;

import com.iwebpp.crypto.TweetNaclFast;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Base64;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.zip.CRC32C;
import java.util.zip.Checksum;

import static java.util.Objects.nonNull;

public class Utils {
    private static final String HEXES = "0123456789ABCDEF";
    private static final long BLN1 = 1000000000L;

    public enum OS {
        WINDOWS, WINDOWS_ARM, LINUX, LINUX_ARM, MAC, MAC_ARM64, UNKNOWN
    }

    /**
     * uses POLY 0x1EDC6F41
     */
    public static Long getCRC32ChecksumAsLong(byte[] bytes) {
        Checksum crc32c = new CRC32C();
        crc32c.update(bytes, 0, bytes.length);
        return crc32c.getValue();
    }

    public static String getCRC32ChecksumAsHex(byte[] bytes) {
        return Long.toHexString(getCRC32ChecksumAsLong(bytes));
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
            result |= value;
        }
        return result;
    }

    public static short bytesToShort(final byte[] b) {
        short result = 0;
        for (int i = 0; i < 2; i++) {
            result <<= 8;
            result |= (b[i] & 0xFF);
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
            result |= b[i];
        }
        return result;
    }

    public static int[] intToIntArray(int l) {
        return new int[]{l};
    }

    public static byte[] intToByteArray(int value) {
        return new byte[]{
                (byte) (value >>> 8),
                (byte) value};
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
                if (hex.length() == 1)
                    hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    public static String sha256(int[] bytes) {
        byte[] converted = new byte[bytes.length];
        Byte[] a;

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

    public static String sha256(byte[] bytes) {
        try {
            final MessageDigest digest = MessageDigest.getInstance("SHA-256");
            final byte[] hash = digest.digest(bytes);
            return Utils.bytesToHex(hash);
        } catch (NoSuchAlgorithmException ex) {
            throw new RuntimeException(ex);
        }
    }

    public static String bitsToDec(boolean[] bits) {
        StringBuilder s = new StringBuilder();
        for (boolean b : bits) {
            s.append(b ? "1" : "0");
        }
        return new BigInteger(s.toString(), 2).toString(10);
    }

    public static String bitsToHex(boolean[] bits) {
        StringBuilder s = new StringBuilder();
        for (boolean b : bits) {
            s.append(b ? "1" : "0");
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
        return Utils.signedBytesToUnsigned(Base64.getDecoder().decode(base64.getBytes(StandardCharsets.UTF_8)));
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
            bits.append("0".repeat(toPad));
        }
        return new BigInteger(bits.toString(), 2).toString(16);
    }

    public static String bitStringToBase64(String binary) throws DecoderException {
        int toPad = (binary.length() % 8) == 0 ? 0 : 8 - (binary.length() % 8);
        final StringBuilder bits = new StringBuilder(binary);
        if (toPad != 0) {
            bits.append("0".repeat(toPad));
        }
        String hex = new BigInteger(bits.toString(), 2).toString(16);
        byte[] decodedHex = Hex.decodeHex(hex);
        return new String(Base64.getEncoder().encode(decodedHex));
    }

    public static String bitStringToBase64UrlSafe(String binary) throws DecoderException {
        int toPad = (binary.length() % 8) == 0 ? 0 : 8 - (binary.length() % 8);
        final StringBuilder bits = new StringBuilder(binary);
        if (toPad != 0) {
            bits.append("0".repeat(toPad));
        }
        String hex = new BigInteger(bits.toString(), 2).toString(16);
        byte[] decodedHex = Hex.decodeHex(hex);
        return new String(Base64.getUrlEncoder().encode(decodedHex));
    }

    public static int[] bitStringToIntArray(String bitString) {
        if (bitString.length() == 0) {
            return new int[0];
        }
        String bin = bitString;
        int[] result = new int[(int) Math.ceil(bin.length() / (double) 8)];

        for (int i = 0; i < bin.length(); i++) {
            if (bin.charAt(i) == '1') {
                result[(i / 8)] |= 1 << (7 - (i % 8));
            } else {
                result[(i / 8)] &= ~(1 << (7 - (i % 8)));
            }
        }

        return result;
    }

    public static byte[] bitStringToByteArray(String bitString) {
        if (bitString.length() == 0) {
            return new byte[0];
        }
        String bin = bitString;
        byte[] result = new byte[(byte) Math.ceil(bin.length() / (double) 8)];

        for (int i = 0; i < bin.length(); i++) {
            if (bin.charAt(i) == '1') {
                result[(i / 8)] |= 1 << (7 - (i % 8));
            } else {
                result[(i / 8)] &= ~(1 << (7 - (i % 8)));
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


    public static int[] copy(int[] dst, int destPos, int[] src, int srcPos) {
        System.arraycopy(src, srcPos, dst, destPos, src.length);
        return dst;
    }

    public static byte[] copy(byte[] dst, int destPos, byte[] src, int srcPos) {
        System.arraycopy(src, srcPos, dst, destPos, src.length);
        return dst;
    }

    public static int dynInt(int[] data) {
        int[] tmp = new int[8];
        Utils.copy(tmp, 8 - data.length, data, 0);
        //System.arraycopy(data, 0, tmp, 8 - data.length, data.length);

        return Integer.valueOf(Utils.bytesToHex(tmp), 16);
    }

//    public static int[] dynamicIntBytes(BigInteger val, int sz) {
//        byte[] tmp = new byte[8];
//        byte[] valArray = val.toByteArray(); // test just return val.toByteArray()
//        Arrays.fill(tmp, 0, 8 - val.toByteArray().length, (byte) 0);
//        for (int i = 8 - valArray.length, j = 0; i < 8; i++, j++) {
//            tmp[i] = valArray[j];
//        }
////        tmp = val.toByteArray();
//        byte[] result = new byte[sz];
//        System.arraycopy(tmp, 8 - sz, result, 0, sz);
//        return Utils.signedBytesToUnsigned(result);
//    }

    public static byte[] dynamicIntBytes(BigInteger val, int sz) {
        byte[] tmp = new byte[8];
        byte[] valArray = val.toByteArray(); // test just return val.toByteArray()
        Arrays.fill(tmp, 0, 8 - val.toByteArray().length, (byte) 0);
        for (int i = 8 - valArray.length, j = 0; i < 8; i++, j++) {
            tmp[i] = valArray[j];
        }
//        tmp = val.toByteArray();
        byte[] result = new byte[sz];
        System.arraycopy(tmp, 8 - sz, result, 0, sz);
        return result;
    }

    public static int log2(int val) {
        return (int) Math.ceil(Math.log(val) / Math.log(2));
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
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4) + Character.digit(s.charAt(i + 1), 16));
        }
        return data;
    }

    private static int[] hexStringToIntArray(String s) {
        String hex = s;
        int[] result = new int[hex.length() / 2];
        int j = 0;
        for (String str : hex.split("(?<=\\G.{2})")) {
            result[j++] = Integer.parseInt(str, 16);
        }
        return result;
    }

    /**
     * Signature algorithm, Implements ed25519.
     *
     * @return TweetNaclFast.Signature.KeyPair, where keyPair.getPublicKey() - 32 bytes and keyPair.getPrivateKey - 64 bytes
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
     * @return TweetNaclFast.Signature.KeyPair, where keyPair.getPublicKey() - 32 bytes and keyPair.getPrivateKey - 64 bytes
     */
    public static TweetNaclFast.Signature.KeyPair generateSignatureKeyPairFromSeed(byte[] secretKey) {
        return TweetNaclFast.Signature.keyPair_fromSeed(secretKey);
    }

    /**
     * @param secretKey 32 bytes secret key
     * @return TweetNaclFast.Box.KeyPair, where keyPair.getPublicKey() - 32 bytes and keyPair.getPrivateKey - 32 bytes
     */
    public static TweetNaclFast.Box.KeyPair generateKeyPairFromSecretKey(byte[] secretKey) {
        return TweetNaclFast.Box.keyPair_fromSecretKey(secretKey);
    }

    /**
     * If 32 bytes secret key is provided, then signature is generated out of it and its secret key is used.
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
     * @param data   data to sign
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
        return DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").format(LocalDateTime.ofEpochSecond(timestamp, 0, ZoneOffset.UTC));
    }

    public static OS getOS() {

        String operSys = System.getProperty("os.name").toLowerCase(Locale.ENGLISH);
        String operArch = System.getProperty("os.arch").toLowerCase(Locale.ENGLISH);
        System.out.println("Detected OS " + operSys + ", " + operArch);

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
            if ((operArch.contains("arm")) || (operArch.contains("aarch")) || (operArch.contains("m1"))) {
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

    public static BigInteger toNano(long toncoins) {
        return BigInteger.valueOf(toncoins * BLN1);
    }

    public static BigInteger toNano(String toncoins) {
        if (nonNull(toncoins)) {
            if (toncoins.matches("^\\d*\\.\\d+|\\d+\\.\\d*$")) {
                return new BigDecimal(toncoins).multiply(BigDecimal.valueOf(BLN1)).toBigInteger();
            } else {
                return new BigInteger(toncoins).multiply(BigInteger.valueOf(BLN1));
            }
        } else {
            throw new Error("Amount is null");
        }
    }

    public static BigInteger toNano(double toncoins) {
        if (BigDecimal.valueOf(toncoins).scale() > 9) {
            throw new Error("Round the number to 9 decimals first");
        }
        return BigDecimal.valueOf(toncoins * BLN1).toBigInteger();
    }

    public static BigInteger toNano(float toncoins) {
        if (BigDecimal.valueOf(toncoins).scale() > 9) {
            throw new Error("Round the number to 9 decimals first");
        }
        return BigDecimal.valueOf(toncoins * BLN1).toBigInteger();
    }

    public static BigInteger toNano(BigDecimal toncoins) {
        if (toncoins.scale() > 9) {
            throw new Error("Round the number to 9 decimals first");
        }
        return toncoins.multiply(BigDecimal.valueOf(BLN1)).toBigInteger();
    }

    public static BigDecimal fromNano(long toncoins) {
        return new BigDecimal(toncoins).divide(BigDecimal.valueOf(BLN1), 9, RoundingMode.CEILING);
    }

    public static BigDecimal fromNano(long toncoins, int scale) {
        return new BigDecimal(toncoins).divide(BigDecimal.valueOf(BLN1), scale, RoundingMode.CEILING);
    }

    public static String formatNanoValue(String value) {
        return String.format("%,.9f", new BigDecimal(value).divide(BigDecimal.valueOf(BLN1), 9, RoundingMode.CEILING));
    }

    public static String formatNanoValue(long value) {
        return String.format("%,.9f", new BigDecimal(value).divide(BigDecimal.valueOf(BLN1), 9, RoundingMode.CEILING));
    }

    public static String formatNanoValue(BigInteger value) {
        return String.format("%,.9f", new BigDecimal(value).divide(BigDecimal.valueOf(BLN1), 9, RoundingMode.CEILING));
    }

    public static String formatNanoValue(String value, int scale) {
        return String.format("%,." + scale + "f", new BigDecimal(value).divide(BigDecimal.valueOf(BLN1), scale, RoundingMode.CEILING));
    }

    public static void sleep(long seconds) {
        try {
            System.out.println("pause " + seconds + " seconds");
            TimeUnit.SECONDS.sleep(seconds);
        } catch (Throwable e) {
            System.out.println(e.getMessage());
        }
    }

    public static void sleep(long seconds, String text) {
        try {
            System.out.println("pause " + seconds + " seconds, " + text);
            TimeUnit.SECONDS.sleep(seconds);
        } catch (Throwable e) {
            System.out.println(e.getMessage());
        }
    }

    public static int ip2int(String address) {
        int result = 0;
        for (String part : address.split(Pattern.quote("."))) {
            result = result << 8;
            result |= Integer.parseInt(part);
        }
        return result;
    }

    public static int[] reverseIntArray(int[] in) {
        int[] temp = in.clone();
        for (int i = 0; i < temp.length / 2; i++) {
            int t = temp[i];
            temp[i] = temp[temp.length - i - 1];
            temp[temp.length - i - 1] = t;
        }
        return temp;
    }

    public static byte[] reverseByteArray(byte[] in) {
        byte[] temp = in.clone();
        for (int i = 0; i < temp.length / 2; i++) {
            byte t = temp[i];
            temp[i] = temp[temp.length - i - 1];
            temp[temp.length - i - 1] = t;
        }
        return temp;
    }

    public static long unsignedIntToLong(int x) {
        //Integer.toUnsignedLong()
        return x & 0x00000000ffffffffL;
    }

    public static int unsignedShortToInt(short x) {
        //Short.toUnsignedInt()
        return x & 0x0000ffff;
    }

    public static int unsignedByteToInt(byte x) {
        return x & 0x00ff;
    }
}
