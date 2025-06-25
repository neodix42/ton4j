package org.ton.ton4j.tl.types.db;

import java.io.Closeable;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;
import org.ton.ton4j.tl.types.db.block.BlockInfo;

/**
 * Main entry point for reading TON RocksDB files. This class provides access to various specialized
 * readers for different TON database types.
 */
public class DbReader implements Closeable {
  private static final Logger log = Logger.getLogger(DbReader.class.getName());

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

    log.info("Initialized DbReader for TON database at: " + dbRootPath);
  }

  /**
   * Gets the archive database reader.
   *
   * @return The archive database reader
   * @throws IOException If an I/O error occurs
   */
  public ArchiveDbReader getArchiveDbReader() throws IOException {
    if (archiveDbReader == null) {
      String archivePath = Paths.get(dbRootPath, "archive").toString();
      archiveDbReader = new ArchiveDbReader(archivePath);
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

  /**
   * Gets a block by its hash from the archive database.
   *
   * @param hash The block hash
   * @return The block data, or null if not found
   * @throws IOException If an I/O error occurs
   */
  public byte[] getBlock(String hash) throws IOException {
    return getArchiveDbReader().readBlock(hash);
  }

  /**
   * Gets a block info by its hash from the archive database.
   *
   * @param hash The block hash
   * @return The block info, or null if not found
   * @throws IOException If an I/O error occurs
   */
  public BlockInfo getBlockInfo(String hash) throws IOException {
    return getArchiveDbReader().readBlockInfo(hash);
  }

  /**
   * Gets all blocks from the archive database.
   *
   * @return Map of block hash to block data
   * @throws IOException If an I/O error occurs
   */
  public Map<String, byte[]> getAllBlocks() throws IOException {
    return getArchiveDbReader().getAllBlocks();
  }

  /**
   * Gets all block infos from the archive database.
   *
   * @return Map of block hash to block info
   * @throws IOException If an I/O error occurs
   */
  public Map<String, BlockInfo> getAllBlockInfos() throws IOException {
    return getArchiveDbReader().getAllBlockInfos();
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
