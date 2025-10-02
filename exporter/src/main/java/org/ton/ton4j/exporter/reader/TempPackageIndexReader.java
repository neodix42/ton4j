package org.ton.ton4j.exporter.reader;

import java.io.Closeable;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.ton.ton4j.cell.Cell;
import org.ton.ton4j.exporter.types.BlockId;
import org.ton.ton4j.tlb.Block;
import org.ton.ton4j.tlb.BlockIdExt;
import org.ton.ton4j.utils.Utils;

/**
 * Reader for temp package index databases. Each temp package (e.g., temp.archive.1756843200.pack)
 * has a corresponding RocksDB index (temp.archive.1756843200.index) that contains hash-&gt;offset
 * mappings for fast block lookup.
 *
 * <p>Based on the original C++ implementation in archive-db.cpp, temp package indexes store: - File
 * hash entries: key = file hash (hex string), value = offset (8 bytes, little-endian) - Status
 * entry: key = "status", value = package size (8 bytes, little-endian)
 */
@Slf4j
public class TempPackageIndexReader implements Closeable {

  @Getter private final String indexPath;

  @Getter private final String packagePath;
  private final Integer packageTimestamp;
  private RocksDbWrapper indexDb;

  /**
   * Creates a new TempPackageIndexReader.
   *
   * @param dbPath Path to the database root directory
   * @param packageTimestamp The temp package timestamp
   * @throws IOException If an I/O error occurs
   */
  public TempPackageIndexReader(String dbPath, Integer packageTimestamp) throws IOException {
    this.packageTimestamp = packageTimestamp;
    this.packagePath =
        Paths.get(dbPath, "files", "packages", "temp.archive." + packageTimestamp + ".pack")
            .toString();
    this.indexPath =
        Paths.get(dbPath, "files", "packages", "temp.archive." + packageTimestamp + ".index")
            .toString();

    initializeIndexDatabase();
  }

  /** Initializes the temp package index database. */
  private void initializeIndexDatabase() throws IOException {
    Path indexDbPath = Paths.get(indexPath);

    if (!Files.exists(indexDbPath)) {
      throw new IOException("Temp package index not found at: " + indexDbPath);
    }

    try {
      indexDb = new RocksDbWrapper(indexPath);
      log.debug("Initialized temp package index: {}", indexPath);
    } catch (IOException e) {
      throw new IOException("Could not initialize temp package index: " + e.getMessage(), e);
    }
  }

  /**
   * Gets all hash-&gt;offset mappings from the temp package index (unsorted).
   *
   * @return Map of file hash to offset
   */
  public Map<String, Long> getAllHashOffsetMappings() {
    Map<String, Long> hashOffsetMap = new HashMap<>();

    indexDb.forEach(
        (key, value) -> {
          try {
            String keyStr = new String(key);

            if ("status".equals(keyStr)) {
              return;
            }

            // Skip non-hex keys or wrong length (should be 64 char hex strings)
            if (!isValidHexString(keyStr) || keyStr.length() != 64) {
              return;
            }

            hashOffsetMap.put(keyStr, Long.parseLong(new String(value)));
          } catch (Exception e) {
            log.debug("Error processing temp package index entry: {}", e.getMessage());
          }
        });

    return hashOffsetMap;
  }

  public TreeMap<Long, Long> getAllSortedOffsets() {
    TreeMap<Long, Long> sorterOffsets = new TreeMap<>(Collections.reverseOrder());

    indexDb.forEach(
        (key, value) -> {
          try {
            String keyStr = new String(key);

            // Skip the "status" entry
            if ("status".equals(keyStr)) {
              return;
            }

            // Skip non-hex keys or wrong length (should be 64 char hex strings)
            if (!isValidHexString(keyStr) || keyStr.length() != 64) {
              return;
            }

            sorterOffsets.put(Long.parseLong(new String(value)), 0L);
          } catch (Exception e) {
            log.debug("Error processing temp package index entry: {}", e.getMessage());
          }
        });

    //    log.debug(
    //        "Found {} hash->offset mappings in temp package index {}",
    //        sorterOffsets.size(),
    //        packageTimestamp);
    return sorterOffsets;
  }

  public Map<BlockId, Block> getAllBlocks() throws IOException {
    Map<BlockId, Block> blocks = new HashMap<>();
    Map<Long, Long> mappings = getAllSortedOffsets();

    PackageReader packageReader = new PackageReader(packagePath);

    for (Map.Entry<Long, Long> kv : mappings.entrySet()) {
      //      log.info("{} {}", kv.getKey(), kv.getValue());
      Object entryObj = packageReader.getEntryAt(kv.getKey());
      if (!(entryObj instanceof PackageReader.PackageEntry)) {
        continue;
      }
      PackageReader.PackageEntry packageEntry = (PackageReader.PackageEntry) entryObj;
      if (packageEntry.getFilename().startsWith("block_")) {
        //        log.info("Found block {}", packageEntry.getFilename());
        Block block = packageEntry.getBlock();
        blocks.put(
            BlockId.builder()
                .workchain(block.getBlockInfo().getShard().getWorkchain())
                .shard(block.getBlockInfo().getShard().convertShardIdentToShard().longValue())
                .seqno(block.getBlockInfo().getSeqno())
                .build(),
            block);
      }
    }
    packageReader.close();
    return blocks;
  }

  /** returns last <code>limit</code> blocks, might include blocks of not only master chain */
  public TreeMap<BlockIdExt, Block> getLast(int limit) throws IOException {
    TreeMap<BlockIdExt, Block> blocks =
        new TreeMap<>(Comparator.comparing(BlockIdExt::getSeqno).reversed());
    TreeMap<Long, Long> mappings = getAllSortedOffsets();

    PackageReader packageReader = new PackageReader(packagePath);
    int count = 0;

    for (Map.Entry<Long, Long> kv : mappings.entrySet()) {
      PackageReader.PackageEntry entryObj = packageReader.getEntryAt(kv.getKey());
      if (entryObj == null) {
        continue;
      }
      if (entryObj.getFilename().startsWith("block_")) {
        String rootHash = StringUtils.substringBetween(entryObj.getFilename(), ":", ":");
        String fileHash = StringUtils.substringAfterLast(entryObj.getFilename(), ":");
        Block block = entryObj.getBlock();
        blocks.put(
            BlockIdExt.builder()
                .workchain(block.getBlockInfo().getShard().getWorkchain())
                .shard(block.getBlockInfo().getShard().convertShardIdentToShard().longValue())
                .seqno(block.getBlockInfo().getSeqno())
                .rootHash(Utils.hexToSignedBytes(rootHash))
                .fileHash(Utils.hexToSignedBytes(fileHash))
                .build(),
            block);
        count++;
        if (count >= limit) {
          break;
        }
      }
    }
    packageReader.close();
    return blocks;
  }

  /** returns last master chain block */
  public Pair<BlockIdExt, Block> getLast() throws IOException {

    TreeMap<Long, Long> mappings = getAllSortedOffsets();

    PackageReader packageReader = new PackageReader(packagePath);

    for (Map.Entry<Long, Long> kv : mappings.entrySet()) {
      PackageReader.PackageEntry entryObj = packageReader.getEntryAt(kv.getKey());
      if (entryObj == null) {
        continue;
      }
      if (entryObj.getFilename().startsWith("block_")) {
        String rootHash = StringUtils.substringBetween(entryObj.getFilename(), ":", ":");
        String fileHash = StringUtils.substringAfterLast(entryObj.getFilename(), ":");
        Block block = entryObj.getBlock();

        BlockIdExt blockIdExt =
            BlockIdExt.builder()
                .workchain(block.getBlockInfo().getShard().getWorkchain())
                .shard(block.getBlockInfo().getShard().convertShardIdentToShard().longValue())
                .seqno(block.getBlockInfo().getSeqno())
                .rootHash(Utils.hexToSignedBytes(rootHash))
                .fileHash(Utils.hexToSignedBytes(fileHash))
                .build();

        //        log.info("block {}", block);
        if (block.getBlockInfo().getShard().getWorkchain() == -1) {
          packageReader.close();
          return Pair.of(blockIdExt, block);
        }
      }
    }
    packageReader.close();
    return null;
  }

  /** returns last master chain block as BoC */
  public byte[] getLastAsBoC() throws IOException {

    TreeMap<Long, Long> mappings = getAllSortedOffsets();

    PackageReader packageReader = new PackageReader(packagePath);

    for (Map.Entry<Long, Long> kv : mappings.entrySet()) {
      PackageReader.PackageEntry entryObj = packageReader.getEntryAt(kv.getKey());
      if (entryObj == null) {
        continue;
      }
      if (entryObj.getFilename().startsWith("block_")) {
        Block block = entryObj.getBlock();
        if (block.getBlockInfo().getShard().getWorkchain() == -1) {
          packageReader.close();
          return entryObj.getData();
        }
      }
    }
    packageReader.close();
    return null;
  }

  /**
   * @return Block as cell and Block as TL-B
   */
  public Cell getLastAsCell() throws IOException {

    TreeMap<Long, Long> mappings = getAllSortedOffsets();

    PackageReader packageReader = new PackageReader(packagePath);

    for (Map.Entry<Long, Long> kv : mappings.entrySet()) {
      PackageReader.PackageEntry entryObj = packageReader.getEntryAt(kv.getKey());
      if (entryObj == null) {
        continue;
      }
      if (entryObj.getFilename().startsWith("block_")) {
        return entryObj.getCell();
      }
    }
    packageReader.close();
    return null;
  }

  /**
   * Gets the offset for a specific file hash.
   *
   * @param hash The file hash (hex string)
   * @return The offset, or null if not found
   */
  public Long getOffsetForHash(String hash) {
    try {
      byte[] offsetBytes = indexDb.get(hash.getBytes());
      if (offsetBytes != null && offsetBytes.length >= 8) {
        ByteBuffer buffer = ByteBuffer.wrap(offsetBytes).order(ByteOrder.LITTLE_ENDIAN);
        return buffer.getLong();
      }
    } catch (Exception e) {
      log.debug("Error getting offset for hash {}: {}", hash, e.getMessage());
    }
    return null;
  }

  /**
   * Checks if a string is a valid hexadecimal string.
   *
   * @param s The string to check
   * @return True if the string is a valid hexadecimal string, false otherwise
   */
  private static boolean isValidHexString(String s) {
    if (s == null || s.isEmpty()) {
      return false;
    }
    return s.matches("^[0-9A-Fa-f]+$");
  }

  @Override
  public void close() throws IOException {
    if (indexDb != null) {
      indexDb.close();
    }
  }
}
