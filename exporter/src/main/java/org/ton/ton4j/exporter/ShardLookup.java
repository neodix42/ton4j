package org.ton.ton4j.exporter;

import java.util.List;
import org.ton.ton4j.tlb.*;

/**
 * Utility class for looking up shard blocks from masterchain block's ShardHashes. Based on the C++
 * implementation in ton/crypto/block/mc-config.cpp
 */
public class ShardLookup {

  /**
   * Finds the shard block that contains a given account address.
   *
   * @param mcBlock The masterchain block
   * @param workchain The account's workchain
   * @param address The account's 256-bit address
   * @return BlockIdExt containing the shard block details, or null if not found
   */
  public static BlockIdExt findShardBlock(Block mcBlock, int workchain, byte[] address) {
    try {
      // 1. Parse masterchain block to get ShardHashes
      BlockExtra blockExtra = mcBlock.getExtra();

      if (blockExtra.getMcBlockExtra() == null) {
        return null;
      }

      McBlockExtra mcExtra = blockExtra.getMcBlockExtra();
      ShardHashes shardHashes = mcExtra.getShardHashes();

      if (shardHashes == null || shardHashes.getShardHashes() == null) {
        return null;
      }

      // 2. Get all ShardDescr entries with their computed shard IDs
      List<ShardDescr> shardDescrList = shardHashes.getShardDescrAsList();
      if (shardDescrList == null || shardDescrList.isEmpty()) {
        return null;
      }

      // 3. Extract address prefix for comparison (first 64 bits)
      long addressPrefix = extractAddressPrefix(address);

      // 4. Find the shard that contains this address
      for (ShardDescr shardDescr : shardDescrList) {
        long shardId = shardDescr.getComputedShardId();
        
        // Check if this shard contains the address
        if (shardContainsAddress(shardId, addressPrefix)) {
          return BlockIdExt.builder()
              .workchain(workchain)
              .seqno(shardDescr.getSeqNo())
              .shard(shardId)
              .rootHash(hexStringToByteArray(shardDescr.getRootHash()))
              .fileHash(hexStringToByteArray(shardDescr.getFileHash()))
              .build();
        }
      }

      return null;

    } catch (Exception e) {
      return null;
    }
  }

  /**
   * Checks if a shard contains a given address based on shard ID.
   * The shard ID encodes which portion of the address space it covers.
   */
  private static boolean shardContainsAddress(long shardId, long addressPrefix) {
    // Root shard contains all addresses
    if (shardId == 0x8000000000000000L) {
      return true;
    }

    // Calculate the split depth from the shard ID
    // The shard ID has a single 1 bit followed by zeros (after the high bit)
    // Count leading zeros after removing the high bit to find split depth
    int splitDepth = 63 - Long.numberOfLeadingZeros(shardId & 0x7FFFFFFFFFFFFFFFL);
    
    if (splitDepth <= 0) {
      return true; // Root shard
    }

    // Create a mask for the significant bits
    long mask = (-1L) << (64 - splitDepth);
    
    // Check if the address prefix matches the shard prefix
    return (addressPrefix & mask) == (shardId & mask);
  }

  /**
   * Extracts the first 64 bits from a 256-bit address as a long.
   */
  private static long extractAddressPrefix(byte[] address) {
    long result = 0;
    for (int i = 0; i < 8 && i < address.length; i++) {
      result = (result << 8) | (address[i] & 0xFF);
    }
    return result;
  }

  /**
   * Converts a hex string to a byte array.
   */
  private static byte[] hexStringToByteArray(String hex) {
    // Pad with leading zero if odd length
    if (hex.length() % 2 != 0) {
      hex = "0" + hex;
    }
    int len = hex.length();
    byte[] data = new byte[len / 2];
    for (int i = 0; i < len; i += 2) {
      data[i / 2] =
          (byte)
              ((Character.digit(hex.charAt(i), 16) << 4) + Character.digit(hex.charAt(i + 1), 16));
    }
    return data;
  }
}
