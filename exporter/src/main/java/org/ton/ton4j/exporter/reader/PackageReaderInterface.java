package org.ton.ton4j.exporter.reader;

import java.io.Closeable;
import java.io.IOException;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Common interface for package readers to enable polymorphic usage.
 * This allows switching between different implementations (e.g., RandomAccessFile vs BufferedReader)
 * without changing the client code.
 */
public interface PackageReaderInterface extends Closeable {

  /**
   * Gets an entry at a specific offset.
   *
   * @param offset The offset in the file
   * @return The entry (implementation-specific type)
   * @throws IOException If an I/O error occurs
   */
  Object getEntryAt(long offset) throws IOException;

  /**
   * Iterates through all entries in the package.
   *
   * @param consumer Consumer for entries (implementation-specific type)
   * @throws IOException If an I/O error occurs
   */
  void forEach(Consumer<Object> consumer) throws IOException;

  /**
   * Reads all entries from the package and returns them as a Map.
   *
   * @return Map containing all entries with filename as key and data as value
   * @throws IOException If an I/O error occurs
   */
  Map<String, byte[]> readAllEntries() throws IOException;
}
