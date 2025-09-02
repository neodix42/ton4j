package org.ton.ton4j.exporter;

import static java.util.Objects.isNull;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.ToNumberPolicy;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.LoggerFactory;
import org.ton.ton4j.cell.Cell;
import org.ton.ton4j.cell.CellBuilder;
import org.ton.ton4j.cell.CellSlice;
import org.ton.ton4j.exporter.reader.ArchiveInfo;
import org.ton.ton4j.exporter.reader.DbReader;
import org.ton.ton4j.tlb.Block;
import org.ton.ton4j.utils.Utils;

@Builder
@Slf4j
public class Exporter {
  /**
   * usually located in /var/ton-work/db on server or myLocalTon/genesis/db in MyLocalTon app.
   * Specify absolute path
   */
  private String tonDatabaseRootPath;

  /** whether to show blocks' reading progress every second, default false */
  private Boolean showProgress;

  private static DbReader dbReader;

  public static class ExporterBuilder {}

  public static ExporterBuilder builder() {
    return new CustomExporterBuilder();
  }

  private static class CustomExporterBuilder extends ExporterBuilder {

    @Override
    public Exporter build() {
      if (isNull(super.tonDatabaseRootPath)) {
        throw new Error("tonDatabaseRootPath is null");
      }

      if (isNull(super.showProgress)) {
        super.showProgress = false;
      }

      return super.build();
    }
  }

  public String getDatabasePath() {
    return tonDatabaseRootPath;
  }

  /**
   * @param outputToFile path to file where result will be stored
   * @param deserialized if true - deserialized Block TL-B object will be saved as json string,
   *     otherwise boc in hex format will be stored in a single line
   * @param parallelThreads number of parallel threads used to export a database
   */
  public void exportToFile(String outputToFile, boolean deserialized, int parallelThreads)
      throws IOException {
    if (StringUtils.isEmpty(outputToFile)) {
      throw new Error("outputToFile is empty");
    }

    Gson gson = new GsonBuilder().setObjectToNumberStrategy(ToNumberPolicy.LONG_OR_DOUBLE).create();

    File file = new File(outputToFile);

    dbReader = new DbReader(tonDatabaseRootPath);

    AtomicInteger globalCounter = new AtomicInteger(0);
    // Create a synchronized PrintWriter for thread-safe immediate file writing
    try (PrintWriter writer = new PrintWriter(new FileWriter(file, StandardCharsets.UTF_8))) {

      // Create thread pool
      ExecutorService executor = Executors.newFixedThreadPool(parallelThreads);
      List<Future<Void>> futures = new ArrayList<>();

      // Submit tasks for each archive
      for (Map.Entry<String, ArchiveInfo> entry :
          dbReader.getArchiveDbReader().getArchiveInfos().entrySet()) {
        String archiveKey = entry.getKey();
        ArchiveInfo archiveInfo = entry.getValue();

        Future<Void> future =
            executor.submit(
                () -> {
                  try {
                    // Create a local map for this thread's results
                    Map<String, byte[]> localBlocks = new HashMap<>();

                    // Check if this is a Files database package (indicated by null indexPath)
                    if (archiveInfo.getIndexPath() == null) {
                      // This is a Files database package, handle it separately
                      dbReader
                          .getArchiveDbReader()
                          .readFromFilesPackage(archiveKey, archiveInfo, localBlocks);
                    } else {
                      // This is a traditional archive package with its own index
                      dbReader
                          .getArchiveDbReader()
                          .readFromTraditionalArchive(archiveKey, archiveInfo, localBlocks);
                    }
                    //                    log.debug("archiveKey {}, localBlocks {}", archiveKey,
                    // localBlocks.size());

                    for (Map.Entry<String, byte[]> kv : localBlocks.entrySet()) {
                      try {
                        String piece = Utils.bytesToHex(Utils.slice(kv.getValue(), 0, 64));
                        //                      log.info("Exporting block: {}", piece);

                        if (piece.contains("11ef55aa")) { // block
                          String lineToWrite;

                          if (deserialized) {
                            Cell c = CellBuilder.beginCell().fromBoc(kv.getValue()).endCell();
                            //                        long magic =
                            // c.getBits().preReadUint(32).longValue();
                            //                        if (magic == 0x11ef55aaL) { // block
                            //                          log.info("block");
                            //                        }
                            // writing deserialized boc in json format
                            Block block = Block.deserialize(CellSlice.beginParse(c));
                            lineToWrite =
                                String.format(
                                    "%s,%s,%s,%s",
                                    block.getBlockInfo().getShard().getWorkchain(),
                                    block
                                        .getBlockInfo()
                                        .getShard()
                                        .convertShardIdentToShard()
                                        .toString(16),
                                    block.getBlockInfo().getSeqno(),
                                    gson.toJson(block));
                          } else { // write boc in hex format
                            lineToWrite = Utils.bytesToHex(kv.getValue());
                          }

                          // Thread-safe immediate writing to file
                          if (lineToWrite != null) {
                            synchronized (writer) {
                              writer.println(lineToWrite);
                              writer.flush(); // Ensure immediate write to disk
                            }
                          }
                        }
                        globalCounter.getAndIncrement();
                      } catch (Throwable e) {
                        log.debug("Error parsing block {}: {}", entry.getKey(), e.getMessage());
                        // Continue processing other blocks instead of failing completely
                      }
                    }

                    log.debug(
                        "Completed reading archive {}: {} entries", archiveKey, localBlocks.size());
                  } catch (IOException e) {
                    log.warn(
                        "Error reading blocks from archive {}: {}", archiveKey, e.getMessage());
                  } catch (Exception e) {
                    log.error(
                        "Unexpected error reading archive {}: {}", archiveKey, e.getMessage());
                  }
                  return null;
                });

        futures.add(future);
      }

      // Wait for all tasks to complete
      for (Future<Void> future : futures) {
        try {
          future.get();
        } catch (Exception e) {
          log.error("Error waiting for archive reading task: {}", e.getMessage());
        }
      }

      // Shutdown executor
      executor.shutdown();
      try {
        if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
          executor.shutdownNow();
        }
      } catch (InterruptedException e) {
        executor.shutdownNow();
        Thread.currentThread().interrupt();
      }

      log.info("Exported {} blocks to file: {}", globalCounter.get(), outputToFile);
    }

    dbReader.close();
  }

  /**
   * @param deserialized if true - deserialized Block TL-B object will be saved as json string,
   *     otherwise boc in hex format will be stored in a single line
   * @param parallelThreads number of parallel threads used to export a database
   */
  public void exportToStdout(boolean deserialized, int parallelThreads) throws IOException {
    LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
    for (Logger logger : loggerContext.getLoggerList()) {
      logger.setLevel(Level.OFF);
    }

    Gson gson = new GsonBuilder().setObjectToNumberStrategy(ToNumberPolicy.LONG_OR_DOUBLE).create();

    dbReader = new DbReader(tonDatabaseRootPath);

    AtomicInteger globalCounter = new AtomicInteger(0);

    // Create thread pool
    ExecutorService executor = Executors.newFixedThreadPool(parallelThreads);
    List<Future<Void>> futures = new ArrayList<>();

    // Submit tasks for each archive
    for (Map.Entry<String, ArchiveInfo> entry :
        dbReader.getArchiveDbReader().getArchiveInfos().entrySet()) {
      String archiveKey = entry.getKey();
      ArchiveInfo archiveInfo = entry.getValue();

      Future<Void> future =
          executor.submit(
              () -> {
                try {
                  // Create a local map for this thread's results
                  Map<String, byte[]> localBlocks = new HashMap<>();

                  // Check if this is a Files database package (indicated by null indexPath)
                  if (archiveInfo.getIndexPath() == null) {
                    // This is a Files database package, handle it separately
                    dbReader
                        .getArchiveDbReader()
                        .readFromFilesPackage(archiveKey, archiveInfo, localBlocks);
                  } else {
                    // This is a traditional archive package with its own index
                    dbReader
                        .getArchiveDbReader()
                        .readFromTraditionalArchive(archiveKey, archiveInfo, localBlocks);
                  }
                  //                    log.debug("archiveKey {}, localBlocks {}", archiveKey,
                  // localBlocks.size());

                  for (Map.Entry<String, byte[]> kv : localBlocks.entrySet()) {
                    try {
                      String piece = Utils.bytesToHex(Utils.slice(kv.getValue(), 0, 64));
                      //                      log.info("Exporting block: {}", piece);

                      if (piece.contains("11ef55aa")) { // block
                        String lineToWrite;

                        if (deserialized) {
                          Cell c = CellBuilder.beginCell().fromBoc(kv.getValue()).endCell();
                          Block block = Block.deserialize(CellSlice.beginParse(c));
                          lineToWrite =
                              String.format(
                                  "%s,%s,%s,%s",
                                  block.getBlockInfo().getShard().getWorkchain(),
                                  block
                                      .getBlockInfo()
                                      .getShard()
                                      .convertShardIdentToShard()
                                      .toString(16),
                                  block.getBlockInfo().getSeqno(),
                                  gson.toJson(block));
                        } else { // write boc in hex format
                          lineToWrite = Utils.bytesToHex(kv.getValue());
                        }

                        // system.out is thread safe already
                        System.out.println(lineToWrite);
                      }
                      globalCounter.getAndIncrement();
                    } catch (Throwable e) {
                      log.warn("Error parsing block {}: {}", entry.getKey(), e.getMessage());
                    }

                    log.debug(
                        "Completed reading archive {}: {} entries", archiveKey, localBlocks.size());
                  }
                } catch (Exception e) {
                  log.error("Unexpected error reading archive {}: {}", archiveKey, e.getMessage());
                }
                return null;
              });

      futures.add(future);
    }

    // Wait for all tasks to complete
    for (Future<Void> future : futures) {
      try {
        future.get();
      } catch (Exception e) {
        log.error("Error waiting for archive reading task: {}", e.getMessage());
      }
    }

    // Shutdown executor
    executor.shutdown();
    try {
      if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
        executor.shutdownNow();
      }
    } catch (InterruptedException e) {
      executor.shutdownNow();
      Thread.currentThread().interrupt();
    }

    log.info("Exported {} blocks to stdout", globalCounter.get());

    dbReader.close();
  }

  public void printADbStats() throws IOException {
    dbReader = new DbReader(tonDatabaseRootPath);

    for (Map.Entry<String, ArchiveInfo> s :
        dbReader.getArchiveDbReader().getArchiveInfos().entrySet()) {
      log.info("Archive {}: {}", s.getKey(), s.getValue());
    }
    log.info(
        "total archive packs found: {}", dbReader.getArchiveDbReader().getArchiveInfos().size());
  }
}
