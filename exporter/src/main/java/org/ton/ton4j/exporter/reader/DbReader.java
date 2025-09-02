package org.ton.ton4j.exporter.reader;

import java.io.Closeable;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;

/**
 * Main entry point for reading TON RocksDB files. This class provides access to various specialized
 * readers for different TON database types.
 */
@Slf4j
public class DbReader implements Closeable {

  private final String dbRootPath;
  private final Map<String, RocksDbWrapper> openDbs = new HashMap<>();
  private ArchiveDbReader archiveDbReader;

  /**
   * Creates a new DbReader.
   *
   * @param dbRootPath Path to the TON database root directory
   * @throws IOException If an I/O error occurs
   */
  public DbReader(String dbRootPath) throws IOException {
    this.dbRootPath = dbRootPath;

    // Validate the path
    Path path = Paths.get(dbRootPath);
    if (!Files.exists(path)) {
      throw new IOException("Database root path does not exist: " + dbRootPath);
    }

    if (!Files.isDirectory(path)) {
      throw new IOException("Database root path is not a directory: " + dbRootPath);
    }

    log.info("Initialized DbReader for TON database at: {}", dbRootPath);
  }

  /**
   * Gets the archive database reader.
   *
   * @return The archive database reader
   * @throws IOException If an I/O error occurs
   */
  public ArchiveDbReader getArchiveDbReader() throws IOException {
    if (archiveDbReader == null) {
      archiveDbReader = new ArchiveDbReader(dbRootPath);
    }

    return archiveDbReader;
  }

  /**
   * Opens a RocksDB database.
   *
   * @param name The name of the database (e.g., "celldb", "files", "adnl")
   * @return The RocksDB wrapper
   * @throws IOException If an I/O error occurs
   */
  public RocksDbWrapper openDb(String name) throws IOException {
    if (!openDbs.containsKey(name)) {
      String dbPath = Paths.get(dbRootPath, name).toString();
      openDbs.put(name, new RocksDbWrapper(dbPath));
    }

    return openDbs.get(name);
  }

  @Override
  public void close() throws IOException {
    // Close all open databases
    for (RocksDbWrapper db : openDbs.values()) {
      db.close();
    }

    // Close the archive database reader
    if (archiveDbReader != null) {
      archiveDbReader.close();
    }
  }
}
