package org.ton.ton4j.utils;

import lombok.Builder;
import lombok.Data;

/**
 * Secp256k1KeyPair cotains 32 bytes private key 33 bytes public key, where first byte 0x02 or 0x03
 * (compression)
 */
@Builder
@Data
public class Secp256k1KeyPair {
  public byte[] privateKey;
  public byte[] publicKey;

  public byte[] publicKeyNoPrefix() {
    return Utils.slice(publicKey, 1, publicKey.length - 1);
  }
}
