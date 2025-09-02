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

  /** Functional interface for abstracting output writing operations */
  @FunctionalInterface
  private interface OutputWriter {
    void writeLine(String line);
  }

  /**
   * usually located in /var/ton-work/db on server or myLocalTon/genesis/db in MyLocalTon app.
   * Specify absolute path
   */
  private String tonDatabaseRootPath;

  /** whether to show blocks' reading progress every second, default false */
  private Boolean showProgress;

  private static DbReader dbReader;
  private StatusManager statusManager;

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

      Exporter exporter = super.build();
      exporter.statusManager = new StatusManager();
      return exporter;
    }
  }

  public String getDatabasePath() {
    return tonDatabaseRootPath;
  }

  /**
   * Common export logic that handles database reading and block processing with state persistence
   *
   * @param outputWriter strategy for writing output lines
   * @param deserialized if true - deserialized Block TL-B object will be saved as json string,
   *     otherwise boc in hex format will be stored in a single line
   * @param parallelThreads number of parallel threads used to export a database
   * @param showProgressInfo whether to show progress information during export
   * @param exportStatus the export status for tracking progress
   * @return array containing [parsedBlocksCounter, nonBlocksCounter, totalProcessed]
   */
  private int[] exportDataWithStatus(
      OutputWriter outputWriter,
      boolean deserialized,
      int parallelThreads,
      boolean showProgressInfo,
      ExportStatus exportStatus)
      throws IOException {
    Gson gson = new GsonBuilder().setObjectToNumberStrategy(ToNumberPolicy.LONG_OR_DOUBLE).create();

    dbReader = new DbReader(tonDatabaseRootPath);

    AtomicInteger parsedBlocksCounter = new AtomicInteger(exportStatus.getParsedBlocksCount());
    AtomicInteger nonBlocksCounter = new AtomicInteger(exportStatus.getNonBlocksCount());
    AtomicInteger packsProcessed = new AtomicInteger(exportStatus.getProcessedCount());
    long totalPacks = dbReader.getArchiveDbReader().getArchiveInfos().size();

    // Update total packages if it has changed
    if (exportStatus.getTotalPackages() != totalPacks) {
      exportStatus.setTotalPackages(totalPacks);
      statusManager.saveStatus(exportStatus);
    }

    long startTime = System.currentTimeMillis();

    // Create thread pool
    ExecutorService executor = Executors.newFixedThreadPool(parallelThreads);
    List<Future<Void>> futures = new ArrayList<>();

    // Submit tasks for each archive that hasn't been processed yet
    for (Map.Entry<String, ArchiveInfo> entry :
        dbReader.getArchiveDbReader().getArchiveInfos().entrySet()) {
      String archiveKey = entry.getKey();
      ArchiveInfo archiveInfo = entry.getValue();

      // Skip already processed packages
      if (exportStatus.isPackageProcessed(archiveKey)) {
        if (showProgressInfo) {
          log.debug("Skipping already processed archive: {}", archiveKey);
        }
        continue;
      }

      Future<Void> future =
          executor.submit(
              () -> {
                try {
                  Map<String, byte[]> localBlocks = new HashMap<>();
                  int localParsedBlocks = 0;
                  int localNonBlocks = 0;

                  if (archiveInfo.getIndexPath() == null) {
                    dbReader
                        .getArchiveDbReader()
                        .readFromFilesPackage(archiveKey, archiveInfo, localBlocks);
                  } else {
                    dbReader
                        .getArchiveDbReader()
                        .readFromTraditionalArchive(archiveKey, archiveInfo, localBlocks);
                  }

                  for (Map.Entry<String, byte[]> kv : localBlocks.entrySet()) {
                    try {
                      Cell c = CellBuilder.beginCell().fromBoc(kv.getValue()).endCell();

                      long magic = c.getBits().preReadUint(32).longValue();
                      if (magic == 0x11ef55aaL) {

                        String lineToWrite;

                        if (deserialized) {
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

                        outputWriter.writeLine(lineToWrite);
                        parsedBlocksCounter.getAndIncrement();
                        localParsedBlocks++;
                      } else {
                        nonBlocksCounter.getAndIncrement();
                        localNonBlocks++;
                      }

                    } catch (Throwable e) {
                      log.debug("Error parsing block {}: {}", entry.getKey(), e.getMessage());
                      // Continue processing other blocks instead of failing completely
                    }
                  }

                  // Mark package as processed and save status
                  synchronized (exportStatus) {
                    exportStatus.markPackageProcessed(
                        archiveKey, localParsedBlocks, localNonBlocks);
                    statusManager.saveStatus(exportStatus);
                  }

                  if (showProgressInfo) {
                    System.out.println(
                        String.format(
                            "Completed reading archive %s: entries %d, progress: %.1f%% (%d/%d)",
                            archiveKey,
                            localBlocks.size(),
                            exportStatus.getProgressPercentage(),
                            exportStatus.getProcessedCount(),
                            exportStatus.getTotalPackages()));
                  } else {
                    log.debug(
                        "Completed reading archive {}: {} entries", archiveKey, localBlocks.size());
                  }
                } catch (IOException e) {
                  log.warn("Error reading blocks from archive {}: {}", archiveKey, e.getMessage());
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

    long endTime = System.currentTimeMillis();
    long durationMs = endTime - startTime;
    double durationSeconds = durationMs / 1000.0;
    double blocksPerSecond = parsedBlocksCounter.get() / durationSeconds;

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

    // Mark export as completed
    exportStatus.markCompleted();
    statusManager.saveStatus(exportStatus);

    log.info(
        "Total duration: {}s, speed: {} blocks per second",
        durationSeconds,
        String.format("%.2f", blocksPerSecond));

    dbReader.close();

    return new int[] {parsedBlocksCounter.get(), nonBlocksCounter.get()};
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

    // Check for existing status and resume if possible
    ExportStatus exportStatus = statusManager.loadStatus();
    boolean isResume = false;

    if (exportStatus != null && !exportStatus.isCompleted()) {
      // Validate that the resume parameters match
      if ("file".equals(exportStatus.getExportType())
          && outputToFile.equals(exportStatus.getOutputFile())
          && deserialized == exportStatus.isDeserialized()
          && parallelThreads == exportStatus.getParallelThreads()) {

        log.info(
            "Resuming export from previous session. Progress: {}% ({}/{})",
            exportStatus.getProgressPercentage(),
            exportStatus.getProcessedCount(),
            exportStatus.getTotalPackages());
        isResume = true;
      } else {
        log.warn("Export parameters don't match existing status. Starting fresh export.");
        statusManager.deleteStatus();
        exportStatus = null;
      }
    }

    // Create new status if not resuming
    if (exportStatus == null) {
      // Get total packages count
      dbReader = new DbReader(tonDatabaseRootPath);
      long totalPackages = dbReader.getArchiveDbReader().getArchiveInfos().size();
      dbReader.close();

      exportStatus =
          statusManager.createNewStatus(
              totalPackages, "file", outputToFile, deserialized, parallelThreads);
      statusManager.saveStatus(exportStatus);
      log.info("Starting new export to file: {}", outputToFile);
    }

    File file = new File(outputToFile);

    // Create a synchronized PrintWriter for thread-safe immediate file writing
    // Use append mode if resuming
    try (PrintWriter writer =
        new PrintWriter(new FileWriter(file, StandardCharsets.UTF_8, isResume))) {

      // Create file-based output writer with thread-safe synchronization
      OutputWriter fileWriter =
          line -> {
            synchronized (writer) {
              writer.println(line);
              writer.flush(); // Ensure immediate write to disk
            }
          };

      // Use common export logic with progress info enabled for file export
      int[] results =
          exportDataWithStatus(fileWriter, deserialized, parallelThreads, true, exportStatus);
      int parsedBlocksCounter = results[0];
      int nonBlocksCounter = results[1];

      log.info(
          "Exported {} blocks (nonBlocks {}) to file: {}",
          parsedBlocksCounter,
          nonBlocksCounter,
          outputToFile);

      // Clean up status file after successful completion
      statusManager.deleteStatus();
    }
  }

  /**
   * @param deserialized if true - deserialized Block TL-B object will be saved as json string,
   *     otherwise boc in hex format will be stored in a single line
   * @param parallelThreads number of parallel threads used to export a database
   */
  public void exportToStdout(boolean deserialized, int parallelThreads) throws IOException {
    // Check for existing status and resume if possible
    ExportStatus exportStatus = statusManager.loadStatus();
    boolean isResume = false;

    if (exportStatus != null && !exportStatus.isCompleted()) {
      // Validate that the resume parameters match
      if ("stdout".equals(exportStatus.getExportType())
          && deserialized == exportStatus.isDeserialized()
          && parallelThreads == exportStatus.getParallelThreads()) {

        log.info(
            "Resuming stdout export from previous session. Progress: {}% ({}/{})",
            exportStatus.getProgressPercentage(),
            exportStatus.getProcessedCount(),
            exportStatus.getTotalPackages());
        isResume = true;
      } else {
        log.warn("Export parameters don't match existing status. Starting fresh export.");
        statusManager.deleteStatus();
        exportStatus = null;
      }
    }

    // Create new status if not resuming
    if (exportStatus == null) {
      // Get total packages count
      dbReader = new DbReader(tonDatabaseRootPath);
      long totalPackages = dbReader.getArchiveDbReader().getArchiveInfos().size();
      dbReader.close();

      exportStatus =
          statusManager.createNewStatus(
              totalPackages, "stdout", null, deserialized, parallelThreads);
      statusManager.saveStatus(exportStatus);
      log.info("Starting new export to stdout");
    }

    // Disable logging for stdout export to avoid interference with output
    LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
    for (Logger logger : loggerContext.getLoggerList()) {
      logger.setLevel(Level.OFF);
    }

    // Create stdout-based output writer (System.out is already thread-safe)
    OutputWriter stdoutWriter = System.out::println;

    // Use common export logic with progress info disabled for stdout export
    int[] results =
        exportDataWithStatus(stdoutWriter, deserialized, parallelThreads, false, exportStatus);
    int parsedBlocksCounter = results[0];
    int nonBlocksCounter = results[1];

    for (Logger logger : loggerContext.getLoggerList()) {
      logger.setLevel(Level.INFO);
    }

    log.info("Exported {} blocks (nonBlocks {}) to stdout", parsedBlocksCounter, nonBlocksCounter);

    // Clean up status file after successful completion
    statusManager.deleteStatus();
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
