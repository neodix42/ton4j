package org.ton.java.adnl;

import com.iwebpp.crypto.TweetNaclFast;
import java.security.MessageDigest;
import lombok.Getter;

/** Server class for ADNL protocol */
@Getter
public class Server {
  private final String host;
  private final int port;
  private final byte[] ed25519Public;
  private final byte[] x25519Public;
  private final byte[] keyId;

  /**
   * Create a server with the specified host, port, and public key
   *
   * @param host Host
   * @param port Port
   * @param ed25519Public Ed25519 public key
   */
  public Server(String host, int port, byte[] ed25519Public) {
    this.host = host;
    this.port = port;
    this.ed25519Public = ed25519Public;

    // Convert Ed25519 public key to X25519 public key
    try {
      this.x25519Public = CryptoUtils.convertEd25519ToX25519Public(ed25519Public);
    } catch (Exception e) {
      throw new RuntimeException("Error converting Ed25519 public key to X25519", e);
    }

    // Calculate key ID
    this.keyId = getKeyId(ed25519Public);
  }

  /**
   * Create a server from a base64-encoded public key
   *
   * @param host Host
   * @param port Port
   * @param base64PublicKey Base64-encoded public key
   * @return Server instance
   */
  public static Server fromBase64(String host, int port, String base64PublicKey) {
    byte[] publicKey = java.util.Base64.getDecoder().decode(base64PublicKey);
    return new Server(host, port, publicKey);
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
