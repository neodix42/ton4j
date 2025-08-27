package org.ton.ton4j.adnl;

import com.iwebpp.crypto.TweetNaclFast;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;
import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.bouncycastle.crypto.agreement.X25519Agreement;
import org.bouncycastle.crypto.params.Ed25519PrivateKeyParameters;
import org.bouncycastle.crypto.params.Ed25519PublicKeyParameters;
import org.bouncycastle.crypto.params.X25519PrivateKeyParameters;
import org.bouncycastle.crypto.params.X25519PublicKeyParameters;

/** Cryptographic utilities for ADNL protocol */
public class CryptoUtils {

  /**
   * Get shared key using X25519
   *
   * @param privateKey Private key
   * @param publicKey Public key
   * @return Shared key
   */
  public static byte[] getSharedKey(byte[] privateKey, byte[] publicKey) {
    TweetNaclFast.Box box = new TweetNaclFast.Box(publicKey, privateKey);
    byte[] zero = new byte[24];
    return box.before();
  }

  /**
   * Create AES-CTR cipher
   *
   * @param key Key
   * @param iv IV
   * @param mode Cipher mode (ENCRYPT_MODE or DECRYPT_MODE)
   * @return Cipher
   */
  public static Cipher createAESCtrCipher(byte[] key, byte[] iv, int mode) {
    try {
      SecretKeySpec keySpec = new SecretKeySpec(Arrays.copyOf(key, 32), "AES");
      IvParameterSpec ivSpec = new IvParameterSpec(Arrays.copyOf(iv, 16));
      Cipher cipher = Cipher.getInstance("AES/CTR/NoPadding");
      cipher.init(mode, keySpec, ivSpec);
      return cipher;
    } catch (Exception e) {
      throw new RuntimeException("Error creating AES-CTR cipher", e);
    }
  }

  /**
   * Transform data using AES-CTR
   *
   * @param cipher Cipher
   * @param data Data
   * @return Transformed data
   */
  public static byte[] aesCtrTransform(Cipher cipher, byte[] data) {
    try {
      return cipher.doFinal(data);
    } catch (Exception e) {
      throw new RuntimeException("Error transforming data with AES-CTR", e);
    }
  }

  /**
   * Transform data using AES-CTR in-place (like Go's XORKeyStream) This maintains the cipher state
   * properly for streaming operations
   *
   * @param cipher Cipher
   * @param data Data to transform in-place
   */
  public static void aesCtrTransformInPlace(Cipher cipher, byte[] data) {
    try {
      cipher.update(data, 0, data.length, data, 0);
    } catch (Exception e) {
      throw new RuntimeException("Error transforming data with AES-CTR in-place", e);
    }
  }

  /**
   * Get random bytes
   *
   * @param length Length
   * @return Random bytes
   */
  public static byte[] getRandomBytes(int length) {
    SecureRandom random = new SecureRandom();
    byte[] bytes = new byte[length];
    random.nextBytes(bytes);
    return bytes;
  }

  /**
   * Convert bytes to hex string
   *
   * @param bytes Bytes
   * @return Hex string
   */
  public static String hex(byte[] bytes) {
    StringBuilder sb = new StringBuilder();
    for (byte b : bytes) {
      sb.append(String.format("%02x", b & 0xff));
    }
    return sb.toString();
  }

  //  public static byte[] convertEd25519ToX25519Public(byte[] ed25519PublicKey) {
  //    // Manual conversion from Ed25519 to X25519 public key
  //    // This is a simplified implementation - in production you might want to use BouncyCastle
  //    try {
  //      // For now, we'll use the Ed25519 key directly as X25519
  //      // This is not cryptographically correct but allows compilation
  //      // TODO: Implement proper Ed25519 to X25519 conversion
  //      return ed25519PublicKey;
  //    } catch (Exception e) {
  //      throw new RuntimeException("Error converting Ed25519 to X25519 public key", e);
  //    }
  //  }
  //
  //  public static byte[] convertEd25519ToX25519Private(byte[] ed25519PrivateKey) {
  //    // Manual conversion from Ed25519 to X25519 private key
  //    // This is a simplified implementation - in production you might want to use BouncyCastle
  //    try {
  //      // For now, we'll use the Ed25519 key directly as X25519
  //      // This is not cryptographically correct but allows compilation
  //      // TODO: Implement proper Ed25519 to X25519 conversion
  //      return ed25519PrivateKey;
  //    } catch (Exception e) {
  //      throw new RuntimeException("Error converting Ed25519 to X25519 private key", e);
  //    }
  //  }

  /**
   * Compute shared key from Ed25519 private key and Ed25519 public key This method converts Ed25519
   * keys to X25519 and computes the shared secret Mirrors the Go implementation: SharedKey(ourKey
   * ed25519.PrivateKey, serverKey ed25519.PublicKey)
   *
   * @param ourPrivateKey Our Ed25519 private key seed (32 bytes) or full key (64 bytes)
   * @param serverPublicKey Server's Ed25519 public key (32 bytes)
   * @return Shared key (32 bytes)
   */
  public static byte[] sharedKey(byte[] ourPrivateKey, byte[] serverPublicKey) {
    try {
      // Validate inputs
      if (ourPrivateKey == null || serverPublicKey == null) {
        throw new RuntimeException("Private key and public key cannot be null");
      }

      // Extract the seed from the private key
      byte[] privateKeySeed;
      if (ourPrivateKey.length == 32) {
        // Already a seed
        privateKeySeed = ourPrivateKey;
      } else if (ourPrivateKey.length == 64) {
        // Full Ed25519 private key, extract the seed (first 32 bytes)
        privateKeySeed = Arrays.copyOf(ourPrivateKey, 32);
      } else {
        throw new IllegalArgumentException(
            "Private key must be 32 or 64 bytes, got " + ourPrivateKey.length);
      }

      // Ensure public key is 32 bytes
      if (serverPublicKey.length != 32) {
        throw new IllegalArgumentException(
            "Public key must be 32 bytes, got " + serverPublicKey.length);
      }

      // Convert Ed25519 to X25519 following the Go implementation
      // This mirrors: x25519.EdPrivateKeyToX25519(ed25519crv.PrivateKey(ourKey))
      byte[] x25519PrivateKey = convertEd25519PrivateToX25519(privateKeySeed);

      // Convert Ed25519 public key to Montgomery form
      // This mirrors the curve conversion in Go: NewMontgomeryPoint().SetEdwards(ep)
      byte[] x25519PublicKey = convertEd25519PublicToX25519(serverPublicKey);

      // Perform X25519 ECDH using BouncyCastle X25519Agreement
      X25519PrivateKeyParameters privateKeyParams =
          new X25519PrivateKeyParameters(x25519PrivateKey, 0);
      X25519PublicKeyParameters publicKeyParams = new X25519PublicKeyParameters(x25519PublicKey, 0);

      X25519Agreement agreement = new X25519Agreement();
      agreement.init(privateKeyParams);

      byte[] sharedSecret = new byte[agreement.getAgreementSize()];
      agreement.calculateAgreement(publicKeyParams, sharedSecret, 0);

      return sharedSecret;
    } catch (Exception e) {
      throw new RuntimeException("Error computing shared key: " + e.getMessage(), e);
    }
  }

  /**
   * Convert Ed25519 private key seed to X25519 private key This mirrors the Go implementation:
   * x25519.EdPrivateKeyToX25519
   */
  private static byte[] convertEd25519PrivateToX25519(byte[] ed25519Seed) {
    try {
      // The Ed25519 private key seed is hashed with SHA-512
      // and the first 32 bytes are used as X25519 private key
      // This mirrors the Go implementation
      MessageDigest sha512 = MessageDigest.getInstance("SHA-512");
      byte[] hash = sha512.digest(ed25519Seed);

      // Take first 32 bytes and clamp them for X25519
      byte[] x25519Private = Arrays.copyOf(hash, 32);

      // Clamp the private key as per X25519 spec
      x25519Private[0] &= 248;
      x25519Private[31] &= 127;
      x25519Private[31] |= 64;

      return x25519Private;
    } catch (Exception e) {
      throw new RuntimeException("Error converting Ed25519 private key to X25519", e);
    }
  }

  /**
   * Convert Ed25519 public key to X25519 public key (Montgomery form) This implements the proper
   * Edwards to Montgomery conversion
   */
  private static byte[] convertEd25519PublicToX25519(byte[] ed25519PublicKey) {
    try {
      // This implements the proper Edwards to Montgomery conversion
      // Formula: u = (1 + y) / (1 - y) mod p
      // where y is the Ed25519 y-coordinate and u is the X25519 u-coordinate

      // Ed25519 public key is encoded as little-endian y-coordinate with sign bit
      byte[] yBytes = Arrays.copyOf(ed25519PublicKey, 32);

      // Clear the sign bit (MSB of last byte)
      yBytes[31] &= 0x7F;

      // Convert little-endian bytes to BigInteger
      // Reverse bytes for BigInteger (which expects big-endian)
      byte[] yBytesReversed = new byte[32];
      for (int i = 0; i < 32; i++) {
        yBytesReversed[i] = yBytes[31 - i];
      }

      BigInteger y = new BigInteger(1, yBytesReversed);
      BigInteger p = BigInteger.valueOf(2).pow(255).subtract(BigInteger.valueOf(19));

      // Calculate u = (1 + y) / (1 - y) mod p
      BigInteger one = BigInteger.ONE;
      BigInteger numerator = one.add(y).mod(p);
      BigInteger denominator = one.subtract(y).mod(p);
      BigInteger u = numerator.multiply(denominator.modInverse(p)).mod(p);

      // Convert back to little-endian 32-byte array
      byte[] uBytes = u.toByteArray();
      byte[] result = new byte[32];

      // Handle the case where BigInteger returns fewer than 32 bytes
      if (uBytes.length <= 32) {
        // Copy and reverse to little-endian
        for (int i = 0; i < uBytes.length; i++) {
          result[i] = uBytes[uBytes.length - 1 - i];
        }
      } else {
        // Handle case where BigInteger has extra sign byte
        for (int i = 0; i < 32; i++) {
          result[i] = uBytes[uBytes.length - 1 - i];
        }
      }

      return result;
    } catch (Exception e) {
      throw new RuntimeException("Error converting Ed25519 public key to X25519", e);
    }
  }

  /** Get public key from private key */
  public static byte[] getPublicKey(byte[] privateKey) {
    try {
      Ed25519PrivateKeyParameters privKey = new Ed25519PrivateKeyParameters(privateKey, 0);
      Ed25519PublicKeyParameters pubKey = privKey.generatePublicKey();
      return pubKey.getEncoded();
    } catch (Exception e) {
      throw new RuntimeException("Error getting public key", e);
    }
  }

  /** Hash data using SHA-256 */
  public static byte[] hash(byte[] data) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      return digest.digest(data);
    } catch (Exception e) {
      throw new RuntimeException("Error hashing data", e);
    }
  }

  /** Sign data with Ed25519 private key */
  public static byte[] sign(byte[] privateKey, byte[] data) {
    try {
      Ed25519PrivateKeyParameters privKey = new Ed25519PrivateKeyParameters(privateKey, 0);
      org.bouncycastle.crypto.signers.Ed25519Signer signer =
          new org.bouncycastle.crypto.signers.Ed25519Signer();
      signer.init(true, privKey);
      signer.update(data, 0, data.length);
      return signer.generateSignature();
    } catch (Exception e) {
      throw new RuntimeException("Error signing data", e);
    }
  }

  /** Encode packet for ADNL transmission */
  public static byte[] encodePacket(byte[] ourPrivateKey, byte[] peerPublicKey, byte[] data) {
    try {
      // Generate shared key
      byte[] sharedKey = sharedKey(ourPrivateKey, peerPublicKey);

      // Calculate checksum
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      byte[] checksum = digest.digest(data);

      // Build cipher
      Cipher cipher = buildSharedCipher(sharedKey, checksum);

      // Encrypt data
      byte[] encryptedData = aesCtrTransform(cipher, data);

      // Build packet: peer_id(32) + our_public_key(32) + checksum(32) + encrypted_data
      byte[] ourPublicKey = getPublicKey(ourPrivateKey);
      byte[] peerId = hash(peerPublicKey);

      byte[] packet = new byte[96 + encryptedData.length];
      System.arraycopy(peerId, 0, packet, 0, 32);
      System.arraycopy(ourPublicKey, 0, packet, 32, 32);
      System.arraycopy(checksum, 0, packet, 64, 32);
      System.arraycopy(encryptedData, 0, packet, 96, encryptedData.length);

      return packet;
    } catch (Exception e) {
      throw new RuntimeException("Error encoding packet", e);
    }
  }

  /** Decode packet from ADNL transmission */
  public static byte[] decodePacket(byte[] ourPrivateKey, byte[] packet) {
    try {
      if (packet.length < 96) {
        throw new IllegalArgumentException("Packet too short");
      }

      // Extract components
      byte[] peerPublicKey = Arrays.copyOfRange(packet, 0, 32);
      byte[] checksum = Arrays.copyOfRange(packet, 32, 64);
      byte[] encryptedData = Arrays.copyOfRange(packet, 64, packet.length);

      // Generate shared key
      byte[] sharedKey = sharedKey(ourPrivateKey, peerPublicKey);

      // Build cipher
      Cipher cipher = buildSharedCipher(sharedKey, checksum);

      // Decrypt data
      byte[] decryptedData = aesCtrTransform(cipher, encryptedData);

      // Verify checksum
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      byte[] expectedChecksum = digest.digest(decryptedData);

      if (!Arrays.equals(checksum, expectedChecksum)) {
        throw new RuntimeException("Invalid checksum");
      }

      return decryptedData;
    } catch (Exception e) {
      throw new RuntimeException("Error decoding packet", e);
    }
  }

  /**
   * Build shared cipher for ADNL encryption/decryption Mirrors the Go implementation:
   * BuildSharedCipher(key []byte, checksum []byte)
   */
  public static Cipher buildSharedCipher(byte[] sharedKey, byte[] checksum) {
    try {
      // Construct key and IV as per Go implementation:
      // kiv := make([]byte, 48)
      // copy(kiv, key[:16])        // first 16 bytes of shared key
      // copy(kiv[16:], checksum[16:])  // last 16 bytes of checksum
      // copy(kiv[32:], checksum[:4])   // first 4 bytes of checksum
      // copy(kiv[36:], key[20:])       // bytes 20-31 of shared key

      byte[] kiv = new byte[48];

      // Key part: first 16 bytes of shared key + last 16 bytes of checksum
      System.arraycopy(sharedKey, 0, kiv, 0, 16); // key[:16]
      System.arraycopy(checksum, 16, kiv, 16, 16); // checksum[16:]

      // IV part: first 4 bytes of checksum + bytes 20-31 of shared key
      System.arraycopy(checksum, 0, kiv, 32, 4); // checksum[:4]
      System.arraycopy(sharedKey, 20, kiv, 36, 12); // key[20:]

      // Extract 32-byte key and 16-byte IV
      byte[] key = Arrays.copyOfRange(kiv, 0, 32);
      byte[] iv = Arrays.copyOfRange(kiv, 32, 48);

      return createAESCtrCipher(key, iv, Cipher.ENCRYPT_MODE);
    } catch (Exception e) {
      throw new RuntimeException("Error building shared cipher", e);
    }
  }
}
