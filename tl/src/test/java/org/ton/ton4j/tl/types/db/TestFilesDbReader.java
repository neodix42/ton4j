package org.ton.ton4j.tl.types.db;

import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.ton.ton4j.tl.types.db.block.BlockIdExt;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeSet;

@Slf4j
public class TestFilesDbReader {

  @Test
  public void testReadFilesDbGlobalIndex() throws IOException {
    String dbPath = "/home/neodix/gitProjects/MyLocalTon/myLocalTon/genesis/db/files/globalindex";
    
    log.info("Opening Files database global index: {}", dbPath);
    
    try (RocksDbWrapper globalIndexDb = new RocksDbWrapper(dbPath)) {
      Map<String, String> fileReferences = new HashMap<>();
      TreeSet<String> allKeys = new TreeSet<>();
      TreeSet<String> hexKeys = new TreeSet<>();
      TreeSet<String> tlKeys = new TreeSet<>();
      
      // Collect all keys and analyze them
      globalIndexDb.forEach((key, value) -> {
        String keyStr = new String(key);
        allKeys.add(keyStr);
        
        if (isValidHexString(keyStr)) {
          hexKeys.add(keyStr);
          
          // Try to parse the value to understand the format
          String valueAnalysis = analyzeValue(value);
          fileReferences.put(keyStr, valueAnalysis);
          
        } else {
          tlKeys.add(keyStr);
          
          // Try to parse TL-serialized key
          String tlKeyAnalysis = analyzeTlKey(key);
          String valueAnalysis = analyzeValue(value);
          log.info("TL key: {} -> value: {}", tlKeyAnalysis, valueAnalysis);
        }
      });
      
      log.info("Total keys in Files database: {}", allKeys.size());
      log.info("Hex keys (file hashes): {}", hexKeys.size());
      log.info("TL keys (file references): {}", tlKeys.size());
      
      // Show some sample hex keys and their values
      if (!hexKeys.isEmpty()) {
        log.info("Sample hex keys and their values:");
        int count = 0;
        for (String hexKey : hexKeys) {
          if (count++ < 10) {
            log.info("  {} -> {}", hexKey, fileReferences.get(hexKey));
          }
        }
      }
      
      // Analyze value patterns for hex keys
      if (!fileReferences.isEmpty()) {
        Map<String, Integer> valuePatterns = new HashMap<>();
        for (String analysis : fileReferences.values()) {
          valuePatterns.merge(analysis, 1, Integer::sum);
        }
        
        log.info("Value patterns found:");
        for (Map.Entry<String, Integer> entry : valuePatterns.entrySet()) {
          log.info("  {}: {} occurrences", entry.getKey(), entry.getValue());
        }
      }
      
      // Try to extract package references from TL keys
      log.info("Attempting to extract file references from TL keys...");
      extractFileReferencesFromTlKeys(globalIndexDb);
      
      // Now read actual package files to extract filenames like TestTl.java does
      log.info("Reading actual package files to extract filenames...");
      readPackageFiles();
    }
  }
  
  private void extractFileReferencesFromTlKeys(RocksDbWrapper globalIndexDb) {
    TreeSet<String> fileNames = new TreeSet<>();
    TreeSet<String> packageReferences = new TreeSet<>();
    Map<String, Integer> tlKeyTypes = new HashMap<>();
    
    globalIndexDb.forEach((key, value) -> {
      String keyStr = new String(key);
      if (!isValidHexString(keyStr)) {
        // This is a TL-serialized key
        try {
          // Parse TL key to extract file information
          TlKeyInfo tlKeyInfo = parseTlKey(key);
          if (tlKeyInfo != null) {
            tlKeyTypes.merge(tlKeyInfo.type, 1, Integer::sum);
            
            // Extract file name or hash from the key
            if (tlKeyInfo.fileName != null) {
              fileNames.add(tlKeyInfo.fileName);
            }
            
            // Parse the value to get package location
            String packageInfo = parsePackageValue(value);
            if (packageInfo != null) {
              packageReferences.add(packageInfo);
            }
            
            log.info("TL Key Type: {}, File: {}, Package: {}", 
                tlKeyInfo.type, tlKeyInfo.fileName, packageInfo);
          }
        } catch (Exception e) {
          // Silently skip parsing errors
        }
      }
    });
    
    log.info("Found {} unique file names", fileNames.size());
    log.info("Found {} unique package references", packageReferences.size());
    
    log.info("TL Key types found:");
    for (Map.Entry<String, Integer> entry : tlKeyTypes.entrySet()) {
      log.info("  {}: {} occurrences", entry.getKey(), entry.getValue());
    }
    
    // Show sample file names
    log.info("Sample file names:");
    int count = 0;
    for (String fileName : fileNames) {
      if (count++ < 20) {
        log.info("  {}", fileName);
      }
    }
    
    // Show sample package references
    log.info("Sample package references:");
    count = 0;
    for (String ref : packageReferences) {
      if (count++ < 20) {
        log.info("  {}", ref);
      }
    }
  }
  
  /**
   * Parses a TL-serialized key to extract file information.
   */
  private TlKeyInfo parseTlKey(byte[] key) {
    if (key.length < 4) {
      return null;
    }
    
    try {
      ByteBuffer buffer = ByteBuffer.wrap(key).order(ByteOrder.LITTLE_ENDIAN);
      int magic = buffer.getInt();
      
      String keyType = getTlKeyType(magic);
      String blockInfo = null;
      
      if (keyType.equals("db_filedb_key_blockFile")) {
        // The key format appears to be: magic(4) + workchain(4) + shard(8) + possibly more
        if (buffer.remaining() >= 12) {
          int workchain = buffer.getInt();
          long shard = buffer.getLong();
          
          // Check if there's more data for seqno
          if (buffer.remaining() >= 8) {
            long seqno = buffer.getLong();
            blockInfo = String.format("block(%d,%016x,%d)", workchain, shard, seqno);
          } else {
            blockInfo = String.format("block(%d,%016x,?)", workchain, shard);
          }
          
          log.debug("Parsed BlockFile key: workchain={}, shard={:016x}, remaining={}", 
              workchain, shard, buffer.remaining());
        } else {
          blockInfo = "block(incomplete)";
        }
      } else if (keyType.equals("db_filedb_key_empty")) {
        blockInfo = "empty";
      }
      
      return new TlKeyInfo(keyType, blockInfo);
    } catch (Exception e) {
      log.warn("Error parsing TL key: {}", e.getMessage());
      return null;
    }
  }
  
  /**
   * Gets the TL key type name from magic number.
   */
  private String getTlKeyType(int magic) {
    // These magic numbers need to be calculated from TL schema
    // For now, let's try to identify them from the patterns we see
    switch (magic) {
      case 0x7dc40502:
        return "db_filedb_key_empty";
      case 0xa504033e:
        return "db_filedb_key_blockFile";
      default:
        return "unknown_tl_key_" + String.format("%08x", magic);
    }
  }
  
  /**
   * Parses a package value to extract package location information.
   * According to TL schema: db.filedb.value key:db.filedb.Key prev:int256 next:int256 file_hash:int256
   */
  private String parsePackageValue(byte[] value) {
    if (value == null || value.length == 0) {
      return null;
    }
    
    try {
      // Try to parse as db.filedb.value structure
      if (value.length >= 96) { // 32 + 32 + 32 bytes for prev + next + file_hash
        ByteBuffer buffer = ByteBuffer.wrap(value).order(ByteOrder.LITTLE_ENDIAN);
        
        // Skip the key part (variable length)
        // Read prev (int256 = 32 bytes)
        byte[] prevBytes = new byte[32];
        buffer.get(prevBytes);
        String prev = bytesToHex(prevBytes);
        
        // Read next (int256 = 32 bytes)  
        byte[] nextBytes = new byte[32];
        buffer.get(nextBytes);
        String next = bytesToHex(nextBytes);
        
        // Read file_hash (int256 = 32 bytes)
        byte[] fileHashBytes = new byte[32];
        buffer.get(fileHashBytes);
        String fileHash = bytesToHex(fileHashBytes);
        
        return String.format("FileDbValue(prev=%s..., next=%s..., file_hash=%s...)", 
            prev.substring(0, 8), next.substring(0, 8), fileHash.substring(0, 8));
      }
      
      // Fallback to simpler parsing
      if (value.length >= 16) {
        ByteBuffer buffer = ByteBuffer.wrap(value).order(ByteOrder.LITTLE_ENDIAN);
        long first = buffer.getLong();
        long second = buffer.getLong();
        return String.format("Binary: (%d, %d)", first, second);
      }
      
      return String.format("Raw bytes: len=%d", value.length);
    } catch (Exception e) {
      return "Parse error: " + e.getMessage();
    }
  }
  
  /**
   * Analyzes a TL key and returns a human-readable description.
   */
  private String analyzeTlKey(byte[] key) {
    if (key == null || key.length == 0) {
      return "empty";
    }
    
    StringBuilder analysis = new StringBuilder();
    analysis.append("len=").append(key.length);
    
    // Show first few bytes as hex
    StringBuilder hex = new StringBuilder();
    for (int i = 0; i < Math.min(8, key.length); i++) {
      hex.append(String.format("%02x", key[i] & 0xFF));
    }
    analysis.append(", hex=").append(hex);
    
    // Try to parse as TL key
    if (key.length >= 4) {
      try {
        ByteBuffer buffer = ByteBuffer.wrap(key).order(ByteOrder.LITTLE_ENDIAN);
        int magic = buffer.getInt();
        String keyType = getTlKeyType(magic);
        analysis.append(", type=").append(keyType);
      } catch (Exception e) {
        analysis.append(", tl_parse_error");
      }
    }
    
    return analysis.toString();
  }
  
  /**
   * Information extracted from a TL key.
   */
  private static class TlKeyInfo {
    final String type;
    final String fileName;
    
    TlKeyInfo(String type, String fileName) {
      this.type = type;
      this.fileName = fileName;
    }
  }
  
  private String analyzeValue(byte[] value) {
    if (value == null || value.length == 0) {
      return "empty";
    }
    
    StringBuilder analysis = new StringBuilder();
    analysis.append("len=").append(value.length);
    
    // Try to interpret as string
    String valueStr = new String(value);
    if (isPrintableString(valueStr)) {
      analysis.append(", str='").append(valueStr.length() > 50 ? valueStr.substring(0, 50) + "..." : valueStr).append("'");
    }
    
    // Try to interpret as binary data
    if (value.length >= 4) {
      try {
        ByteBuffer buffer = ByteBuffer.wrap(value).order(ByteOrder.LITTLE_ENDIAN);
        if (value.length >= 8) {
          long longValue = buffer.getLong();
          analysis.append(", long=").append(longValue);
          if (value.length >= 16) {
            buffer.rewind();
            long first = buffer.getLong();
            long second = buffer.getLong();
            analysis.append(", pair=(").append(first).append(",").append(second).append(")");
          }
        } else {
          int intValue = buffer.getInt();
          analysis.append(", int=").append(intValue);
        }
      } catch (Exception e) {
        analysis.append(", binary_parse_error");
      }
    }
    
    // Show first few bytes as hex
    StringBuilder hex = new StringBuilder();
    for (int i = 0; i < Math.min(8, value.length); i++) {
      hex.append(String.format("%02x", value[i] & 0xFF));
    }
    analysis.append(", hex=").append(hex);
    
    return analysis.toString();
  }
  
  private boolean isPrintableString(String s) {
    if (s == null || s.isEmpty()) {
      return false;
    }
    for (char c : s.toCharArray()) {
      if (c < 32 || c > 126) {
        return false;
      }
    }
    return true;
  }
  
  private boolean isValidHexString(String s) {
    if (s == null || s.isEmpty()) {
      return false;
    }
    return s.matches("^[0-9A-Fa-f]+$");
  }
  
  /**
   * Reads package files directly to extract filenames like TestTl.java does.
   */
  private void readPackageFiles() throws IOException {
    // Read some of the orphaned archive packages that don't have .index files
    String[] packagePaths = {
        "/home/neodix/gitProjects/MyLocalTon/myLocalTon/genesis/db/archive/packages/arch0000/archive.00100.pack",
        "/home/neodix/gitProjects/MyLocalTon/myLocalTon/genesis/db/archive/packages/arch0000/archive.00200.pack",
        "/home/neodix/gitProjects/MyLocalTon/myLocalTon/genesis/db/archive/packages/arch0000/archive.00300.pack",
        "/home/neodix/gitProjects/MyLocalTon/myLocalTon/genesis/db/archive/packages/arch0000/archive.00400.pack"
    };
    
    for (String packagePath : packagePaths) {
      try {
        log.info("Reading package file: {}", packagePath);
        readSinglePackageFile(packagePath);
      } catch (Exception e) {
        log.warn("Error reading package {}: {}", packagePath, e.getMessage());
      }
    }
  }
  
  /**
   * Reads a single package file and extracts filenames, following TestTl.java logic.
   */
  private void readSinglePackageFile(String packagePath) throws IOException {
    java.nio.file.Path path = java.nio.file.Paths.get(packagePath);
    if (!java.nio.file.Files.exists(path)) {
      log.warn("Package file does not exist: {}", packagePath);
      return;
    }
    
    byte[] packageData = java.nio.file.Files.readAllBytes(path);
    org.ton.ton4j.cell.ByteReader reader = new org.ton.ton4j.cell.ByteReader(packageData);
    
    // Read package header magic
    int packageHeaderMagic = reader.readIntLittleEndian(); // 32 - 0xae8fdd01
    if (packageHeaderMagic != 0xae8fdd01) {
      log.error("Wrong packageHeaderMagic in {}, should be 0xae8fdd01, got 0x{}", 
          packagePath, Integer.toHexString(packageHeaderMagic));
      return;
    }
    
    log.info("Successfully opened package: {}", packagePath);
    int entryCount = 0;
    
    // Read entries until end of file
    while (reader.getDataSize() > 0) {
      try {
        // Read entry header magic
        short entryHeaderMagic = reader.readShortLittleEndian(); // 16 - 0x1e8b
        if (entryHeaderMagic != 0x1e8b) {
          log.error("Wrong entryHeaderMagic, should be 0x1e8b, got 0x{}", 
              Integer.toHexString(entryHeaderMagic & 0xFFFF));
          break;
        }
        
        // Read filename length and BOC size
        int filenameLength = reader.readShortLittleEndian(); // 16
        int bocSize = reader.readIntLittleEndian(); // 32
        
        // Read filename
        byte[] filenameBytes = org.ton.ton4j.utils.Utils.unsignedBytesToSigned(reader.readBytes(filenameLength));
        String filename = new String(filenameBytes);
        
        // Skip BOC data for now (we're just interested in filenames)
        reader.readBytes(bocSize);
        
        entryCount++;
        log.info("Entry {}: bocSize={}, filename={}", entryCount, bocSize, filename);
        
        // Parse the filename to extract block information
        parseFilename(filename);
        
      } catch (Exception e) {
        log.error("Error reading entry {} from {}: {}", entryCount + 1, packagePath, e.getMessage());
        break;
      }
    }
    
    log.info("Total entries read from {}: {}", packagePath, entryCount);
  }
  
  /**
   * Parses a filename like "block_(-1,8000000000000000,123):hash1:hash2" to extract block info.
   */
  private void parseFilename(String filename) {
    try {
      if (filename.startsWith("block_(") || filename.startsWith("proof_(")) {
        // Extract the part between parentheses
        int startParen = filename.indexOf('(');
        int endParen = filename.indexOf(')', startParen);
        if (startParen != -1 && endParen != -1) {
          String blockInfo = filename.substring(startParen + 1, endParen);
          String[] parts = blockInfo.split(",");
          if (parts.length >= 3) {
            try {
              int workchain = Integer.parseInt(parts[0]);
              String shardStr = parts[1];
              int seqno = Integer.parseInt(parts[2]);
              
              log.info("  Parsed: workchain={}, shard={}, seqno={}", workchain, shardStr, seqno);
              
              // This shows us the actual block ranges in the orphaned packages
              if (seqno >= 100 && seqno <= 461) {
                log.info("  *** FOUND MISSING BLOCK: workchain={}, seqno={} ***", workchain, seqno);
              }
            } catch (NumberFormatException e) {
              log.debug("Could not parse numeric values from: {}", blockInfo);
            }
          }
        }
      }
    } catch (Exception e) {
      log.debug("Error parsing filename {}: {}", filename, e.getMessage());
    }
  }

  /**
   * Converts a byte array to a hexadecimal string.
   */
  private static String bytesToHex(byte[] bytes) {
    StringBuilder hex = new StringBuilder(2 * bytes.length);
    for (byte b : bytes) {
      hex.append(Character.forDigit((b >> 4) & 0xF, 16)).append(Character.forDigit((b & 0xF), 16));
    }
    return hex.toString().toLowerCase();
  }
}
