package org.ton.ton4j.address;

import static java.util.Objects.isNull;

import java.io.IOException;
import java.io.Serializable;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import org.ton.ton4j.utils.Utils;

/**
 * TON Address implementation with performance optimizations. Represents a TON blockchain address
 * with various formats and properties.
 */
public class Address implements Serializable {

  private static final long serialVersionUID = 1L;

  // Constants
  public static final byte BOUNCEABLE_TAG = 0x11;
  public static final byte NON_BOUNCEABLE_TAG = 0x51;
  public static final int TEST_FLAG = 0x80;

  // Bit flags for boolean properties (memory optimization)
  public static final byte FLAG_TEST_ONLY = 1; // 0000 0001
  public static final byte FLAG_USER_FRIENDLY = 2; // 0000 0010
  public static final byte FLAG_BOUNCEABLE = 4; // 0000 0100
  public static final byte FLAG_WALLET = 8; // 0000 1000
  public static final byte FLAG_URL_SAFE = 16; // 0001 0000

  // Core address data
  public byte wc;
  public byte[] hashPart;
  private byte flags = 0; // Bit field for boolean flags

  // Optional type information
  public AddressType addressType;

  /** Private constructor for internal use */
  private Address() {}

  /**
   * Constructs an Address from a string representation
   *
   * @param address The address string in any supported format
   * @throws IllegalArgumentException if address is null
   * @throws Error if address format is invalid
   */
  public Address(String address) {
    if (isNull(address)) {
      throw new IllegalArgumentException("Address is null");
    }

    // Process URL-safe format if needed
    boolean hasColon = address.indexOf(':') != -1;
    if (!hasColon) {
      if (address.indexOf('-') != -1 || address.indexOf('_') != -1) {
        setFlag(FLAG_URL_SAFE, true);
        // Convert to unsafe URL format
        address = address.replace('-', '+').replace('_', '/');
      }
    }

    // Process raw address format (workchain:hex)
    if (hasColon) {
      parseRawAddress(address);
    } else {
      // User-friendly format
      setFlag(FLAG_USER_FRIENDLY, true);
      Address parseResult = parseFriendlyAddress(address);
      wc = parseResult.wc;
      hashPart = parseResult.hashPart;
      flags = parseResult.flags; // Copy all flags from parsed result
    }
  }

  /** Parse raw address format (workchain:hex) */
  private void parseRawAddress(String address) {
    int colonIndex = address.indexOf(':');
    if (colonIndex != address.lastIndexOf(':')) {
      throw new Error("Invalid address " + address);
    }

    String wcPart = address.substring(0, colonIndex);
    String hexPart = address.substring(colonIndex + 1);

    byte wcInternal = Byte.parseByte(wcPart);
    if (wcInternal != 0 && wcInternal != -1) {
      throw new Error("Invalid address wc " + address);
    }

    // Normalize hex part
    if (hexPart.length() == 63) {
      hexPart = "0" + hexPart;
    } else if (hexPart.length() == 1) {
      hexPart = "000000000000000000000000000000000000000000000000000000000000000" + hexPart;
    }

    if (hexPart.length() != 64) {
      throw new Error("Invalid address hex " + address);
    }

    wc = wcInternal;
    hashPart = Utils.hexToSignedBytes(hexPart);
    setFlag(FLAG_WALLET, true); // Raw addresses are wallet by default
  }

  /**
   * Copy constructor
   *
   * @param address The address to copy
   * @throws IllegalArgumentException if address is null
   */
  public Address(Address address) {
    if (isNull(address)) {
      throw new IllegalArgumentException("Address is null");
    }

    wc = address.wc;
    hashPart = address.hashPart;
    flags = address.flags;
    addressType = address.addressType;
  }

  /**
   * Factory method to create an Address from a string
   *
   * @param address The address string
   * @return A new Address instance
   */
  public static Address of(String address) {
    return new Address(address);
  }

  /**
   * Factory method to create a bounceable address with default workchain (-1)
   *
   * @param hashCrc The hash part of the address
   * @return A new Address instance
   */
  public static Address of(byte[] hashCrc) {
    return of(BOUNCEABLE_TAG, -1, hashCrc);
  }

  /**
   * Factory method to create an address with specific flags, workchain and hash
   *
   * @param flags The address flags
   * @param wc The workchain ID
   * @param hashCrc The hash part of the address
   * @return A new Address instance
   */
  public static Address of(byte flags, int wc, byte[] hashCrc) {
    int flagsByte = flags & 0xff;
    boolean isTestOnly = false;
    boolean isBounceable;

    if ((flagsByte & TEST_FLAG) != 0) {
      isTestOnly = true;
      flagsByte = (byte) (flagsByte ^ TEST_FLAG);
    }

    if ((flagsByte != BOUNCEABLE_TAG) && (flagsByte != NON_BOUNCEABLE_TAG)) {
      throw new Error("Unknown address tag");
    }

    byte workchain;
    if ((wc & 0xff) == 0xff) {
      workchain = -1;
    } else {
      workchain = (byte) wc;
    }

    isBounceable = flagsByte == BOUNCEABLE_TAG;

    Address addr = new Address();
    addr.wc = workchain;
    addr.hashPart = hashCrc;
    addr.setFlag(FLAG_TEST_ONLY, isTestOnly);
    addr.setFlag(FLAG_BOUNCEABLE, isBounceable);
    addr.setFlag(FLAG_WALLET, !isBounceable);
    return addr;
  }

  /**
   * Factory method to create a copy of an existing address
   *
   * @param address The address to copy
   * @return A new Address instance
   */
  public static Address of(Address address) {
    return new Address(address);
  }

  /** Helper method to set or clear a flag bit */
  private void setFlag(byte flagBit, boolean value) {
    if (value) {
      flags |= flagBit;
    } else {
      flags &= ~flagBit;
    }
  }

  /** Helper method to check if a flag bit is set */
  private boolean hasFlag(byte flagBit) {
    return (flags & flagBit) != 0;
  }

  /**
   * @return true if this address is for testnet only
   */
  public boolean isTestOnly() {
    return hasFlag(FLAG_TEST_ONLY);
  }

  /**
   * @return true if this address is in user-friendly format
   */
  public boolean isUserFriendly() {
    return hasFlag(FLAG_USER_FRIENDLY);
  }

  /**
   * @return true if this address is bounceable
   */
  public boolean isBounceable() {
    return hasFlag(FLAG_BOUNCEABLE);
  }

  /**
   * @return true if this address is a wallet address
   */
  public boolean isWallet() {
    return hasFlag(FLAG_WALLET);
  }

  /**
   * @return true if this address is URL-safe
   */
  public boolean isUrlSafe() {
    return hasFlag(FLAG_URL_SAFE);
  }

  /**
   * Convert the hash part to decimal string
   *
   * @return Decimal representation of the hash
   */
  public String toDecimal() {
    return new BigInteger(Utils.bytesToHex(hashPart), 16).toString(10);
  }

  /**
   * Convert the hash part to BigInteger
   *
   * @return BigInteger representation of the hash
   */
  public BigInteger toBigInteger() {
    return new BigInteger(Utils.bytesToHex(hashPart), 16);
  }

  /**
   * Convert the hash part to hex string
   *
   * @return Hex representation of the hash
   */
  public String toHex() {
    return Utils.bytesToHex(hashPart);
  }

  /**
   * Save address to file in 36-byte format
   *
   * @param filename The file to save to
   * @throws IOException if file operations fail
   */
  public void saveToFile(String filename) throws IOException {
    // Preallocate buffer with exact size needed
    byte[] result = new byte[hashPart.length + 4];
    System.arraycopy(hashPart, 0, result, 0, hashPart.length);

    // Add workchain bytes
    ByteBuffer.wrap(result, hashPart.length, 4).putInt(wc);

    // Write in one operation
    Files.write(Paths.get(filename), result);
  }

  /**
   * Default flags are: userFriendly=true, UrlSafe=true, Bounceable=true and TestOnly=false
   *
   * @return String representation of the address
   */
  @Override
  public String toString() {
    return toBounceable();
  }

  /** Convert to string with specified user-friendly flag */
  public String toString(boolean isUserFriendly) {
    return toString(isUserFriendly, isUrlSafe(), isBounceable(), isTestOnly());
  }

  /** Convert to string with specified user-friendly and URL-safe flags */
  public String toString(boolean isUserFriendly, boolean isUrlSafe) {
    return toString(isUserFriendly, isUrlSafe, isBounceable(), isTestOnly());
  }

  /** Convert to string with specified user-friendly, URL-safe, and bounceable flags */
  public String toString(boolean isUserFriendly, boolean isUrlSafe, boolean isBounceable) {
    return toString(isUserFriendly, isUrlSafe, isBounceable, isTestOnly());
  }

  /** Convert to bounceable format */
  public String toBounceable() {
    return toString(true, true, true, false);
  }

  /** Convert to bounceable testnet format */
  public String toBounceableTestnet() {
    return toString(true, true, true, true);
  }

  /** Convert to raw format */
  public String toRaw() {
    return toString(false, true, true, false);
  }

  /** Convert to non-bounceable format */
  public String toNonBounceable() {
    return toString(true, true, false, false);
  }

  /** Convert to non-bounceable testnet format */
  public String toNonBounceableTestnet() {
    return toString(true, true, false, true);
  }

  /** Convert to string with all format options specified */
  public String toString(
      boolean isUserFriendly, boolean isUrlSafe, boolean isBounceable, boolean isTestOnly) {

    if (!isUserFriendly) {
      return wc + ":" + Utils.bytesToHex(hashPart);
    } else {
      int tag = isBounceable ? BOUNCEABLE_TAG : NON_BOUNCEABLE_TAG;
      if (isTestOnly) {
        tag |= TEST_FLAG;
      }

      // Preallocate buffers with exact sizes
      byte[] addr = new byte[34];
      addr[0] = (byte) tag;
      addr[1] = wc;
      System.arraycopy(hashPart, 0, addr, 2, 32);

      // Calculate checksum
      byte[] crc16 = Utils.getCRC16ChecksumAsBytes(addr);

      // Create final address with checksum
      byte[] addressWithChecksum = new byte[36];
      System.arraycopy(addr, 0, addressWithChecksum, 0, 34);
      System.arraycopy(crc16, 0, addressWithChecksum, 34, 2);

      // Convert to appropriate base64 format
      return isUrlSafe
          ? Utils.bytesToBase64SafeUrl(addressWithChecksum)
          : Utils.bytesToBase64(addressWithChecksum);
    }
  }

  /**
   * Check if a string is a valid TON address
   *
   * @param address The address string to validate
   * @return true if the address is valid
   */
  public static boolean isValid(String address) {
    try {
      Address.of(address);
      return true;
    } catch (Throwable e) {
      return false;
    }
  }

  /**
   * Parse a user-friendly address format
   *
   * @param addressString The address string to parse
   * @return A new Address instance
   */
  public static Address parseFriendlyAddress(String addressString) {
    if (addressString.length() != 48) {
      throw new Error("User-friendly address should contain strictly 48 characters");
    }

    byte[] data = Utils.base64ToBytes(addressString);
    if (data.length != 36) { // 1byte tag + 1byte workchain + 32 bytes hash + 2 byte crc
      throw new Error("Unknown address type: byte length is not equal to 36");
    }

    // Extract address and checksum parts
    byte[] addr = new byte[34];
    byte[] crc = new byte[2];
    System.arraycopy(data, 0, addr, 0, 34);
    System.arraycopy(data, 34, crc, 0, 2);

    // Validate checksum
    byte[] calculatedCrc16 = Utils.getCRC16ChecksumAsBytes(addr);
    if (!(calculatedCrc16[0] == crc[0] && calculatedCrc16[1] == crc[1])) {
      throw new Error("Wrong crc16 hashsum");
    }

    // Parse tag
    int tag = addr[0] & 0xff;
    boolean isTestOnly = false;
    boolean isBounceable = false;

    if ((tag & TEST_FLAG) != 0) {
      isTestOnly = true;
      tag = (byte) (tag ^ TEST_FLAG);
    }

    if ((tag != BOUNCEABLE_TAG) && (tag != NON_BOUNCEABLE_TAG)) {
      throw new Error("Unknown address tag");
    }

    isBounceable = tag == BOUNCEABLE_TAG;

    // Parse workchain
    byte workchain;
    if ((addr[1] & 0xff) == 0xff) {
      workchain = -1;
    } else {
      workchain = addr[1];
    }

    if (workchain != 0 && workchain != -1) {
      throw new Error("Invalid address wc " + workchain);
    }

    // Extract hash part
    byte[] hashPart = new byte[32];
    System.arraycopy(addr, 2, hashPart, 0, 32);

    // Create and return address
    Address parsedAddress = new Address();
    parsedAddress.wc = workchain;
    parsedAddress.hashPart = hashPart;
    parsedAddress.setFlag(FLAG_TEST_ONLY, isTestOnly);
    parsedAddress.setFlag(FLAG_BOUNCEABLE, isBounceable);
    parsedAddress.setFlag(FLAG_WALLET, !isBounceable);
    parsedAddress.addressType = AddressType.STD_ADDRESS;

    return parsedAddress;
  }

  /**
   * Get the shard as a long value
   *
   * @return The shard as a long
   */
  public long getShardAsLong() {
    // Cache the hash array conversion for multiple calls
    int[] hash = getHash();
    long shardIdxLong = hash[0] >> 4;
    return BigInteger.valueOf(shardIdxLong).shiftLeft(60).longValue();
  }

  /**
   * Get the shard as a BigInteger
   *
   * @return The shard as a BigInteger
   */
  public BigInteger getShardAsBigInt() {
    // Cache the hash array conversion for multiple calls
    int[] hash = getHash();
    long shardIdxLong = hash[0] >> 4;
    return BigInteger.valueOf(shardIdxLong).shiftLeft(60);
  }

  /**
   * Get the hash as an unsigned int array
   *
   * @return The hash as an unsigned int array
   */
  public int[] getHash() {
    return Utils.signedBytesToUnsigned(hashPart);
  }

  /** Implement equals method for proper object comparison */
  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;

    Address other = (Address) obj;
    return wc == other.wc && Arrays.equals(hashPart, other.hashPart);
  }

  /** Implement hashCode for use in collections */
  @Override
  public int hashCode() {
    int result = wc;
    result = 31 * result + Arrays.hashCode(hashPart);
    return result;
  }
}
