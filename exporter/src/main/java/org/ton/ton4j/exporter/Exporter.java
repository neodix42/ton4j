package org.ton.ton4j.exporter;

import static java.util.Objects.isNull;
import static org.ton.ton4j.exporter.reader.CellDbReader.parseCell;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.ToNumberPolicy;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.LoggerFactory;
import org.ton.ton4j.address.Address;
import org.ton.ton4j.bitstring.BitString;
import org.ton.ton4j.cell.*;
import org.ton.ton4j.exporter.lazy.CellSliceLazy;
import org.ton.ton4j.exporter.lazy.ShardAccountLazy;
import org.ton.ton4j.exporter.lazy.ShardStateUnsplitLazy;
import org.ton.ton4j.exporter.reader.*;
import org.ton.ton4j.exporter.types.*;
import org.ton.ton4j.tl.types.db.block.BlockIdExt;
import org.ton.ton4j.tl.types.db.block.BlockInfo;
import org.ton.ton4j.tl.types.db.blockdb.key.BlockDbValueKey;
import org.ton.ton4j.tl.types.db.celldb.CellDbValue;
import org.ton.ton4j.tl.types.db.filedb.key.BlockFileKey;
import org.ton.ton4j.tl.types.db.files.index.IndexValue;
import org.ton.ton4j.tl.types.db.lt.desc.DbLtDescKey;
import org.ton.ton4j.tlb.Block;
import org.ton.ton4j.tlb.BlockId;
import org.ton.ton4j.tlb.adapters.*;
import org.ton.ton4j.utils.Utils;

@Builder
@Slf4j
public class Exporter {

  // Optimized static GSON instance - single shared instance for all threads
  // No thread-local instances needed since GSON is thread-safe for serialization
  public static final Gson gson =
      new GsonBuilder()
          .setObjectToNumberStrategy(ToNumberPolicy.LONG_OR_DOUBLE)
          .registerTypeAdapter(Cell.class, new CellTypeAdapter())
          .registerTypeAdapter(BitString.class, new BitStringTypeAdapter())
          .registerTypeAdapter(byte[].class, new ByteArrayToHexTypeAdapter())
          //          .registerTypeAdapter(TonHashMapAug.class, new TonHashMapAugTypeAdapter())
          //          .registerTypeAdapter(TonHashMapAugE.class, new TonHashMapAugETypeAdapter())
          //          .registerTypeAdapter(TonHashMap.class, new TonHashMapTypeAdapter())
          //          .registerTypeAdapter(TonHashMapE.class, new TonHashMapETypeAdapter())
          .disableHtmlEscaping()
          //          .setLenient()
          .create();

  /** Functional interface for abstracting output writing operations */
  @FunctionalInterface
  private interface OutputWriter {
    void writeLine(String line);
  }

  // Volatile reference to current executor services for shutdown coordination
  private volatile ExecutorService currentProcessingExecutor;
  private volatile ExecutorService currentWriterExecutor;
  private volatile ScheduledExecutorService currentRateDisplayExecutor;
  // Shutdown signal to stop processing new packages
  private volatile boolean shutdownRequested;

  // Statistics tracking for interrupted exports
  volatile AtomicInteger totalParsedBlocks;
  volatile AtomicInteger totalNonBlocks;
  volatile AtomicInteger totalErrors;

  /**
   * usually located in /var/ton-work/db on server or myLocalTon/genesis/db in MyLocalTon app.
   * Specify absolute path
   */
  private String tonDatabaseRootPath;

  /** whether to show blocks' reading progress every second, default false */
  private Boolean showProgress;

  private static DbReader dbReader;

  // No thread-local GSON needed - static GSON is thread-safe for serialization
  // Removed ThreadLocal to eliminate potential memory leaks and state accumulation

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
      try {
        dbReader = new DbReader(super.tonDatabaseRootPath);
      } catch (IOException e) {
        throw new RuntimeException(e);
      }

      if (isNull(super.showProgress)) {
        super.showProgress = false;
      }

      Exporter exporter = super.build();

      // Initialize statistics tracking fields
      if (exporter.totalParsedBlocks == null) {
        exporter.totalParsedBlocks = new AtomicInteger(0);
      }
      if (exporter.totalNonBlocks == null) {
        exporter.totalNonBlocks = new AtomicInteger(0);
      }
      if (exporter.totalErrors == null) {
        exporter.totalErrors = new AtomicInteger(0);
      }

      // No reset mechanisms needed - all caching removed

      return exporter;
    }
  }

  public String getDatabasePath() {
    return tonDatabaseRootPath;
  }

  /**
   * Gets the current count of successfully parsed blocks. This method is thread-safe and can be
   * called during export interruption.
   *
   * @return the number of successfully parsed blocks
   */
  public int getParsedBlocksCount() {
    return totalParsedBlocks.get();
  }

  /**
   * Gets the current count of non-block entries processed. This method is thread-safe and can be
   * called during export interruption.
   *
   * @return the number of non-block entries processed
   */
  public int getNonBlocksCount() {
    return totalNonBlocks.get();
  }

  /**
   * Gets the current count of blocks that failed to parse. This method is thread-safe and can be
   * called during export interruption.
   *
   * @return the number of blocks that failed to parse
   */
  public int getErrorsCount() {
    return totalErrors.get();
  }

  /**
   * Gets the total count of all processed entries (blocks + non-blocks + errors). This method is
   * thread-safe and can be called during export interruption.
   *
   * @return the total number of processed entries
   */
  public int getTotalProcessedCount() {
    return totalParsedBlocks.get() + totalNonBlocks.get() + totalErrors.get();
  }

  /**
   * Calculates the success rate as a percentage of successfully parsed blocks out of total block
   * entries (excluding non-blocks). This method is thread-safe and can be called during export
   * interruption.
   *
   * @return the success rate as a percentage (0.0 to 100.0)
   */
  public double getSuccessRate() {
    int totalBlocks = totalParsedBlocks.get() + totalErrors.get();
    return totalBlocks > 0 ? (double) totalParsedBlocks.get() / totalBlocks * 100.0 : 0.0;
  }

  /**
   * Calculates the error rate as a percentage of failed blocks out of total block entries
   * (excluding non-blocks). This method is thread-safe and can be called during export
   * interruption.
   *
   * @return the error rate as a percentage (0.0 to 100.0)
   */
  public double getErrorRate() {
    int totalBlocks = totalParsedBlocks.get() + totalErrors.get();
    return totalBlocks > 0 ? (double) totalErrors.get() / totalBlocks * 100.0 : 0.0;
  }

  /**
   * Signals shutdown and waits for all currently running executor services to finish. This method
   * is called by the shutdown hook to ensure clean termination.
   */
  public void waitForThreadsToFinish() {
    // Signal shutdown to stop processing new packages
    shutdownRequested = true;
    log.info("Shutdown signal sent to all worker threads...");

    // MassiveBlockQueue removed - no longer needed
    // if (blockQueue != null) {
    //   blockQueue.shutdown(optimalWriterThreads);
    // }
    // Shutdown processing executor
    ExecutorService processingExecutor = currentProcessingExecutor;
    if (processingExecutor != null && !processingExecutor.isShutdown()) {
      log.info("Waiting for processing threads to finish current packages...");
      processingExecutor.shutdown();
      try {
        if (!processingExecutor.awaitTermination(15, TimeUnit.SECONDS)) {
          log.warn("Processing threads did not finish within 15 seconds, forcing shutdown...");
          processingExecutor.shutdownNow();
          if (!processingExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
            log.error("Processing threads did not respond to forced shutdown");
          }
        } else {
          log.info("All processing threads finished successfully");
        }
      } catch (InterruptedException e) {
        log.warn("Interrupted while waiting for processing threads to finish");
        processingExecutor.shutdownNow();
        Thread.currentThread().interrupt();
      }
    }

    // Shutdown writer executor
    ExecutorService writerExecutor = currentWriterExecutor;
    if (writerExecutor != null && !writerExecutor.isShutdown()) {
      log.info("Waiting for writer threads to finish...");
      writerExecutor.shutdown();
      try {
        if (!writerExecutor.awaitTermination(30, TimeUnit.SECONDS)) {
          log.warn("Writer threads did not finish within 30 seconds, forcing shutdown...");
          writerExecutor.shutdownNow();
          if (!writerExecutor.awaitTermination(10, TimeUnit.SECONDS)) {
            log.error("Writer threads did not respond to forced shutdown");
          }
        } else {
          log.info("All writer threads finished successfully");
        }
      } catch (InterruptedException e) {
        log.warn("Interrupted while waiting for writer threads to finish");
        writerExecutor.shutdownNow();
        Thread.currentThread().interrupt();
      }
    }

    // Shutdown rate display executor
    ScheduledExecutorService rateExecutor = currentRateDisplayExecutor;
    if (rateExecutor != null && !rateExecutor.isShutdown()) {
      //      log.debug("Shutting down rate display executor...");
      rateExecutor.shutdown();
      try {
        if (!rateExecutor.awaitTermination(2, TimeUnit.SECONDS)) {
          rateExecutor.shutdownNow();
        }
      } catch (InterruptedException e) {
        rateExecutor.shutdownNow();
        Thread.currentThread().interrupt();
      }
    }
  }

  /**
   * Formats duration in seconds to a human-readable string (e.g., "2h 30m 45s")
   *
   * @param seconds the duration in seconds
   * @return formatted duration string
   */
  private static String formatDuration(long seconds) {
    if (seconds < 0) {
      return "N/A";
    }

    long hours = seconds / 3600;
    long minutes = (seconds % 3600) / 60;
    long secs = seconds % 60;

    if (hours > 0) {
      return String.format("%dh %dm %ds", hours, minutes, secs);
    } else if (minutes > 0) {
      return String.format("%dm %ds", minutes, secs);
    } else {
      return String.format("%ds", secs);
    }
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
   * @param errorFilePath path to the errors.txt file where error block data will be written
   * @return array containing [parsedBlocksCounter, nonBlocksCounter, errorCounter]
   */
  private int[] exportDataWithStatus(
      OutputWriter outputWriter,
      boolean deserialized,
      int parallelThreads,
      boolean showProgressInfo,
      ExportStatus exportStatus,
      String errorFilePath)
      throws IOException {

    // Reuse existing dbReader if available, otherwise create new one
    if (dbReader == null) {
      dbReader = new DbReader(tonDatabaseRootPath);
    }

    //    if (dbReader.getArchiveDbReader() == null) {
    //      throw new IOException("ArchiveDbReader is not initialized");
    //    }

    Map<String, ArchiveInfo> packFiles = dbReader.getAllPackFiles();
    if (packFiles == null) {
      throw new IOException("Archive infos map is null");
    }

    AtomicInteger parsedBlocksCounter = new AtomicInteger(exportStatus.getParsedBlocksCount());
    AtomicInteger nonBlocksCounter = new AtomicInteger(exportStatus.getNonBlocksCount());
    AtomicInteger errorCounter = new AtomicInteger(0);
    long totalPacks = packFiles.size();

    // Track blocks processed in current session for accurate rate calculation
    AtomicInteger sessionParsedBlocks = new AtomicInteger(0);

    // Update total packages if it has changed
    if (exportStatus.getTotalPackages() != totalPacks) {
      exportStatus.setTotalPackages(totalPacks);
      StatusManager.getInstance().saveStatus(exportStatus);
    }

    long startTime = System.currentTimeMillis();

    ExecutorService executor = Executors.newFixedThreadPool(parallelThreads);
    currentProcessingExecutor = executor; // Store reference for shutdown coordination
    List<Future<Void>> futures = new ArrayList<>();

    // Phase 2: Track processed blocks for reset mechanism
    AtomicInteger globalProcessedBlocks = new AtomicInteger(0);

    // Create a separate thread for periodic rate display
    ScheduledExecutorService rateDisplayExecutor;
    if (showProgressInfo) {
      // Create a custom thread factory to make threads daemon so they don't keep JVM alive
      ThreadFactory daemonThreadFactory =
          r -> {
            Thread t = new Thread(r, "PerformanceReporter");
            t.setDaemon(true); // Daemon thread won't keep JVM alive
            return t;
          };
      rateDisplayExecutor = Executors.newSingleThreadScheduledExecutor(daemonThreadFactory);
      currentRateDisplayExecutor = rateDisplayExecutor; // Store reference for shutdown coordination

      rateDisplayExecutor.scheduleWithFixedDelay(
          () -> {
            try {
              //              System.out.println("DEBUG: Scheduler thread starting execution...");
              long currentTime = System.currentTimeMillis();
              long elapsedSeconds = (currentTime - startTime) / 1000;
              if (elapsedSeconds > 0) {
                // Use session blocks for rate calculation to get accurate current rate
                double blocksPerSecond = sessionParsedBlocks.get() / (double) elapsedSeconds;
                double progressPercentage = exportStatus.getProgressPercentage();

                // Calculate estimated time remaining
                String timeRemainingStr = "N/A";
                if (progressPercentage > 0.1) { // Only calculate if we have meaningful progress
                  double estimatedTotalTimeSeconds = elapsedSeconds / (progressPercentage / 100.0);
                  long estimatedRemainingSeconds =
                      (long) (estimatedTotalTimeSeconds - elapsedSeconds);

                  if (estimatedRemainingSeconds > 0) {
                    timeRemainingStr = formatDuration(estimatedRemainingSeconds);
                  }
                }

                System.out.printf(
                    "Block rate: %.2f blocks/sec (total: %d blocks, session: %d blocks, elapsed: %ds, progress: %.1f%%, ETA: %s)%n",
                    blocksPerSecond,
                    parsedBlocksCounter.get(),
                    sessionParsedBlocks.get(),
                    elapsedSeconds,
                    progressPercentage,
                    timeRemainingStr);
              }
            } catch (Exception e) {
              System.out.println("DEBUG: CRITICAL ERROR in scheduler thread: " + e.getMessage());
              e.printStackTrace();
              // Don't rethrow - keep scheduler running
            }
          },
          10,
          10,
          TimeUnit.SECONDS);
    }

    for (Map.Entry<String, ArchiveInfo> entry : packFiles.entrySet()) {
      if (shutdownRequested) {
        log.info("Shutdown requested, stopping submission of new packages");
        break;
      }

      String archiveKey = entry.getKey();
      ArchiveInfo archiveInfo = entry.getValue();

      if (exportStatus.isPackageProcessed(archiveKey)) {
        if (showProgressInfo) {
          log.info("Skipping already processed archive: {}", archiveKey);
        }
        continue;
      }

      Future<Void> future =
          executor.submit(
              () -> {
                StopWatch stopWatch = new StopWatch();
                stopWatch.start();
                if (shutdownRequested) {
                  return null;
                }
                try {
                  int localParsedBlocks = 0;
                  int localNonBlocks = 0;
                  localParsedBlocks =
                      processFilesPackageInMemory(
                          archiveKey,
                          archiveInfo,
                          outputWriter,
                          deserialized,
                          errorFilePath,
                          parsedBlocksCounter,
                          nonBlocksCounter,
                          errorCounter,
                          sessionParsedBlocks);

                  // Mark package as processed (optimized to avoid synchronized file I/O)
                  exportStatus.markPackageProcessed(archiveKey, localParsedBlocks, localNonBlocks);

                  StatusManager.getInstance().saveStatus(exportStatus);

                  if (showProgressInfo) {
                    System.out.printf(
                        "progress: %5.1f%% %6d/%d, size %7dkb, blocks %6d, elapsed %6dms - %s %n",
                        exportStatus.getProgressPercentage(),
                        exportStatus.getProcessedCount(),
                        exportStatus.getTotalPackages(),
                        entry.getValue().getPackageSize() / 1024,
                        localParsedBlocks,
                        stopWatch.getTime(),
                        archiveKey);
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

    long endTime = System.currentTimeMillis();
    long durationMs = endTime - startTime;
    double durationSeconds = durationMs / 1000.0;
    double blocksPerSecond = parsedBlocksCounter.get() / durationSeconds;

    // PerformanceProfiler completely disabled to eliminate atomic contention
    // globalProfiler.recordWallClockEnd();

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

    // Shutdown rate display executor properly
    ScheduledExecutorService rateExecutor = currentRateDisplayExecutor;
    if (rateExecutor != null && !rateExecutor.isShutdown()) {
      //      log.debug("Shutting down rate display executor...");
      rateExecutor.shutdown();
      try {
        if (!rateExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
          rateExecutor.shutdownNow();
        }
      } catch (InterruptedException e) {
        rateExecutor.shutdownNow();
        Thread.currentThread().interrupt();
      }
    }
    //
    //    // Only mark as completed if we actually processed all packages and weren't interrupted
    boolean actuallyCompleted =
        !shutdownRequested && exportStatus.getProcessedCount() >= exportStatus.getTotalPackages();

    if (actuallyCompleted) {
      exportStatus.markCompleted();
      StatusManager.getInstance().saveStatus(exportStatus);
      System.out.printf(
          "Export completed successfully. Processed %s/%s packages.%n",
          exportStatus.getProcessedCount(), exportStatus.getTotalPackages());
      // Clean up status file after successful completion
      StatusManager.getInstance().deleteStatus();
    } else {
      // Save status without marking as completed for potential resume
      StatusManager.getInstance().saveStatus(exportStatus);
      if (shutdownRequested) {
        log.info(
            "Export interrupted by shutdown request. Processed {}/{} packages.",
            exportStatus.getProcessedCount(),
            exportStatus.getTotalPackages());
      } else {
        log.warn(
            "Export finished but not all packages were processed. Processed {}/{} packages.",
            exportStatus.getProcessedCount(),
            exportStatus.getTotalPackages());
      }
    }

    System.out.printf(
        "Total duration: %.1fs, speed: %.2f blocks per second, blocks %s%n",
        durationSeconds, blocksPerSecond, parsedBlocksCounter.get());

    dbReader.close();

    // Update total statistics for potential access during interruption
    totalParsedBlocks.set(parsedBlocksCounter.get());
    totalNonBlocks.set(nonBlocksCounter.get());
    totalErrors.set(errorCounter.get());

    return new int[] {parsedBlocksCounter.get(), nonBlocksCounter.get(), errorCounter.get()};
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
    ExportStatus exportStatus = StatusManager.getInstance().loadStatus();
    boolean isResume = false;

    if (exportStatus != null && !exportStatus.isCompleted()) {
      // Validate that the resume parameters match
      if ("file".equals(exportStatus.getExportType())
          && outputToFile.equals(exportStatus.getOutputFile())
          && deserialized == exportStatus.isDeserialized()
          && parallelThreads == exportStatus.getParallelThreads()) {

        // Check if the output file still exists for resume
        File outputFile = new File(outputToFile);
        File absoluteOutputFile = outputFile.getAbsoluteFile();

        if (absoluteOutputFile.exists() && absoluteOutputFile.length() > 0) {
          log.info(
              "Resuming export from previous session. Progress: {}% ({}/{}), file size: {} bytes",
              exportStatus.getProgressPercentage(),
              exportStatus.getProcessedCount(),
              exportStatus.getTotalPackages(),
              absoluteOutputFile.length());
          isResume = true;
        } else {
          log.warn(
              "Resume requested but output file '{}' doesn't exist or is empty. Starting fresh export.",
              absoluteOutputFile.getAbsolutePath());
          StatusManager.getInstance().deleteStatus();
          exportStatus = null;
          isResume = false;
        }
      } else {
        log.warn("Export parameters don't match existing status. Starting fresh export.");
        StatusManager.getInstance().deleteStatus();
        exportStatus = null;
      }
    }

    // Create new status if not resuming
    if (exportStatus == null) {
      // Get total packages count - reuse dbReader to avoid duplicate scanning
      if (dbReader == null) {
        dbReader = new DbReader(tonDatabaseRootPath);
      }
      long totalPackages = dbReader.getAllPackFiles().size();
      // Don't close dbReader here as we'll reuse it in exportDataWithStatus

      exportStatus =
          StatusManager.getInstance()
              .createNewStatus(totalPackages, "file", outputToFile, deserialized, parallelThreads);
      StatusManager.getInstance().saveStatus(exportStatus);
      log.info("Starting new export to file: {}", outputToFile);
    }

    File outputFile = new File(outputToFile);
    String errorFilePath = new File(outputFile.getParent(), "errors.txt").getAbsolutePath();

    try (AsyncFileWriter asyncWriter =
        new AsyncFileWriter(
            outputToFile, isResume, 5000, 256 * 1024, 1000)) { // Flush every 1000 lines

      // Create output writer using AsyncFileWriter
      OutputWriter outputWriter = asyncWriter::writeLine;

      exportDataWithStatus(
          outputWriter, deserialized, parallelThreads, showProgress, exportStatus, errorFilePath);
    }
  }

  /**
   * @param deserialized if true - deserialized Block TL-B object will be saved as json string,
   *     otherwise boc in hex format will be stored in a single line
   * @param parallelThreads number of parallel threads used to export a database
   */
  public void exportToStdout(boolean deserialized, int parallelThreads) throws IOException {
    ExportStatus exportStatus = StatusManager.getInstance().loadStatus();

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
      } else {
        log.warn("Export parameters don't match existing status. Starting fresh export.");
        StatusManager.getInstance().deleteStatus();
        exportStatus = null;
      }
    }

    // Create new status if not resuming
    if (exportStatus == null) {
      // Get total packages count - reuse dbReader to avoid duplicate scanning
      if (dbReader == null) {
        dbReader = new DbReader(tonDatabaseRootPath);
      }
      long totalPackages = dbReader.getAllPackFiles().size();
      // Don't close dbReader here as we'll reuse it in exportDataWithStatus

      exportStatus =
          StatusManager.getInstance()
              .createNewStatus(totalPackages, "stdout", null, deserialized, parallelThreads);
      StatusManager.getInstance().saveStatus(exportStatus);
      log.info("Starting new export to stdout");
    }

    // Disable logging for stdout export to avoid interference with output
    LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
    for (Logger logger : loggerContext.getLoggerList()) {
      logger.setLevel(Level.OFF);
    }

    OutputWriter stdoutWriter = System.out::println;

    exportDataWithStatus(stdoutWriter, deserialized, parallelThreads, false, exportStatus, null);

    for (Logger logger : loggerContext.getLoggerList()) {
      logger.setLevel(Level.INFO);
    }
  }

  /**
   * Export blocks to a Stream of ExportedBlock objects that can be used with parallelStream()
   *
   * @param deserialized if true - Block objects will be deserialized, otherwise only raw data is
   *     available
   * @param parallelThreads number of parallel threads to use for processing packages
   * @return Stream of ExportedBlock objects that supports parallelStream() with the specified
   *     thread count
   * @throws IOException if there's an error reading the database
   */
  public Stream<ExportedBlock> exportToObjects(boolean deserialized, int parallelThreads)
      throws IOException {

    // Check for existing status and resume if possible
    ExportStatus exportStatus = StatusManager.getInstance().loadStatus();

    if (exportStatus != null && !exportStatus.isCompleted()) {
      // Validate that the resume parameters match
      if ("objects".equals(exportStatus.getExportType())
          && deserialized == exportStatus.isDeserialized()
          && parallelThreads == exportStatus.getParallelThreads()) {

        System.out.printf(
            "Resuming objects export from previous session. Progress: %.1f%% %s/%s",
            exportStatus.getProgressPercentage(),
            exportStatus.getProcessedCount(),
            exportStatus.getTotalPackages());
      } else {
        log.warn("Export parameters don't match existing status. Starting fresh export.");
        StatusManager.getInstance().deleteStatus();
        exportStatus = null;
      }
    }

    dbReader = new DbReader(tonDatabaseRootPath);

    // Get all archive entries
    Map<String, ArchiveInfo> archiveInfos = dbReader.getAllPackFiles();

    // Create new status if not resuming
    if (exportStatus == null) {
      long totalPackages = archiveInfos.size();
      exportStatus =
          StatusManager.getInstance()
              .createNewStatus(totalPackages, "objects", null, deserialized, parallelThreads);
      StatusManager.getInstance().saveStatus(exportStatus);
      log.info("Starting new export to objects stream");
    }

    try {
      // Create a stream of archive entries and process them in parallel
      // Filter out already processed packages if resuming
      final ExportStatus finalExportStatus = exportStatus;
      Stream<ExportedBlock> blockStream =
          archiveInfos.entrySet().stream()
              .filter(
                  entry ->
                      !finalExportStatus.isPackageProcessed(
                          entry.getKey())) // Skip processed packages
              .flatMap(
                  entry -> {
                    String archiveKey = entry.getKey();
                    ArchiveInfo archiveInfo = entry.getValue();

                    try {
                      //                      Map<String, byte[]> localBlocks = new HashMap<>();
                      AtomicInteger localParsedBlocks = new AtomicInteger();
                      AtomicInteger localNonBlocks = new AtomicInteger();
                      List<ExportedBlock> exportedBlocks = new ArrayList<>();
                      AtomicInteger localErrors = new AtomicInteger();

                      try (PackageReader packageReader =
                          new PackageReader(archiveInfo.getPackagePath())) {
                        packageReader.forEachTyped(
                            kv -> {
                              String filename = kv.getFilename();
                              if (filename.startsWith("block_")) {
                                String blockKey = extractHashFromFilename(filename);
                                if (blockKey != null) {
                                  try {
                                    Cell c = kv.getCell();
                                    long magic = c.getBits().preReadUint(32).longValue();

                                    if (magic == 0x11ef55aaL) {
                                      Block deserializedBlock = null;

                                      if (deserialized) {
                                        try {
                                          deserializedBlock =
                                              Block.deserialize(CellSlice.beginParse(c));
                                        } catch (Throwable e) {
                                          log.info(
                                              "Error deserializing block {}: {}",
                                              kv.getFilename(),
                                              e.getMessage());
                                          localErrors.getAndIncrement();
                                          // Continue with null deserializedBlock
                                        }
                                      }

                                      ExportedBlock exportedBlock =
                                          ExportedBlock.builder()
                                              .archiveKey(archiveKey)
                                              .blockKey(kv.getFilename())
                                              .rawData(kv.getData())
                                              .deserializedBlock(deserializedBlock)
                                              .isDeserialized(
                                                  deserialized && deserializedBlock != null)
                                              .build();

                                      exportedBlocks.add(exportedBlock);
                                      localParsedBlocks.getAndIncrement();
                                    } else {
                                      localNonBlocks.getAndIncrement();
                                    }
                                  } catch (Throwable e) {
                                    log.debug(
                                        "Error processing block {}: {}",
                                        kv.getFilename(),
                                        e.getMessage());
                                    localErrors.getAndIncrement();
                                  }
                                }
                              }
                            });
                      }
                      //                      if (archiveInfo.getIndexPath() == null) {
                      //                        dbReader
                      //                            .getArchiveDbReader()
                      //                            .readFromFilesPackage(archiveKey, archiveInfo,
                      // localBlocks);
                      //                        dbReader
                      //                            .getGlobalIndexDbReader()
                      //                            .readFromFilesPackage(archiveKey, archiveInfo,
                      // localBlocks);
                      //                      } else {
                      //                        dbReader
                      //                            .getArchiveDbReader()
                      //                            .readFromTraditionalArchive(archiveKey,
                      // archiveInfo, localBlocks);
                      //                      }

                      // Convert to ExportedBlock objects and count blocks/non-blocks/errors
                      //                      List<ExportedBlock> exportedBlocks = new
                      // ArrayList<>();
                      //                      int localErrors = 0;
                      //
                      //                      for (Map.Entry<String, byte[]> kv :
                      // localBlocks.entrySet()) {
                      //
                      //                      }

                      // Mark package as processed and save status synchronously
                      finalExportStatus.markPackageProcessed(
                          archiveKey,
                          localParsedBlocks.get(),
                          localNonBlocks.get(),
                          localErrors.get());

                      // Save status synchronously to ensure all packages are tracked
                      try {
                        StatusManager.getInstance().saveStatus(finalExportStatus);
                      } catch (Exception e) {
                        log.warn("Error saving export status: {}", e.getMessage());
                      }

                      if (showProgress) {
                        System.out.printf(
                            "progress: %5.1f%% %6d/%d archive %s%n",
                            finalExportStatus.getProgressPercentage(),
                            finalExportStatus.getProcessedCount(),
                            finalExportStatus.getTotalPackages(),
                            archiveKey);
                      }

                      return exportedBlocks.stream();

                    } catch (Throwable e) {
                      log.warn(
                          "Error reading blocks from archive {}: {}", archiveKey, e.getMessage());
                      return Stream.empty();
                    }
                  });

      // Mark export as completed when stream is consumed
      Stream<ExportedBlock> wrappedStream =
          blockStream.onClose(
              () -> {
                try {
                  // Ensure final status is saved before marking as completed
                  StatusManager.getInstance().saveStatus(finalExportStatus);
                  finalExportStatus.markCompleted();
                  StatusManager.getInstance().saveStatus(finalExportStatus);
                  log.info(
                      "Completed objects export: {} blocks, {} non-blocks, {} errors processed",
                      finalExportStatus.getParsedBlocksCount(),
                      finalExportStatus.getNonBlocksCount(),
                      finalExportStatus.getErrors());
                  // Clean up status file after successful completion
                  StatusManager.getInstance().deleteStatus();
                } catch (Exception e) {
                  log.error("Error finalizing export status: {}", e.getMessage());
                }
              });

      // Create a fresh ForkJoinPool for each stream to avoid thread pool reuse issues
      // The ParallelStreamWrapper will handle the thread pool lifecycle
      ForkJoinPool customThreadPool = new ForkJoinPool(parallelThreads);

      // Return a stream that uses the custom thread pool for parallel operations
      return new ParallelStreamWrapper<>(wrappedStream, customThreadPool);

    } catch (Exception e) {
      dbReader.close();
      throw e;
    }
  }

  /** Process Files package using streaming approach to avoid memory accumulation */
  private int processFilesPackageInMemory(
      String archiveKey,
      ArchiveInfo archiveInfo,
      OutputWriter outputWriter,
      boolean deserialized,
      String errorFilePath,
      AtomicInteger parsedBlocksCounter,
      AtomicInteger nonBlocksCounter,
      AtomicInteger errorCounter,
      AtomicInteger sessionParsedBlocks)
      throws IOException {

    AtomicInteger localParsedBlocks = new AtomicInteger(0);
    AtomicInteger localNonBlocks = new AtomicInteger(0);

    // Use streaming PackageReader instead of loading everything into memory
    try (PackageReader packageReader = new PackageReader(archiveInfo.getPackagePath())) {
      // Process each entry as it's read from disk (no memory accumulation)
      packageReader.forEachTyped(
          entry -> {
            String filename = entry.getFilename();

            // Only process block files
            if (filename.startsWith("block_")) {
              String blockKey = extractHashFromFilename(filename);
              if (blockKey != null) {
                int beforeParsed = parsedBlocksCounter.get();
                int beforeNonBlocks = nonBlocksCounter.get();

                processBlockData(
                    blockKey,
                    entry.getData(),
                    outputWriter,
                    deserialized,
                    errorFilePath,
                    parsedBlocksCounter,
                    nonBlocksCounter,
                    errorCounter,
                    sessionParsedBlocks);

                // Track local increments
                int afterParsed = parsedBlocksCounter.get();
                int afterNonBlocks = nonBlocksCounter.get();

                localParsedBlocks.addAndGet(afterParsed - beforeParsed);
                localNonBlocks.addAndGet(afterNonBlocks - beforeNonBlocks);
              }
            }
          });

      return localParsedBlocks.get();
    }
  }

  /**
   * Extracts hash from a filename like "block_(-1,8000000000000000,100):hash1:hash2". Returns the
   * first hash (hash1) which is typically used as the key.
   */
  private String extractHashFromFilename(String filename) {
    try {
      if (filename.contains("):")) {
        int colonIndex = filename.indexOf("):");
        if (colonIndex != -1) {
          String hashPart = filename.substring(colonIndex + 2);
          int nextColonIndex = hashPart.indexOf(':');
          if (nextColonIndex != -1) {
            return hashPart.substring(0, nextColonIndex);
          } else {
            return hashPart;
          }
        }
      }
    } catch (Exception e) {
      log.warn("Error extracting hash from filename {}: {}", filename, e.getMessage());
    }
    return null;
  }

  //
  //  /** Process traditional archive using streaming approach to avoid memory accumulation */
  //  private int processTraditionalArchiveInMemory(
  //      String archiveKey,
  //      ArchiveInfo archiveInfo,
  //      OutputWriter outputWriter,
  //      boolean deserialized,
  //      String errorFilePath,
  //      AtomicInteger parsedBlocksCounter,
  //      AtomicInteger nonBlocksCounter,
  //      AtomicInteger errorCounter,
  //      AtomicInteger sessionParsedBlocks)
  //      throws IOException {
  //
  //    AtomicInteger localParsedBlocks = new AtomicInteger(0);
  //    AtomicInteger localNonBlocks = new AtomicInteger(0);
  //
  //    // Use streaming PackageReader instead of loading everything into memory
  //    try (PackageReader packageReader = new PackageReader(archiveInfo.getPackagePath())) {
  //
  //      // Process each entry as it's read from disk (no memory accumulation)
  //      packageReader.forEachTyped(
  //          entry -> {
  //            String filename = entry.getFilename();
  //
  //            // Only process block files
  //            if (filename.startsWith("block_")) {
  //              String blockKey = extractHashFromFilename(filename);
  //              if (blockKey != null) {
  //                int beforeParsed = parsedBlocksCounter.get();
  //                int beforeNonBlocks = nonBlocksCounter.get();
  //
  //                processBlockData(
  //                    blockKey,
  //                    entry.getData(),
  //                    outputWriter,
  //                    deserialized,
  //                    errorFilePath,
  //                    parsedBlocksCounter,
  //                    nonBlocksCounter,
  //                    errorCounter,
  //                    sessionParsedBlocks);
  //
  //                // Track local increments
  //                int afterParsed = parsedBlocksCounter.get();
  //                int afterNonBlocks = nonBlocksCounter.get();
  //
  //                localParsedBlocks.addAndGet(afterParsed - beforeParsed);
  //                localNonBlocks.addAndGet(afterNonBlocks - beforeNonBlocks);
  //              }
  //            }
  //          });
  //      return localParsedBlocks.get();
  //    }
  //  }

  /** Process individual block data with optimized performance (profiling disabled) */
  private void processBlockData(
      String blockKey,
      byte[] blockData,
      OutputWriter outputWriter,
      boolean deserialized,
      String errorFilePath,
      AtomicInteger parsedBlocksCounter,
      AtomicInteger nonBlocksCounter,
      AtomicInteger errorCounter,
      AtomicInteger sessionParsedBlocks) {

    try {
      // Parse BOC to TLB
      Cell c = CellBuilder.beginCell().fromBoc(blockData).endCell();

      // Check magic number after BOC parsing
      long magic = c.getBits().preReadUint(32).longValue();

      if (magic == 0x11ef55aaL) {
        String lineToWrite;
        Block block = null; // Declare block variable for explicit cleanup

        if (deserialized) {
          // Deserialize Block from TLB when deserialized output is requested
          block = Block.deserialize(CellSlice.beginParse(c));

          // Pre-compute values to avoid repeated calls during JSON serialization
          int workchain = block.getBlockInfo().getShard().getWorkchain();
          String shardHex = block.getBlockInfo().getShard().convertShardIdentToShard().toString(16);
          long seqno = block.getBlockInfo().getSeqno();

          String jsonBlock = Exporter.gson.toJson(block);

          // Use StringBuilder for more efficient string construction
          StringBuilder lineBuilder = new StringBuilder(1024); // Pre-allocate reasonable size
          lineBuilder
              .append(workchain)
              .append(',')
              .append(shardHex)
              .append(',')
              .append(seqno)
              .append(',')
              .append(jsonBlock);

          lineToWrite = lineBuilder.toString();

          // Clear JSON string reference immediately after use
          jsonBlock = null;
        } else {
          // Write raw BOC in hex format - no deserialization needed
          lineToWrite = Utils.bytesToHex(blockData);
        }

        // Write to output
        outputWriter.writeLine(lineToWrite);

        int currentParsedCount = parsedBlocksCounter.getAndIncrement();
        sessionParsedBlocks.incrementAndGet(); // Track session blocks for accurate rate calculation
        totalParsedBlocks.incrementAndGet();

        // Pure reference clearing - No GC calls, just help the garbage collector
        if (deserialized && currentParsedCount > 0 && currentParsedCount % 1000 == 0) {
          // Clear references to help GC - let G1GC handle timing naturally
          block = null;
          c = null;
          lineToWrite = null;
        }

      } else {
        nonBlocksCounter.getAndIncrement();
        totalNonBlocks.incrementAndGet();
      }

    } catch (Throwable e) {
      log.debug(
          "Error parsing block {}: {}",
          blockKey,
          e.getMessage()); // Changed to debug to reduce logging overhead
      errorCounter.getAndIncrement();
      totalErrors.incrementAndGet();

      // Write error block data to errors.txt file if errorFilePath is provided
      if (errorFilePath != null) {
        try {
          synchronized (this) {
            try (PrintWriter errorWriter =
                new PrintWriter(new FileWriter(errorFilePath, StandardCharsets.UTF_8, true))) {
              errorWriter.println(Utils.bytesToHex(blockData));
              errorWriter.flush();
            }
          }
        } catch (IOException ioException) {
          log.warn(
              "Failed to write error block data to {}: {}",
              errorFilePath,
              ioException.getMessage());
        }
      }
    }
  }

  public void printADbStats() throws IOException {
    dbReader = new DbReader(tonDatabaseRootPath);

    for (Map.Entry<String, ArchiveInfo> s : dbReader.getAllPackFiles().entrySet()) {
      log.info("Archive {}: {}", s.getKey(), s.getValue());
    }
    log.info("total archive packs found: {}", dbReader.getAllPackFiles().size());
  }

  public Block getBlock(org.ton.ton4j.tlb.BlockIdExt blockIdExt) throws IOException {
    org.ton.ton4j.tl.types.db.block.BlockIdExt blockIdExtTl =
        org.ton.ton4j.tl.types.db.block.BlockIdExt.builder()
            .seqno((int) blockIdExt.getSeqno())
            .workchain(blockIdExt.getWorkchain())
            .shard(blockIdExt.shard)
            .fileHash(blockIdExt.fileHash)
            .rootHash(blockIdExt.rootHash)
            .build();
    return getBlock(blockIdExtTl);
  }

  public Block getBlock(BlockIdExt blockIdExt) throws IOException {
    //    org.ton.ton4j.tl.types.db.block.BlockIdExt blockIdExtTl =
    //        org.ton.ton4j.tl.types.db.block.BlockIdExt.builder()
    //            .seqno((int) blockIdExt.getSeqno())
    //            .workchain(blockIdExt.getWorkchain())
    //            .shard(blockIdExt.shard)
    //            .fileHash(blockIdExt.fileHash)
    //            .rootHash(blockIdExt.rootHash)
    //            .build();

    BlockFileKey blockFileKey = BlockFileKey.builder().blockIdExt(blockIdExt).build();
    //    log.info("key hash {}", blockFileKey.getKeyHash());
    int archiveIndex =
        dbReader
            .getGlobalIndexDbReader()
            .getArchiveIndexBySeqno(blockIdExt.getWorkchain(), blockIdExt.getSeqno());
    //    log.info("archive index {}", archiveIndex);
    long offset;
    try (ArchiveIndexReader archiveIndexReader =
        new ArchiveIndexReader(dbReader.getDbRootPath(), archiveIndex)) {
      offset = archiveIndexReader.getOffsetByHash(blockFileKey.getKeyHash());
      //      log.info("found offset: {}", offset);

      long mcSeqno;
      if (blockIdExt.getWorkchain() == -1) {
        mcSeqno = blockIdExt.getSeqno();
      } else {
        BlockDbValueKey key = BlockDbValueKey.builder().blockIdExt(blockIdExt).build();
        BlockInfo blockInfo = archiveIndexReader.getDbInfoByHash(key.getKeyHash());
        mcSeqno = blockInfo.getMasterRefSeqno();
      }
      //      log.info("found mcSeqno: {}", mcSeqno);
      String packFilename =
          archiveIndexReader.getExactPackFilename(
              archiveIndex,
              blockIdExt.getSeqno(),
              blockIdExt.getWorkchain(),
              blockIdExt.getShard(),
              mcSeqno);
      //      log.info("found pack filename: {}", packFilename);
      try (PackageReader packageReader = new PackageReader(packFilename)) {
        PackageReader.PackageEntry packageEntry = packageReader.getEntryAt(offset);
        return packageEntry.getBlock();
      }
    }
  }

  public Block getBlock(BlockId blockId) throws IOException {

    DbLtDescKey keyHash =
        DbLtDescKey.builder().workchain(blockId.getWorkchain()).shard(blockId.shard).build();

    int archiveIndex =
        dbReader
            .getGlobalIndexDbReader()
            .getArchiveIndexBySeqno(blockId.getWorkchain(), blockId.getSeqno());
    //    log.info("archive index {}", archiveIndex);

    try (ArchiveIndexReader archiveIndexReader =
        new ArchiveIndexReader(dbReader.getDbRootPath(), archiveIndex)) {

      // getting blockExtId
      org.ton.ton4j.tl.types.db.block.BlockIdExt blockIdExt =
          archiveIndexReader.getBlockIdExtByDbLtDescKey(keyHash, blockId.getSeqno());

      // mcSeqno
      long mcSeqno;
      if (blockIdExt.getWorkchain() == -1) {
        mcSeqno = blockIdExt.getSeqno();
      } else {
        BlockDbValueKey key = BlockDbValueKey.builder().blockIdExt(blockIdExt).build();
        BlockInfo blockInfo = archiveIndexReader.getDbInfoByHash(key.getKeyHash());
        mcSeqno = blockInfo.getMasterRefSeqno();
      }

      // offset
      BlockFileKey blockFileKey = BlockFileKey.builder().blockIdExt(blockIdExt).build();
      long offset = archiveIndexReader.getOffsetByHash(blockFileKey.getKeyHash());
      //      log.info("found mcSeqno: {}", mcSeqno);

      String packFilename =
          archiveIndexReader.getExactPackFilename(
              archiveIndex,
              blockIdExt.getSeqno(),
              blockIdExt.getWorkchain(),
              blockIdExt.getShard(),
              mcSeqno);
      //      log.info("found pack filename: {}", packFilename);
      try (PackageReader packageReader = new PackageReader(packFilename)) {
        PackageReader.PackageEntry packageEntry = packageReader.getEntryAt(offset);
        return packageEntry.getBlock();
      }
    }
  }

  //  public Block getBlock(
  //      int wc, long shard, int seqno, byte[] rootHash, byte[] fileHash) throws IOException {
  //
  //    try (GlobalIndexDbReader globalIndexReader = new GlobalIndexDbReader(tonDatabaseRootPath))
  // {}
  //  }

  public org.ton.ton4j.tl.types.db.block.BlockIdExt getLastBlockIdExt() {
    try (StateDbReader stateReader = new StateDbReader(tonDatabaseRootPath)) {

      return stateReader.getLastBlockIdExt();

    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Gets the very last (most recently added) deserialized block from the RocksDB database. This
   * optimized version uses GlobalIndexDbReader to get temp package timestamps directly from the
   * global index, then reads temp packages directly from the files database.
   *
   * @return The most recently added Block of masterchain, or null if no blocks are found
   * @throws IOException If an I/O error occurs while reading the database
   */
  public Pair<org.ton.ton4j.tlb.BlockIdExt, Block> getLast() throws IOException {

    try (GlobalIndexDbReader globalIndexReader = new GlobalIndexDbReader(tonDatabaseRootPath)) {
      IndexValue mainIndex = globalIndexReader.getMainIndexIndexValue();

      if (mainIndex == null || mainIndex.getTempPackages().isEmpty()) {
        log.warn("No temp packages found in global index");
        return null;
      }

      // Get temp package timestamps (they are Unix timestamps)
      List<Integer> tempPackageTimestamps = mainIndex.getTempPackages();
      log.debug("Found {} temp packages in global index", tempPackageTimestamps.size());

      // Sort timestamps in descending order (most recent first)
      List<Integer> sortedTimestamps = new ArrayList<>(tempPackageTimestamps);

      // sort in descending order
      sortedTimestamps.sort(Collections.reverseOrder());

      // get the top (most recent, with the biggest timestamp)
      Integer packageTimestamp = sortedTimestamps.get(0);
      try (TempPackageIndexReader tempIndexReader =
          new TempPackageIndexReader(tonDatabaseRootPath, packageTimestamp)) {
        return tempIndexReader.getLast();
      }
    }
  }

  /**
   * Gets the very last (most recently added) serialized block in format of Bag Of Cells from the
   * RocksDB database. This optimized version uses GlobalIndexDbReader to get temp package
   * timestamps directly from the global index, then reads temp packages directly from the files
   * database.
   *
   * @return The most recently added Block of masterchain, or null if no blocks are found
   * @throws IOException If an I/O error occurs while reading the database
   */
  public byte[] getLastAsBoc() throws IOException {

    try (GlobalIndexDbReader globalIndexReader = new GlobalIndexDbReader(tonDatabaseRootPath)) {
      IndexValue mainIndex = globalIndexReader.getMainIndexIndexValue();

      if (mainIndex == null || mainIndex.getTempPackages().isEmpty()) {
        log.warn("No temp packages found in global index");
        return null;
      }

      // Get temp package timestamps (they are Unix timestamps)
      List<Integer> tempPackageTimestamps = mainIndex.getTempPackages();
      log.debug("Found {} temp packages in global index", tempPackageTimestamps.size());

      // Sort timestamps in descending order (most recent first)
      List<Integer> sortedTimestamps = new ArrayList<>(tempPackageTimestamps);

      // sort in descending order
      sortedTimestamps.sort(Collections.reverseOrder());

      // get the top (most recent, with the biggest timestamp)
      Integer packageTimestamp = sortedTimestamps.get(0);
      try (TempPackageIndexReader tempIndexReader =
          new TempPackageIndexReader(tonDatabaseRootPath, packageTimestamp)) {
        return tempIndexReader.getLastAsBoC();
      }
    }
  }

  public Cell getLastAsCell() throws IOException {

    try (GlobalIndexDbReader globalIndexReader = new GlobalIndexDbReader(tonDatabaseRootPath)) {
      IndexValue mainIndex = globalIndexReader.getMainIndexIndexValue();

      if (mainIndex == null || mainIndex.getTempPackages().isEmpty()) {
        log.warn("No temp packages found in global index");
        return null;
      }

      // Get temp package timestamps (they are Unix timestamps)
      List<Integer> tempPackageTimestamps = mainIndex.getTempPackages();
      log.debug("Found {} temp packages in global index", tempPackageTimestamps.size());

      // Sort timestamps in descending order (most recent first)
      List<Integer> sortedTimestamps = new ArrayList<>(tempPackageTimestamps);

      // sort in descending order
      sortedTimestamps.sort(Collections.reverseOrder());

      // get the top (most recent, with the biggest timestamp)
      Integer packageTimestamp = sortedTimestamps.get(0);
      try (TempPackageIndexReader tempIndexReader =
          new TempPackageIndexReader(tonDatabaseRootPath, packageTimestamp)) {
        return tempIndexReader.getLastAsCell();
      }
    }
  }

  /**
   * Gets the very last (most recently added) <code>limit</code> blocks from the RocksDB database.
   * This optimized version uses GlobalIndexDbReader to get temp package timestamps directly from
   * the global index, then reads temp packages directly from the files database.
   *
   * @return The most recently added number of blocks of masterchain and any workchain.
   * @throws IOException If an I/O error occurs while reading the database
   */
  public TreeMap<org.ton.ton4j.tlb.BlockIdExt, Block> getLast(int limit) throws IOException {

    try (GlobalIndexDbReader globalIndexReader = new GlobalIndexDbReader(tonDatabaseRootPath)) {
      IndexValue mainIndex = globalIndexReader.getMainIndexIndexValue();

      if (mainIndex == null || mainIndex.getTempPackages().isEmpty()) {
        log.warn("No temp packages found in global index");
        return null;
      }

      // Get temp package timestamps (they are Unix timestamps)
      List<Integer> tempPackageTimestamps = mainIndex.getTempPackages();
      log.debug("Found {} temp packages in global index", tempPackageTimestamps.size());

      // Sort timestamps in descending order (most recent first)
      List<Integer> sortedTimestamps = new ArrayList<>(tempPackageTimestamps);

      // sort in descending order
      sortedTimestamps.sort(Collections.reverseOrder());

      // get the top (most recent, with the biggest timestamp)
      Integer packageTimestamp = sortedTimestamps.get(0);
      try (TempPackageIndexReader tempIndexReader =
          new TempPackageIndexReader(tonDatabaseRootPath, packageTimestamp)) {
        return tempIndexReader.getLast(limit);
      }
    }
  }

  public ShardAccountLazy getShardAccountByAddress(BlockIdExt blockIdExt, Address address)
      throws IOException {
    try (CellDbReader cellDbReader = new CellDbReader(tonDatabaseRootPath)) {
      String key = "desc" + Utils.bytesToBase64(Utils.sha256AsArray(blockIdExt.serializeBoxed()));
      byte[] value = cellDbReader.getCellDb().get(key.getBytes());
      //      log.info("key: {}, value: {}", key, Utils.bytesToHex(value));

      CellDbValue cellDbValue = CellDbValue.deserialize(ByteBuffer.wrap(value));
      //      log.info("cellDbValue: {}", cellDbValue);
      byte[] shardStateRootHash = cellDbValue.rootHash;

      // find full cell containing ShardStateUnsplit by shardStateRootHash
      byte[] rawShardStateUnsplit = cellDbReader.getCellDb().get(shardStateRootHash);
      //      log.info("rawShardStateUnsplit: {}", Utils.bytesToHex(rawShardStateUnsplit)); // top
      // level cell

      Cell c = parseCell(ByteBuffer.wrap(rawShardStateUnsplit));
      //      log.info("getMaxLevel: {}, getDepthLevels: {}", c.getMaxLevel(), c.getDepthLevels());

      ShardStateUnsplitLazy shardStateUnsplitLazy =
          ShardStateUnsplitLazy.deserialize(
              cellDbReader, CellSliceLazy.beginParse(cellDbReader, c));

      return shardStateUnsplitLazy.getShardAccounts().getShardAccountByAddress(address);
    }
  }
}
