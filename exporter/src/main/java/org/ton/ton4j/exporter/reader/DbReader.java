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
  //  private final Map<String, RocksDbWrapper> openDbs = new HashMap<>();

  private final GlobalIndexDbReader globalIndexDbReader;
  private final Map<String, ArchiveInfo> packFilesInfo = new HashMap<>();

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

    //    archiveIndexReader = new ArchiveIndexReader(dbRootPath);
    //    log.info("Initialized ArchiveDbReader for TON database at: {}", dbRootPath);
    globalIndexDbReader = new GlobalIndexDbReader(dbRootPath);
    log.info("Initialized GlobalIndexDbReader for TON database at: {}", dbRootPath);

    discoverAllArchivePackagesFromFilesystem(packFilesInfo);
    //    globalIndexDbReader.discoverArchivesFromFilesDatabase(archiveInfos);
  }

  /**
   * Discovers ALL archive packages by directly scanning the filesystem. This is the faster than
   * searching using global index db.
   */
  public void discoverAllArchivePackagesFromFilesystem(Map<String, ArchiveInfo> existingArchives) {
    Path archivePackagesDir = Paths.get(dbRootPath, "archive", "packages");

    try {
      // Scan for archive directories (arch0000, arch0001, etc.)
      Files.list(archivePackagesDir)
          .filter(Files::isDirectory)
          .filter(path -> path.getFileName().toString().startsWith("arch"))
          .forEach(
              archDir -> {
                //                log.debug("Scanning archive directory: {}", archDir);

                try {
                  // Find all .pack files in this archive directory
                  Files.list(archDir)
                      .filter(Files::isRegularFile)
                      .filter(path -> path.getFileName().toString().endsWith(".pack"))
                      .forEach(
                          packFile -> {
                            try {
                              String packFileName = packFile.getFileName().toString();

                              Path parentDir = packFile.getParent();
                              String dirName = parentDir.getFileName().toString();

                              String packageBaseName =
                                  packFileName.substring(0, packFileName.lastIndexOf('.'));
                              String archiveKey = dirName + "/" + packageBaseName;

                              // Extract archive ID from directory name (arch0000 -> 0)
                              int archiveId = 0;
                              if (dirName.startsWith("arch")) {
                                try {
                                  archiveId = Integer.parseInt(dirName.substring(4));
                                } catch (NumberFormatException e) {
                                  log.debug(
                                      "Could not parse archive ID from directory name: {}",
                                      dirName);
                                }
                              }

                              existingArchives.put(
                                  archiveKey,
                                  new ArchiveInfo(
                                      archiveId, packFile.toString(), Files.size(packFile)));

                            } catch (Exception e) {
                              log.debug(
                                  "Error processing archive package file {}: {}",
                                  packFile,
                                  e.getMessage());
                            }
                          });
                } catch (IOException e) {
                  log.debug("Error scanning archive directory {}: {}", archDir, e.getMessage());
                }
              });
    } catch (IOException e) {
      log.error("Error scanning archive packages directory: {}", e.getMessage());
    }

    log.info("Discovered {} total archive packages from filesystem", existingArchives.size());
  }

  /**
   * Gets all archive information.
   *
   * @return Map of archive keys to archive information
   */
  public Map<String, ArchiveInfo> getAllPackFiles() {
    return new HashMap<>(packFilesInfo);
  }

  @Override
  public void close() throws IOException {
    if (globalIndexDbReader != null) {
      globalIndexDbReader.close();
      log.debug("Closed globalIndex database");
    }
  }
}
