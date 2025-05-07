package org.ton.java.utils;

import static java.util.Objects.isNull;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;

/**
 * Utility class for TON-specific operations
 */
public final class TonUtils {
    private static final long BLN1 = 1000000000L;
    private static final BigInteger BI_BLN1 = BigInteger.valueOf(BLN1);
    private static final BigDecimal BD_BLN1 = BigDecimal.valueOf(BLN1);
    
    private TonUtils() {
        // Private constructor to prevent instantiation
    }
    
    /**
     * Convert TON coins to nanoTON with a specified precision
     * @param toncoins The TON coins
     * @param precision The precision
     * @return The nanoTON
     */
    public static BigInteger toNano(double toncoins, Integer precision) {
        return BigDecimal.valueOf(toncoins).multiply(BigDecimal.TEN.pow(precision)).toBigInteger();
    }
    
    /**
     * Convert TON coins to nanoTON with a specified precision
     * @param toncoins The TON coins
     * @param precision The precision
     * @return The nanoTON
     */
    public static BigInteger toNano(BigDecimal toncoins, Integer precision) {
        return toncoins.multiply(BigDecimal.TEN.pow(precision)).toBigInteger();
    }
    
    /**
     * Convert TON coins to nanoTON with a specified precision
     * @param toncoins The TON coins
     * @param precision The precision
     * @return The nanoTON
     */
    public static BigInteger toNano(String toncoins, Integer precision) {
        return new BigDecimal(toncoins).multiply(BigDecimal.TEN.pow(precision)).toBigInteger();
    }
    
    /**
     * Convert TON coins to nanoTON with a specified precision
     * @param toncoins The TON coins
     * @param precision The precision
     * @return The nanoTON
     */
    public static BigInteger toNano(long toncoins, Integer precision) {
        return new BigDecimal(toncoins).multiply(BigDecimal.TEN.pow(precision)).toBigInteger();
    }
    
    /**
     * Convert nanoTON to TON coins with a specified precision
     * @param nanoCoins The nanoTON
     * @param precision The precision
     * @return The TON coins
     */
    public static BigDecimal fromNano(BigInteger nanoCoins, Integer precision) {
        return new BigDecimal(nanoCoins)
                .divide(BigDecimal.TEN.pow(precision), precision, RoundingMode.FLOOR);
    }
    
    /**
     * Convert nanoTON to TON coins with a specified precision
     * @param nanoCoins The nanoTON
     * @param precision The precision
     * @return The TON coins
     */
    public static BigDecimal fromNano(BigDecimal nanoCoins, Integer precision) {
        return nanoCoins.divide(BigDecimal.TEN.pow(precision), precision, RoundingMode.FLOOR);
    }
    
    /**
     * Convert nanoTON to TON coins with a specified precision
     * @param nanoCoins The nanoTON
     * @param precision The precision
     * @return The TON coins
     */
    public static BigDecimal fromNano(String nanoCoins, Integer precision) {
        return new BigDecimal(nanoCoins)
                .divide(BigDecimal.TEN.pow(precision), precision, RoundingMode.FLOOR);
    }
    
    /**
     * Convert nanoTON to TON coins with a specified precision
     * @param nanoCoins The nanoTON
     * @param precision The precision
     * @return The TON coins
     */
    public static BigDecimal fromNano(long nanoCoins, Integer precision) {
        return new BigDecimal(nanoCoins)
                .divide(BigDecimal.TEN.pow(precision), precision, RoundingMode.FLOOR);
    }
    
    /**
     * Convert TON coins to nanoTON (9 decimals)
     * @param toncoins The TON coins
     * @return The nanoTON
     */
    public static BigInteger toNano(long toncoins) {
        BigInteger result = BigInteger.valueOf(toncoins).multiply(BI_BLN1);
        checkToncoinsOverflow(result);
        return result;
    }
    
    /**
     * Convert TON coins to nanoTON (9 decimals)
     * @param toncoins The TON coins
     * @return The nanoTON
     */
    public static BigInteger toNano(String toncoins) {
        BigInteger result;
        if (toncoins.matches("^\\d*\\.\\d+|\\d+\\.\\d*$")) {
            result = new BigDecimal(toncoins).multiply(BigDecimal.valueOf(BLN1)).toBigInteger();
        } else {
            result = new BigInteger(toncoins).multiply(BigInteger.valueOf(BLN1));
        }
        checkToncoinsOverflow(result);
        return result;
    }
    
    /**
     * Convert TON coins to nanoTON (9 decimals)
     * @param toncoins The TON coins
     * @return The nanoTON
     */
    public static BigInteger toNano(double toncoins) {
        if (BigDecimal.valueOf(toncoins).scale() > 9) {
            throw new Error("Round the number to 9 decimals first");
        }
        BigInteger result = BigDecimal.valueOf(toncoins).multiply(BigDecimal.valueOf(BLN1)).toBigInteger();
        checkToncoinsOverflow(result);
        return result;
    }
    
    /**
     * Convert TON coins to nanoTON (9 decimals)
     * @param toncoins The TON coins
     * @return The nanoTON
     */
    public static BigInteger toNano(float toncoins) {
        if (BigDecimal.valueOf(toncoins).scale() > 9) {
            throw new Error("Round the number to 9 decimals first");
        }
        BigInteger result = BigDecimal.valueOf(toncoins).multiply(BigDecimal.valueOf(BLN1)).toBigInteger();
        checkToncoinsOverflow(result);
        return result;
    }
    
    /**
     * Convert TON coins to nanoTON (9 decimals)
     * @param toncoins The TON coins
     * @return The nanoTON
     */
    public static BigInteger toNano(BigDecimal toncoins) {
        if (toncoins.scale() > 9) {
            throw new Error("Round the number to 9 decimals first");
        }
        BigInteger result = toncoins.multiply(BigDecimal.valueOf(BLN1)).toBigInteger();
        checkToncoinsOverflow(result);
        return result;
    }
    
    /**
     * Convert nanoTON to TON coins (9 decimals)
     * @param nanoCoins The nanoTON
     * @return The TON coins
     */
    public static BigDecimal fromNano(BigInteger nanoCoins) {
        checkToncoinsOverflow(nanoCoins);
        return new BigDecimal(nanoCoins).divide(BigDecimal.valueOf(BLN1), 9, RoundingMode.HALF_UP);
    }
    
    /**
     * Convert nanoTON to TON coins (9 decimals)
     * @param nanoCoins The nanoTON
     * @return The TON coins
     */
    public static BigDecimal fromNano(String nanoCoins) {
        BigInteger nanoCoinsBI = new BigInteger(nanoCoins);
        checkToncoinsOverflow(nanoCoinsBI);
        return new BigDecimal(nanoCoins).divide(BigDecimal.valueOf(BLN1), 9, RoundingMode.HALF_UP);
    }
    
    /**
     * Convert nanoTON to TON coins (9 decimals)
     * @param nanoCoins The nanoTON
     * @return The TON coins
     */
    public static BigDecimal fromNano(long nanoCoins) {
        checkToncoinsOverflow(BigInteger.valueOf(nanoCoins));
        return new BigDecimal(nanoCoins).divide(BigDecimal.valueOf(BLN1), 9, RoundingMode.HALF_UP);
    }
    
    /**
     * Format TON coins
     * @param toncoins The TON coins
     * @return The formatted TON coins
     */
    public static String formatCoins(BigDecimal toncoins) {
        checkToncoinsOverflow(toncoins.multiply(BigDecimal.valueOf(BLN1)).toBigInteger());
        if (toncoins.scale() > 9) {
            throw new Error("Round the number to 9 decimals first");
        }
        return String.format("%,.9f", toncoins.multiply(BigDecimal.valueOf(BLN1)));
    }
    
    /**
     * Format TON coins
     * @param toncoins The TON coins
     * @return The formatted TON coins
     */
    public static String formatCoins(String toncoins) {
        BigInteger nano = toNano(toncoins);
        return formatNanoValue(nano);
    }
    
    /**
     * Format TON coins with a specified scale
     * @param toncoins The TON coins
     * @param scale The scale
     * @return The formatted TON coins
     */
    public static String formatCoins(BigDecimal toncoins, int scale) {
        BigInteger nano = toNano(toncoins);
        return formatNanoValue(nano, scale);
    }
    
    /**
     * Format TON coins with a specified scale
     * @param toncoins The TON coins
     * @param scale The scale
     * @return The formatted TON coins
     */
    public static String formatCoins(String toncoins, int scale) {
        BigInteger nano = toNano(toncoins);
        return formatNanoValue(nano, scale);
    }
    
    /**
     * Format nanoTON
     * @param nanoCoins The nanoTON
     * @return The formatted nanoTON
     */
    public static String formatNanoValue(String nanoCoins) {
        checkToncoinsOverflow(new BigInteger(nanoCoins));
        return String.format(
                "%,.9f",
                new BigDecimal(nanoCoins).divide(BigDecimal.valueOf(BLN1), 9, RoundingMode.HALF_UP));
    }
    
    /**
     * Format nanoTON
     * @param nanoCoins The nanoTON
     * @return The formatted nanoTON
     */
    public static String formatNanoValue(long nanoCoins) {
        checkToncoinsOverflow(BigInteger.valueOf(nanoCoins));
        return String.format(
                "%,.9f",
                new BigDecimal(nanoCoins).divide(BigDecimal.valueOf(BLN1), 9, RoundingMode.HALF_UP));
    }
    
    /**
     * Format nanoTON
     * @param nanoCoins The nanoTON
     * @return The formatted nanoTON
     */
    public static String formatNanoValue(BigInteger nanoCoins) {
        checkToncoinsOverflow(nanoCoins);
        return String.format(
                "%,.9f",
                new BigDecimal(nanoCoins).divide(BigDecimal.valueOf(BLN1), 9, RoundingMode.HALF_UP));
    }
    
    /**
     * Format nanoTON with zero handling
     * @param nanoCoins The nanoTON
     * @return The formatted nanoTON
     */
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
    
    /**
     * Format nanoTON with a specified scale
     * @param nanoCoins The nanoTON
     * @param scale The scale
     * @return The formatted nanoTON
     */
    public static String formatNanoValue(String nanoCoins, int scale) {
        checkToncoinsOverflow(new BigInteger(nanoCoins));
        return String.format(
                "%,." + scale + "f",
                new BigDecimal(nanoCoins).divide(BigDecimal.valueOf(BLN1), scale, RoundingMode.HALF_UP));
    }
    
    /**
     * Format nanoTON with a specified scale and rounding mode
     * @param nanoCoins The nanoTON
     * @param scale The scale
     * @param roundingMode The rounding mode
     * @return The formatted nanoTON
     */
    public static String formatNanoValue(String nanoCoins, int scale, RoundingMode roundingMode) {
        checkToncoinsOverflow(new BigInteger(nanoCoins));
        return String.format(
                "%,." + scale + "f",
                new BigDecimal(nanoCoins).divide(BigDecimal.valueOf(BLN1), scale, roundingMode));
    }
    
    /**
     * Format jetton value
     * @param jettons The jettons
     * @param decimals The decimals
     * @param scale The scale
     * @return The formatted jetton value
     */
    public static String formatJettonValue(String jettons, int decimals, int scale) {
        return String.format(
                "%,." + scale + "f",
                new BigDecimal(jettons).divide(BigDecimal.valueOf(Math.pow(10, decimals))),
                scale,
                RoundingMode.HALF_UP);
    }
    
    /**
     * Format jetton value
     * @param jettons The jettons
     * @param decimals The decimals
     * @param scale The scale
     * @return The formatted jetton value
     */
    public static String formatJettonValue(BigInteger jettons, int decimals, int scale) {
        return String.format(
                "%,." + scale + "f",
                new BigDecimal(jettons).divide(BigDecimal.valueOf(Math.pow(10, decimals))),
                scale,
                RoundingMode.HALF_UP);
    }
    
    /**
     * Format nanoTON with a specified scale
     * @param nanoCoins The nanoTON
     * @param scale The scale
     * @return The formatted nanoTON
     */
    public static String formatNanoValue(BigInteger nanoCoins, int scale) {
        checkToncoinsOverflow(nanoCoins);
        return String.format(
                "%,." + scale + "f",
                new BigDecimal(nanoCoins).divide(BigDecimal.valueOf(BLN1), scale, RoundingMode.HALF_UP));
    }
    
    /**
     * Convert a shard identifier to a shard
     * @param shardPrefix The shard prefix
     * @param prefixBits The prefix bits
     * @return The shard
     */
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
    
    /**
     * Convert a long to an unsigned BigInteger
     * @param num The long
     * @return The unsigned BigInteger
     */
    public static BigInteger longToUnsignedBigInteger(long num) {
        BigInteger b = BigInteger.valueOf(num);
        if (b.compareTo(BigInteger.ZERO) < 0) b = b.add(BigInteger.ONE.shiftLeft(64));
        return b;
    }
    
    /**
     * Convert a string to an unsigned BigInteger
     * @param num The string
     * @return The unsigned BigInteger
     */
    public static BigInteger longToUnsignedBigInteger(String num) {
        BigInteger b = new BigInteger(num);
        if (b.compareTo(BigInteger.ZERO) < 0) b = b.add(BigInteger.ONE.shiftLeft(64));
        return b;
    }
    
    /**
     * Generate a random TON address
     * @param workchain The workchain
     * @return The random TON address
     */
    public static String generateRandomAddress(long workchain) {
        try {
            return workchain
                    + ":"
                    + ByteUtils.bytesToHex(
                    HashUtils.sha256AsArray(java.util.UUID.randomUUID().toString().getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        } catch (Throwable e) {
            throw new Error("cannot generate random address. Error " + e.getMessage());
        }
    }
    
    /**
     * Convert a signed bytes array to a BigInteger
     * @param intArray The signed bytes array
     * @return The BigInteger
     */
    public static BigInteger signedBytesArrayToBigInteger(int[] intArray) {
        // Convert int[] to byte[]
        byte[] byteArray = new byte[intArray.length * 4]; // Each int is 4 bytes
        for (int i = 0; i < intArray.length; i++) {
            byteArray[i * 4] = (byte) (intArray[i] >>> 24); // First byte (most significant byte)
            byteArray[i * 4 + 1] = (byte) (intArray[i] >>> 16); // Second byte
            byteArray[i * 4 + 2] = (byte) (intArray[i] >>> 8); // Third byte
            byteArray[i * 4 + 3] = (byte) (intArray[i]); // Fourth byte (least significant byte)
        }

        // Create and return BigInteger from byte array
        return new BigInteger(1, byteArray); // 1 for positive sign
    }
    
    /**
     * Check if a TON coins amount is too large
     * @param amount The amount
     */
    private static void checkToncoinsOverflow(BigInteger amount) {
        int bytesSize = (int) Math.ceil((amount.bitLength() / (double) 8));
        if (bytesSize >= 16) {
            throw new Error("Value is too big. Maximum value 2^120-1");
        }
    }
}
