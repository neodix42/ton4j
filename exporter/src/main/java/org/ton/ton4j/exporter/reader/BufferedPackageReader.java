package org.ton.ton4j.exporter.reader;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.ton.ton4j.cell.Cell;
import org.ton.ton4j.cell.CellBuilder;
import org.ton.ton4j.cell.CellSlice;
import org.ton.ton4j.tlb.Block;

/**
 * High-performance buffered reader for TON package files. Optimized for sequential buffer reading
 * to eliminate random I/O seeks.
 *
 * <p>Key optimizations: - Loads entire file into memory buffer (suitable for files ≤5MB) -
 * Pre-parses all entry positions for O(1) access - Eliminates RandomAccessFile seeks and multiple
 * system calls - Uses direct ByteBuffer for efficient memory access
 */
@Slf4j
public class BufferedPackageReader implements PackageReaderInterface {

  private static final int PACKAGE_HEADER_MAGIC = 0xae8fdd01;
  private static final short ENTRY_HEADER_MAGIC = 0x1e8b;

  private final String filePath;
  private ByteBuffer buffer;
  private final Map<Long, EntryInfo> entryIndex = new HashMap<>();
  private final List<EntryInfo> entryList = new ArrayList<>();
  private boolean initialized = false;

  /** Entry information for fast lookup */
  @Getter
  private static class EntryInfo {
    private final long offset;
    private final String filename;
    private final int dataSize;
    private final int dataOffset;

    public EntryInfo(long offset, String filename, int dataSize, int dataOffset) {
      this.offset = offset;
      this.filename = filename;
      this.dataSize = dataSize;
      this.dataOffset = dataOffset;
    }
  }

  /**
   * Creates a new BufferedPackageReader.
   *
   * @param path Path to the package file
   * @throws IOException If an I/O error occurs
   */
  public BufferedPackageReader(String path) throws IOException {
    this.filePath = path;
    // Lazy initialization - only load when first accessed
  }

  /**
   * Initialize the buffer and parse all entries. This is called lazily on first access to optimize
   * memory usage.
   */
  private synchronized void initialize() throws IOException {
    if (initialized) {
      return;
    }

    Path path = Paths.get(filePath);
    if (!Files.exists(path)) {
      throw new IOException("Package file does not exist: " + filePath);
    }

    long fileSize = Files.size(path);
    if (fileSize > Integer.MAX_VALUE) {
      throw new IOException("Package file too large for buffer: " + fileSize + " bytes");
    }

    // Load entire file into direct ByteBuffer for optimal performance
    byte[] fileData = Files.readAllBytes(path);
    buffer = ByteBuffer.allocateDirect(fileData.length);
    buffer.put(fileData);
    buffer.flip();
    buffer.order(ByteOrder.LITTLE_ENDIAN);

    // Verify package header magic
    int magic = buffer.getInt(0);
    if (magic != PACKAGE_HEADER_MAGIC) {
      throw new IOException(
          "Invalid package header magic: 0x"
              + Integer.toHexString(magic)
              + ", expected: 0x"
              + Integer.toHexString(PACKAGE_HEADER_MAGIC));
    }

    // Pre-parse all entries for fast access
    parseAllEntries();
    initialized = true;

    //    log.debug(
    //        "Initialized BufferedPackageReader for {}: {} entries, {} bytes",
    //        filePath,
    //        entryList.size(),
    //        fileSize);
  }

  /**
   * Pre-parse all entries in the package file and build index. This eliminates the need for seeks
   * during actual data access.
   */
  private void parseAllEntries() {
    int position = 4; // Skip package header
    int entryCount = 0;

    while (position < buffer.limit()) {
      try {
        // Read entry header (8 bytes total)
        if (position + 8 > buffer.limit()) {
          break; // Not enough data for header
        }

        int header0 = buffer.getInt(position);
        int entryMagic = header0 & 0xFFFF;
        int filenameLength = (header0 >>> 16) & 0xFFFF;

        if (entryMagic != ENTRY_HEADER_MAGIC) {
          log.warn(
              "Invalid entry header magic at position {}: 0x{}, skipping rest of file",
              position,
              Integer.toHexString(entryMagic));
          break;
        }

        int dataSize = buffer.getInt(position + 4);

        // Validate entry size
        int totalEntrySize = 8 + filenameLength + dataSize;
        if (position + totalEntrySize > buffer.limit()) {
          log.warn("Entry at position {} extends beyond file boundary, skipping", position);
          break;
        }

        // Read filename
        byte[] filenameBytes = new byte[filenameLength];
        buffer.position(position + 8);
        buffer.get(filenameBytes);
        String filename = new String(filenameBytes);

        // Calculate data offset
        int dataOffset = position + 8 + filenameLength;

        // Create entry info
        EntryInfo entryInfo = new EntryInfo(position, filename, dataSize, dataOffset);
        entryIndex.put((long) position, entryInfo);
        entryList.add(entryInfo);

        // Move to next entry
        position += totalEntrySize;
        entryCount++;

      } catch (Exception e) {
        log.warn(
            "Error parsing entry at position {}: {}, stopping parsing", position, e.getMessage());
        break;
      }
    }

    //    log.debug("Parsed {} entries from package file", entryCount);
  }

  /**
   * Gets an entry at a specific offset. This is now O(1) lookup from pre-built index instead of
   * file seek.
   *
   * @param offset The offset in the file
   * @return The entry
   * @throws IOException If an I/O error occurs
   */
  @Override
  public Object getEntryAt(long offset) throws IOException {
    if (!initialized) {
      initialize();
    }

    if (offset < 0) {
      throw new IOException("Negative seek offset: " + offset);
    }

    // Adjust offset to account for package header
    long adjustedOffset = offset + 4;

    EntryInfo entryInfo = entryIndex.get(adjustedOffset);
    if (entryInfo == null) {
      throw new IOException("No entry found at offset: " + offset);
    }

    // Read data directly from buffer
    byte[] data = new byte[entryInfo.getDataSize()];
    buffer.position(entryInfo.getDataOffset());
    buffer.get(data);

    return new PackageEntry(entryInfo.getFilename(), data);
  }

  /**
   * Iterates through all entries in the package. Interface implementation using Object consumer.
   *
   * @param consumer Consumer for entries
   * @throws IOException If an I/O error occurs
   */
  @Override
  public void forEach(Consumer<Object> consumer) throws IOException {
    if (!initialized) {
      initialize();
    }

    for (EntryInfo entryInfo : entryList) {
      try {
        // Read data directly from buffer
        byte[] data = new byte[entryInfo.getDataSize()];
        buffer.position(entryInfo.getDataOffset());
        buffer.get(data);

        PackageEntry entry = new PackageEntry(entryInfo.getFilename(), data);
        consumer.accept(entry);
      } catch (Exception e) {
        log.warn("Error processing entry {}: {}", entryInfo.getFilename(), e.getMessage());
      }
    }
  }

  /**
   * Iterates through all entries in the package with typed consumer. Now uses pre-parsed entry list
   * for optimal performance.
   *
   * @param consumer Consumer for entries
   * @throws IOException If an I/O error occurs
   */
  public void forEachTyped(Consumer<PackageEntry> consumer) throws IOException {
    if (!initialized) {
      initialize();
    }

    for (EntryInfo entryInfo : entryList) {
      try {
        // Read data directly from buffer
        byte[] data = new byte[entryInfo.getDataSize()];
        buffer.position(entryInfo.getDataOffset());
        buffer.get(data);

        PackageEntry entry = new PackageEntry(entryInfo.getFilename(), data);
        consumer.accept(entry);
      } catch (Exception e) {
        log.warn("Error processing entry {}: {}", entryInfo.getFilename(), e.getMessage());
      }
    }
  }

  /**
   * Reads all entries from the package and returns them as a Map. Optimized version using
   * pre-parsed entries.
   *
   * @return Map containing all entries with filename as key and data as value
   * @throws IOException If an I/O error occurs
   */
  @Override
  public Map<String, byte[]> readAllEntries() throws IOException {
    Map<String, byte[]> result = new HashMap<>(entryList.size());

    forEachTyped(entry -> result.put(entry.getFilename(), entry.getData()));

    return result;
  }

  /**
   * Gets the number of entries in this package. Available after initialization.
   *
   * @return Number of entries
   * @throws IOException If an I/O error occurs
   */
  public int getEntryCount() throws IOException {
    if (!initialized) {
      initialize();
    }
    return entryList.size();
  }

  /**
   * Gets the file size in bytes.
   *
   * @return File size in bytes
   * @throws IOException If an I/O error occurs
   */
  public long getFileSize() throws IOException {
    if (!initialized) {
      initialize();
    }
    return buffer.limit();
  }

  @Override
  public void close() throws IOException {
    if (buffer != null) {
      // For direct ByteBuffer, we should clean it up explicitly
      // Note: This uses sun.misc.Cleaner which is internal API
      // In Java 9+, consider using sun.misc.Unsafe or other cleanup methods
      try {
        if (buffer.isDirect()) {
          // Best effort cleanup - JVM will eventually clean it up
          buffer = null;
          System.gc(); // Suggest GC to clean up direct buffer
        }
      } catch (Exception e) {
        log.debug("Could not explicitly clean direct buffer: {}", e.getMessage());
      }
    }
    entryIndex.clear();
    entryList.clear();
    initialized = false;
  }

  /**
   * Represents an entry in a package file. Same interface as original PackageReader.PackageEntry
   * for compatibility.
   */
  @Getter
  public static class PackageEntry {
    private final String filename;
    private final byte[] data; // boc

    public PackageEntry(String filename, byte[] data) {
      this.filename = filename;
      this.data = data;
    }

    public Cell getCell() {
      return CellBuilder.beginCell().fromBoc(data).endCell();
    }

    public Block getBlock() {
      return Block.deserialize(
          CellSlice.beginParse(CellBuilder.beginCell().fromBoc(data).endCell()));
    }
  }
}
