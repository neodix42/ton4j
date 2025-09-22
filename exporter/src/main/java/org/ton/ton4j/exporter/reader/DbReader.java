package org.ton.ton4j.exporter.reader;

import java.io.Closeable;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.ton.ton4j.exporter.types.ArchiveInfo;

/**
 * Main entry point for reading TON RocksDB files. This class provides access to various specialized
 * readers for different TON database types.
 */
@Slf4j
@Getter
public class DbReader implements Closeable {

  private final String dbRootPath;
  private final Map<String, RocksDbWrapper> openDbs = new HashMap<>();
  private final ArchiveDbReader archiveDbReader;
  private final GlobalIndexDbReader globalIndexDbReader;
  private final Map<String, ArchiveInfo> archiveInfos = new HashMap<>();

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

    archiveDbReader = new ArchiveDbReader(dbRootPath);
    log.info("Initialized ArchiveDbReader for TON database at: {}", dbRootPath);
    globalIndexDbReader = new GlobalIndexDbReader(dbRootPath);
    log.info("Initialized GlobalIndexDbReader for TON database at: {}", dbRootPath);

    archiveDbReader.discoverAllArchivePackagesFromFilesystem(archiveInfos);
    globalIndexDbReader.discoverArchivesFromFilesDatabase(archiveInfos);
  }

  /**
   * Gets all archive information.
   *
   * @return Map of archive keys to archive information
   */
  public Map<String, ArchiveInfo> getArchiveInfos() {
    return new HashMap<>(archiveInfos);
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
