package org.ton.java.adnl;

import com.iwebpp.crypto.TweetNaclFast;
import java.security.MessageDigest;
import java.security.SecureRandom;

/** Client class for ADNL protocol */
public class Client {
  private final byte[] ed25519Private;
  private final byte[] ed25519Public;
  private final byte[] x25519Private;
  private final byte[] x25519Public;
  private final byte[] keyId;

  /**
   * Create a client with the specified keys
   *
   * @param ed25519Private Ed25519 private key
   * @param ed25519Public Ed25519 public key
   */
  public Client(byte[] ed25519Private, byte[] ed25519Public) {
    this.ed25519Private = ed25519Private;
    this.ed25519Public = ed25519Public;

    // Convert Ed25519 keys to X25519 keys
    // TweetNaclFast.convertEd25519PrivateToX25519 expects a 32-byte key
    // If the private key is 64 bytes, we need to extract the seed (first 32 bytes)
    byte[] privateKeySeed = ed25519Private;
    if (ed25519Private.length == 64) {
        privateKeySeed = new byte[32];
        System.arraycopy(ed25519Private, 0, privateKeySeed, 0, 32);
    }
    
    this.x25519Private = TweetNaclFast.convertEd25519PrivateToX25519(privateKeySeed);
    this.x25519Public = TweetNaclFast.convertEd25519PublicToX25519(ed25519Public);

    // Calculate key ID
    this.keyId = getKeyId(ed25519Public);
  }

  /**
   * Generate a new client with random keys
   *
   * @return Client instance
   */
  public static Client generate() {
    SecureRandom random = new SecureRandom();
    byte[] seed = new byte[32];
    random.nextBytes(seed);

    TweetNaclFast.Signature.KeyPair keyPair = TweetNaclFast.Signature.keyPair_fromSeed(seed);

    return new Client(keyPair.getSecretKey(), keyPair.getPublicKey());
  }

  /**
   * Get Ed25519 private key
   *
   * @return Ed25519 private key
   */
  public byte[] getEd25519Private() {
    return ed25519Private;
  }

  /**
   * Get Ed25519 public key
   *
   * @return Ed25519 public key
   */
  public byte[] getEd25519Public() {
    return ed25519Public;
  }

  /**
   * Get X25519 private key
   *
   * @return X25519 private key
   */
  public byte[] getX25519Private() {
    return x25519Private;
  }

  /**
   * Get X25519 public key
   *
   * @return X25519 public key
   */
  public byte[] getX25519Public() {
    return x25519Public;
  }

  /**
   * Get key ID
   *
   * @return Key ID
   */
  public byte[] getKeyId() {
    return keyId;
  }

  /**
   * Sign data with Ed25519 private key
   *
   * @param data Data to sign
   * @return Signature
   */
  public byte[] sign(byte[] data) {
    TweetNaclFast.Signature signature = new TweetNaclFast.Signature(ed25519Public, ed25519Private);
    return signature.detached(data);
  }

  /**
   * Verify signature with Ed25519 public key
   *
   * @param signature Signature
   * @param data Data
   * @return True if signature is valid
   */
  public boolean verify(byte[] signature, byte[] data) {
    TweetNaclFast.Signature verifier = new TweetNaclFast.Signature(ed25519Public, null);
    return verifier.detached_verify(data, signature);
  }

  /**
   * Calculate key ID from public key
   *
   * @param publicKey Public key
   * @return Key ID
   */
  private static byte[] getKeyId(byte[] publicKey) {
    try {
      MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
      return sha256.digest(publicKey);
    } catch (Exception e) {
      throw new RuntimeException("SHA-256 error", e);
    }
  }
}
