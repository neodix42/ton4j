package org.ton.ton4j.exporter.reader;

import java.io.Closeable;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.ton.ton4j.exporter.types.ArchiveInfo;

/** Specialized reader for TON archive database. */
@Slf4j
@Data
public class ArchiveDbReader implements Closeable {

  private final String rootPath;
  String dbPath;
  private final Map<String, PackageReaderInterface> packageReaders = new HashMap<>();

  public ArchiveDbReader(String rootPath) {

    this.rootPath = Paths.get(rootPath, "archive").toString();
    this.dbPath = Paths.get(rootPath).toString();
  }

  /**
   * Discovers ALL archive packages by directly scanning the filesystem. This is the faster than
   * searching using global index db.
   */
  public void discoverAllArchivePackagesFromFilesystem(Map<String, ArchiveInfo> existingArchives) {
    Path archivePackagesDir = Paths.get(rootPath, "packages");

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
                              //                              String indexFileName =
                              // packFileName.replace(".pack", ".index");
                              //                              Path indexPath =
                              // archDir.resolve(indexFileName);

                              // Extract package info from the file path
                              Path parentDir = packFile.getParent();
                              String dirName = parentDir.getFileName().toString();

                              // Remove .pack extension to get the package base name
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

                              //                              String indexPathStr =
                              //                                  Files.exists(indexPath) ?
                              // indexPath.toString() : null;

                              existingArchives.put(
                                  archiveKey,
                                  new ArchiveInfo(
                                      archiveId,
                                      //                                      indexPathStr,
                                      packFile.toString(),
                                      Files.size(packFile)));

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

  @Override
  public void close() throws IOException {

    // Close all package readers
    for (PackageReaderInterface reader : packageReaders.values()) {
      if (reader != null) {
        try {
          reader.close();
        } catch (IOException e) {
          log.warn("Error closing package reader: {}", e.getMessage());
        }
      }
    }
  }
}
