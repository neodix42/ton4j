package org.ton.ton4j.exporter.reader;

import java.io.Closeable;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import lombok.Getter;
import org.ton.ton4j.cell.Cell;
import org.ton.ton4j.cell.CellBuilder;
import org.ton.ton4j.cell.CellSlice;
import org.ton.ton4j.tlb.Block;

/** Reader for TON package files. */
public class PackageReader implements PackageReaderInterface {

  private static final int PACKAGE_HEADER_MAGIC = 0xae8fdd01;
  private static final short ENTRY_HEADER_MAGIC = 0x1e8b;

  private final RandomAccessFile file;
  private long currentPosition;

  /**
   * Creates a new PackageReader.
   *
   * @param path Path to the package file
   * @throws IOException If an I/O error occurs
   */
  public PackageReader(String path) throws IOException {
    file = new RandomAccessFile(path, "r");

    // Verify package header magic
    int magic = readInt();
    if (magic != PACKAGE_HEADER_MAGIC) {
      throw new IOException(
          "Invalid package header magic: 0x"
              + Integer.toHexString(magic)
              + ", expected: 0x"
              + Integer.toHexString(PACKAGE_HEADER_MAGIC));
    }

    currentPosition = 4; // After header
  }

  /**
   * Reads the next entry in the package.
   *
   * @return The next entry, or null if the end of the file is reached
   * @throws IOException If an I/O error occurs
   */
  public PackageEntry readNextEntry() throws IOException {
    if (currentPosition >= file.length()) {
      return null;
    }

    file.seek(currentPosition);

    // Read entry header (8 bytes total)
    // First 4 bytes: entry_header_magic (lower 16 bits) + filename_size (upper 16 bits)
    int header0 = readInt();
    int entryMagic = header0 & 0xFFFF;
    int filenameLength = (header0 >>> 16) & 0xFFFF;

    if (entryMagic != ENTRY_HEADER_MAGIC) {
      throw new IOException(
          "Invalid entry header magic: 0x"
              + Integer.toHexString(entryMagic)
              + ", expected: 0x"
              + Integer.toHexString(ENTRY_HEADER_MAGIC));
    }

    // Next 4 bytes: data_size
    int dataSize = readInt();

    // Read filename
    byte[] filenameBytes = new byte[filenameLength];
    file.readFully(filenameBytes);
    String filename = new String(filenameBytes);

    // Read data
    byte[] data = new byte[dataSize];
    file.readFully(data);

    // Update position for next read
    currentPosition = file.getFilePointer();

    return new PackageEntry(filename, data);
  }

  /**
   * Gets an entry at a specific offset.
   *
   * @param offset The offset in the file
   * @return The entry
   * @throws IOException If an I/O error occurs
   */
  @Override
  public Object getEntryAt(long offset) throws IOException {
    if (offset < 0) {
      throw new IOException("Negative seek offset: " + offset);
    }

    long oldPosition = currentPosition;
    try {
      currentPosition = offset + 4; // Skip package header

      // Check if the position is valid
      if (currentPosition >= file.length()) {
        throw new IOException("Offset beyond file size: " + offset);
      }

      return readNextEntry();
    } catch (IOException e) {
      throw new IOException("Error reading entry at offset " + offset + ": " + e.getMessage(), e);
    } finally {
      currentPosition = oldPosition;
    }
  }

  /**
   * Iterates through all entries in the package.
   *
   * @param consumer Consumer for entries
   * @throws IOException If an I/O error occurs
   */
  @Override
  public void forEach(Consumer<Object> consumer) throws IOException {
    currentPosition = 4; // Reset to start (after header)

    PackageEntry entry;
    while ((entry = readNextEntry()) != null) {
      consumer.accept(entry);
    }
  }

  /**
   * Iterates through all entries in the package with typed consumer.
   *
   * @param consumer Consumer for entries
   * @throws IOException If an I/O error occurs
   */
  public void forEachTyped(Consumer<PackageEntry> consumer) throws IOException {
    currentPosition = 4; // Reset to start (after header)

    PackageEntry entry;
    while ((entry = readNextEntry()) != null) {
      consumer.accept(entry);
    }
  }

  /**
   * Reads all entries from the package and returns them as a Map.
   *
   * @return Map containing all entries with filename as key and data as value
   * @throws IOException If an I/O error occurs
   */
  @Override
  public Map<String, byte[]> readAllEntries() throws IOException {
    Map<String, byte[]> result = new HashMap<>();

    forEachTyped(
        entry -> {
          result.put(entry.getFilename(), entry.getData());
        });

    return result;
  }

  private int readInt() throws IOException {
    byte[] bytes = new byte[4];
    file.readFully(bytes);
    return ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).getInt();
  }

  private short readShort() throws IOException {
    byte[] bytes = new byte[2];
    file.readFully(bytes);
    return ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).getShort();
  }

  @Override
  public void close() throws IOException {
    file.close();
  }

  /** Represents an entry in a package file. */
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
      //      System.out.println(Utils.bytesToHex(data));
      return Block.deserialize(
          CellSlice.beginParse(CellBuilder.beginCell().fromBoc(data).endCell()));
    }
  }
}
