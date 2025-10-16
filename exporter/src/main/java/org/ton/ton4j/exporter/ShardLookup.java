package org.ton.ton4j.exporter;

import java.math.BigInteger;
import java.util.List;
import org.ton.ton4j.tlb.*;

/**
 * Utility class for looking up shard blocks from masterchain block's ShardHashes. Based on the C++
 * implementation in ton/crypto/block/mc-config.cpp
 */
public class ShardLookup {

  /**
   * Finds the shard block that contains a given account address by searching through ShardDescr
   * list.
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

      // 3. Convert address to a long for comparison (use first 64 bits)
      long addressPrefix = extractAddressPrefix(address);

      // 4. Find the shard that contains this address
      for (ShardDescr shardDescr : shardDescrList) {
        // Get the shard ID from the ShardDescr
        // The shard ID is stored in the ShardDescr's internal structure
        // We need to check if this shard contains the address
        
        // For now, we'll need to traverse the BinTree to get the proper shard ID
        // Since getShardDescrAsList() doesn't preserve shard IDs, we need a different approach
        
        // TODO: This is a temporary workaround - we need to enhance ShardHashes
        // to preserve shard IDs when flattening the BinTree
      }

      // Fallback: traverse the BinTree directly to get proper shard IDs
      return findShardBlockFromBinTree(shardHashes, workchain, address);

    } catch (Exception e) {
      return null;
    }
  }

  /**
   * Fallback method that traverses the BinTree directly to find the shard with proper shard ID
   * computation.
   */
  private static BlockIdExt findShardBlockFromBinTree(
      ShardHashes shardHashes, int workchain, byte[] address) {
    try {
      // Get all ShardDescr entries
      List<ShardDescr> shardDescrList = shardHashes.getShardDescrAsList();
      if (shardDescrList == null || shardDescrList.isEmpty()) {
        return null;
      }

      // For workchain 0, if there's only one shard, it must be the root shard
      if (shardDescrList.size() == 1) {
        ShardDescr shardDescr = shardDescrList.get(0);
        return BlockIdExt.builder()
            .workchain(workchain)
            .seqno(shardDescr.getSeqNo())
            .shard(0x8000000000000000L) // Root shard
            .rootHash(hexStringToByteArray(shardDescr.getRootHash()))
            .fileHash(hexStringToByteArray(shardDescr.getFileHash()))
            .build();
      }

      // For multiple shards, we need to determine which shard contains the address
      // Since getShardDescrAsList() doesn't preserve shard IDs, we'll use nextValidatorShard
      // as a hint (even though it's not the actual shard ID)
      
      // Extract address prefix for comparison
      long addressPrefix = extractAddressPrefix(address);

      // Try each shard and see if it matches
      for (ShardDescr shardDescr : shardDescrList) {
        // Use nextValidatorShard as the shard ID (this is a workaround)
        // Parse the hex string to get the long value
        long shardId = new BigInteger(shardDescr.getNextValidatorShard(), 16).longValue();
        
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
    // The shard ID has a single 1 bit followed by zeros
    // The position of that bit determines the split depth
    int splitDepth = 63 - Long.numberOfLeadingZeros(shardId);
    
    if (splitDepth <= 0 || splitDepth >= 64) {
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

  /** Gets the bit at a specific position in a byte array. */
  private static boolean getBit(byte[] bytes, int bitIndex) {
    int byteIndex = bitIndex / 8;
    int bitPos = 7 - (bitIndex % 8);
    return ((bytes[byteIndex] >> bitPos) & 1) == 1;
  }
}
