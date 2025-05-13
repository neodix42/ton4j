package org.ton.ton4j.utils;

import com.iwebpp.crypto.TweetNaclFast;
import org.bouncycastle.asn1.x9.X9IntegerConverter;
import org.bouncycastle.crypto.params.ECDomainParameters;
import org.bouncycastle.crypto.params.ECPrivateKeyParameters;
import org.bouncycastle.crypto.params.ParametersWithRandom;
import org.bouncycastle.crypto.signers.ECDSASigner;
import org.bouncycastle.jce.ECNamedCurveTable;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.jce.spec.ECNamedCurveParameterSpec;
import org.bouncycastle.math.ec.ECAlgorithms;
import org.bouncycastle.math.ec.ECPoint;
import org.bouncycastle.util.BigIntegers;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.security.Security;
import java.util.LinkedList;

/**
 * Utility class for cryptographic operations
 */
public final class CryptoUtils {
    private static final String RANDOM_NUMBER_ALGORITHM = "SHA1PRNG";
    private static final String RANDOM_NUMBER_ALGORITHM_PROVIDER = "SUN";
    private static final String SECP256K1 = "secp256k1";
    public static final BigInteger MAXPRIVATEKEY =
            new BigInteger("00FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFEBAAEDCE6AF48A03BBFD25E8CD0364140", 16);

    public static final byte[] HIGH_S =
            ByteUtils.hexToSignedBytes("7FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF5D576E7357A4501DDFE92F46681B20A0");

    private CryptoUtils() {
        // Private constructor to prevent instantiation
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
     * @param pubKey 32 bytes public key
     * @param prvKey 32 or 64 bytes secret key
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

    public static TweetNaclFast.Signature.KeyPair keyPairFromHex(String hex) {
        return generateSignatureKeyPairFromSeed(ByteUtils.hexToSignedBytes(hex));
    }

    /**
     * Signs data using TweetNaclFast algorithm
     *
     * @param pubKey 32 bytes pubKey
     * @param prvKey 32 or 64 bytes prvKey
     * @param data data to sign
     * @return byte[] signature
     */
    public static byte[] signData(byte[] pubKey, byte[] prvKey, byte[] data) {
        return getSignature(pubKey, prvKey).detached(data);
    }

    /**
     * @param privateKeyHex 32 bytes private key in hex
     * @return Secp256k1KeyPair
     */
    public static Secp256k1KeyPair getSecp256k1FromPrivateKey(String privateKeyHex) {
        byte[] privateKey = ByteUtils.hexToSignedBytes(privateKeyHex);
        return Secp256k1KeyPair.builder()
                .privateKey(privateKey)
                .publicKey(getPublicKey(privateKey))
                .build();
    }

    public static Secp256k1KeyPair generateSecp256k1SignatureKeyPair() {
        byte[] privateKey = generatePrivateKey();
        return Secp256k1KeyPair.builder()
                .privateKey(privateKey)
                .publicKey(getPublicKey(privateKey))
                .build();
    }

    /** Generate a random private key that can be used with Secp256k1. */
    public static byte[] generatePrivateKey() {
        try {
            SecureRandom secureRandom =
                    SecureRandom.getInstance(RANDOM_NUMBER_ALGORITHM, RANDOM_NUMBER_ALGORITHM_PROVIDER);

            // Generate the key, skipping as many as desired.
            byte[] privateKeyAttempt = new byte[32];
            secureRandom.nextBytes(privateKeyAttempt);
            BigInteger privateKeyCheck = new BigInteger(1, privateKeyAttempt);
            while (privateKeyCheck.compareTo(BigInteger.ZERO) == 0
                    || privateKeyCheck.compareTo(MAXPRIVATEKEY) == 1) {
                secureRandom.nextBytes(privateKeyAttempt);
                privateKeyCheck = new BigInteger(1, privateKeyAttempt);
            }

            return privateKeyAttempt;
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    /** Converts a private key into its corresponding public key. */
    public static byte[] getPublicKey(byte[] privateKey) {
        try {
            ECNamedCurveParameterSpec spec = ECNamedCurveTable.getParameterSpec(SECP256K1);
            ECPoint pointQ = spec.getG().multiply(new BigInteger(1, privateKey));

            return pointQ.getEncoded(true);
        } catch (Exception e) {
            throw new RuntimeException("Error getting public key", e);
        }
    }

    public static SignatureWithRecovery signDataSecp256k1(
            byte[] data, byte[] privateKey, byte[] publicKey) {
        return signDataSecp256k1Once(data, privateKey, publicKey);
    }

    private static SignatureWithRecovery signDataSecp256k1Once(
            byte[] data, byte[] privateKey, byte[] publicKey) {
        try {
            Security.addProvider(new BouncyCastleProvider());
            ECNamedCurveParameterSpec spec = ECNamedCurveTable.getParameterSpec(SECP256K1);

            ECDSASigner ecdsaSigner = new ECDSASigner();
            ECDomainParameters domain = new ECDomainParameters(spec.getCurve(), spec.getG(), spec.getN());
            ECPrivateKeyParameters privateKeyParms =
                    new ECPrivateKeyParameters(new BigInteger(1, privateKey), domain);
            ParametersWithRandom params = new ParametersWithRandom(privateKeyParms);

            ecdsaSigner.init(true, params);

            BigInteger[] sig = ecdsaSigner.generateSignature(data);
            // Ensure r and s are 32 bytes
            byte[] rBytes = to32ByteArray(sig[0]);

            BigInteger highS = BigIntegers.fromUnsignedByteArray(HIGH_S);
            if (sig[1].compareTo(highS) >= 0) {
                sig[1] = domain.getN().subtract(sig[1]);
            }

            byte[] sBytes = to32ByteArray(sig[1]);

            LinkedList<byte[]> sigData = new LinkedList<>();
            byte recoveryId = getRecoveryId(rBytes, sBytes, data, publicKey);
            for (BigInteger sigChunk : sig) {
                sigData.add(to32ByteArray(sigChunk));
            }
            sigData.add(new byte[] {recoveryId});
            return SignatureWithRecovery.builder()
                    .r(sigData.get(0))
                    .s(sigData.get(1))
                    .v(sigData.get(2))
                    .build();

        } catch (Exception e) {
            throw new Error("cannot sign, error " + e.getMessage());
        }
    }

    private static byte[] to32ByteArray(BigInteger value) {
        byte[] rawBytes = value.toByteArray();

        if (rawBytes.length == 33 && rawBytes[0] == 0x00) {
            // Strip leading zero caused by sign bit
            byte[] trimmed = new byte[32];
            System.arraycopy(rawBytes, 1, trimmed, 0, 32);
            return trimmed;
        }

        if (rawBytes.length > 32) {
            throw new IllegalArgumentException(
                    "Value too large to fit in 32 bytes: " + value.toString(16));
        }

        if (rawBytes.length < 32) {
            // Pad with leading zeros
            byte[] padded = new byte[32];
            System.arraycopy(rawBytes, 0, padded, 32 - rawBytes.length, rawBytes.length);
            return padded;
        }

        return rawBytes; // Already 32 bytes
    }

    /**
     * Determine the recovery ID for the given signature and public key.
     *
     * <p>Any signed message can resolve to one of two public keys due to the nature ECDSA. The
     * recovery ID provides information about which one it is, allowing confirmation that the message
     * was signed by a specific key.
     */
    public static byte getRecoveryId(byte[] sigR, byte[] sigS, byte[] message, byte[] publicKey) {
        ECNamedCurveParameterSpec spec = ECNamedCurveTable.getParameterSpec(SECP256K1);
        BigInteger pointN = spec.getN();
        for (int recoveryId = 0; recoveryId < 2; recoveryId++) {
            try {
                BigInteger pointX = new BigInteger(1, sigR);

                X9IntegerConverter x9 = new X9IntegerConverter();
                byte[] compEnc = x9.integerToBytes(pointX, 1 + x9.getByteLength(spec.getCurve()));
                compEnc[0] = (byte) ((recoveryId & 1) == 1 ? 0x03 : 0x02);
                ECPoint pointR = spec.getCurve().decodePoint(compEnc);
                if (!pointR.multiply(pointN).isInfinity()) {
                    continue;
                }

                BigInteger pointE = new BigInteger(1, message);
                BigInteger pointEInv = BigInteger.ZERO.subtract(pointE).mod(pointN);
                BigInteger pointRInv = new BigInteger(1, sigR).modInverse(pointN);
                BigInteger srInv = pointRInv.multiply(new BigInteger(1, sigS)).mod(pointN);
                BigInteger pointEInvRInv = pointRInv.multiply(pointEInv).mod(pointN);
                ECPoint pointQ = ECAlgorithms.sumOfTwoMultiplies(spec.getG(), pointEInvRInv, pointR, srInv);
                byte[] pointQBytes = pointQ.getEncoded(true);
                boolean matchedKeys = true;
                for (int j = 0; j < publicKey.length; j++) {
                    if (pointQBytes[j] != publicKey[j]) {
                        matchedKeys = false;
                        break;
                    }
                }
                if (!matchedKeys) {
                    continue;
                }
                return (byte) (0xFF & recoveryId);
            } catch (Exception e) {
                throw new Error("getRecoveryId unexpected exception", e);
            }
        }

        return (byte) 0xFF;
    }

    /** Recover the public key that corresponds to the private key, which signed this message. */
    public static byte[] recoverPublicKey(byte[] sigR, byte[] sigS, byte[] sigV, byte[] message) {
        ECNamedCurveParameterSpec spec = ECNamedCurveTable.getParameterSpec(SECP256K1);
        BigInteger pointN = spec.getN();

        try {
            BigInteger pointX = new BigInteger(1, sigR);

            X9IntegerConverter x9 = new X9IntegerConverter();
            byte[] compEnc = x9.integerToBytes(pointX, 1 + x9.getByteLength(spec.getCurve()));
            compEnc[0] = (byte) ((sigV[0] & 1) == 1 ? 0x03 : 0x02); // Compressed format
            ECPoint pointR = spec.getCurve().decodePoint(compEnc);
            if (pointR.isInfinity()) {
                return new byte[0]; // Invalid point, unable to recover
            }

            BigInteger pointE = new BigInteger(1, message);
            BigInteger pointEInv = BigInteger.ZERO.subtract(pointE).mod(pointN);
            BigInteger pointRInv = new BigInteger(1, sigR).modInverse(pointN);
            BigInteger srInv = pointRInv.multiply(new BigInteger(1, sigS)).mod(pointN);
            BigInteger pointEInvRInv = pointRInv.multiply(pointEInv).mod(pointN);
            ECPoint pointQ = ECAlgorithms.sumOfTwoMultiplies(spec.getG(), pointEInvRInv, pointR, srInv);

            // Use compressed format for recovery
            return pointQ.getEncoded(true); // Compressed public key
        } catch (Exception e) {
            throw new RuntimeException("Error recovering public key from message", e);
        }
    }
}
