package org.ton.ton4j.exporter.reader;

import java.io.Closeable;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.ton.ton4j.tl.types.db.blockdb.BlockDbValue;

/**
 * Reader for individual archive index databases (archive.XXXXX.index). Each archive package has a
 * corresponding RocksDB index that contains hash-&gt;offset mappings for files within that package.
 * Addtionally contains keys: "status.", slices, slice_size, "info."
 *
 * <p>Based on the C++ implementation in ArchiveFile class.
 */
@Slf4j
@Data
public class ArchiveIndexReader implements Closeable {

  private final String archiveIndexPath;
  private RocksDbWrapper indexDb;

  /**
   * Creates a new ArchiveIndexReader for a specific archive index database.
   *
   * @param archiveIndexPath Path to the archive.XXXXX.index directory
   * @throws IOException If the index database cannot be opened
   */
  public ArchiveIndexReader(String archiveIndexPath) throws IOException {
    this.archiveIndexPath = archiveIndexPath;

    if (!Files.exists(Paths.get(archiveIndexPath))) {
      throw new IOException("Archive index database not found at: " + archiveIndexPath);
    }

    try {
      indexDb = new RocksDbWrapper(archiveIndexPath);
    } catch (IOException e) {
      throw new IOException("Could not open archive index database: " + e.getMessage(), e);
    }
  }

  /**
   * Creates a new ArchiveIndexReader for a specific archive index database.
   *
   * @param tonDbRootDir Path to the TON DB ROOT DIR
   * @param index of index folder
   * @throws IOException If the index database cannot be opened
   */
  public ArchiveIndexReader(String tonDbRootDir, int index) throws IOException {
    this.archiveIndexPath = ArchiveIndexReader.getArchiveIndexPath(tonDbRootDir, index);

    if (!Files.exists(Paths.get(archiveIndexPath))) {
      throw new IOException("Archive index database not found at: " + archiveIndexPath);
    }

    try {
      indexDb = new RocksDbWrapper(archiveIndexPath);
    } catch (IOException e) {
      throw new IOException("Could not open archive index database: " + e.getMessage(), e);
    }
  }

  public List<String> getAllPackFiles() throws IOException {
    List<String> result = new ArrayList<>();
    byte[] sliced = indexDb.get("status".getBytes());
    if ("sliced".equals(new String(sliced))) {
      int numberOfSlices = Integer.parseInt(new String(indexDb.get("slices".getBytes())));
      //      int sliceSize = Integer.parseInt(new String(indexDb.get("slice_size".getBytes())));
      for (int i = 0; i < numberOfSlices; i++) {
        String rawPack = new String(indexDb.get(("info." + i).getBytes())) + ".pack";
        rawPack = rawPack.replace(".-1:8000000000000000", "");
        if (rawPack.contains(":")) {
          rawPack = "archive." + StringUtils.leftPad(rawPack, 30, "0");
        } else {
          rawPack = "archive." + StringUtils.leftPad(rawPack, 10, "0");
        }
        result.add(Path.of(archiveIndexPath).getParent().resolve(rawPack).toString());
      }
    }

    return result;
  }

  public static String getArchiveIndexPath(String tonRootDbPath, int archiveIndex) {
    String archFolder = String.format("arch%04d", (archiveIndex / 100000));
    String indexStr = String.format("%05d", archiveIndex);
    String absoluteArchFolder =
        Path.of(tonRootDbPath, "archive", "packages", archFolder).toString();
    return Paths.get(absoluteArchFolder, "archive." + indexStr + ".index").toString();
  }

  public String getExactPackFilename(
      int packageId, long seqno, int wc, long shard, long masterChainSeqno) throws IOException {
    long baseSeqno = (wc != -1) ? masterChainSeqno : seqno;
    long sliceSeqno = (baseSeqno - (baseSeqno - packageId) % 100);
    //    int sliceSeqno = (seqno - (seqno % 20000));
    byte[] sliced = indexDb.get("status".getBytes());
    if ("sliced".equals(new String(sliced))) {
      int numberOfSlices = Integer.parseInt(new String(indexDb.get("slices".getBytes())));
      for (int i = 0; i < numberOfSlices; i++) {
        String packFilename = new String(indexDb.get(("info." + i).getBytes())) + ".pack";
        //        System.out.println(packFilename);
        String searchString = String.format("%d.%d:%16x.pack", sliceSeqno, wc, shard);
        if (packFilename.equals(searchString)) {

          packFilename = packFilename.replace(".-1:8000000000000000", "");
          if (packFilename.contains(":")) {
            packFilename = "archive." + StringUtils.leftPad(packFilename, 30, "0");
          } else {
            packFilename = "archive." + StringUtils.leftPad(packFilename, 10, "0");
          }

          return Path.of(archiveIndexPath).getParent().resolve(packFilename).toString();
        }
      }
    }
    return null;
  }

  /**
   * Gets all hash-&gt;offset mappings from this archive index database. This follows the C++
   * implementation where each archive index contains file hashes as keys and offsets as values.
   *
   * @return Map of file hash to offset within the package file
   */
  public Map<String, Long> getAllHashOffsetMappings() {
    Map<String, Long> hashOffsetMap = new HashMap<>();

    AtomicInteger totalEntries = new AtomicInteger(0);
    AtomicInteger validMappings = new AtomicInteger(0);
    AtomicInteger parseErrors = new AtomicInteger(0);

    indexDb.forEach(
        (key, value) -> {
          try {
            totalEntries.incrementAndGet();

            String keyStr = new String(key);
            String valueStr = new String(value);

            // Skip special keys like "status"
            if (keyStr.startsWith("status")) {
              return;
            }

            // Validate that key looks like a hash (hex string)
            if (isValidHash(keyStr)) {

              // Parse offset from value (stored as string in C++ implementation)
              try {
                long offset = Long.parseLong(valueStr);
                hashOffsetMap.put(keyStr, offset);
                validMappings.incrementAndGet();
              } catch (NumberFormatException e) {
                parseErrors.incrementAndGet();
                log.debug("Error parsing offset for hash {}: {}", keyStr, e.getMessage());
              }
            }

          } catch (Exception e) {
            parseErrors.incrementAndGet();
            log.debug("Error processing archive index entry: {}", e.getMessage());
          }
        });

    return hashOffsetMap;
  }

  public long getOffsetByHash(String hash) throws IOException {
    return Long.parseLong(new String(indexDb.get(hash.getBytes())));
  }

  public BlockDbValue getDbValueByHash(String hash) throws IOException {
    return BlockDbValue.deserialize(ByteBuffer.wrap(indexDb.get(hash.getBytes())));
  }

  public BlockDbValue getDbValueByHash(byte[] hash) throws IOException {
    return BlockDbValue.deserialize(ByteBuffer.wrap(indexDb.get(hash)));
  }

  /**
   * Gets the number of file entries in this archive index.
   *
   * @return Number of hash-&gt;offset mappings
   */
  public int getFileCount() {
    return getAllHashOffsetMappings().size();
  }

  /**
   * Checks if a string is a valid hexadecimal string.
   *
   * @param s The string to check
   * @return True if the string is a valid hexadecimal string, false otherwise
   */
  private static boolean isValidHash(String s) {
    if (s == null || s.isEmpty()) {
      return false;
    }
    if (s.length() != 64) {
      return false;
    }
    return s.matches("^[0-9A-Fa-f]+$");
  }

  public static boolean isPrintableAscii(String s) {
    return s.chars().allMatch(c -> c >= 32 && c <= 126);
  }

  @Override
  public void close() throws IOException {
    if (indexDb != null) {
      indexDb.close();
    }
  }
}
