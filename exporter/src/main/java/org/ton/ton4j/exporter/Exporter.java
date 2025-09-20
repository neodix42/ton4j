package org.ton.ton4j.exporter;

import static java.util.Objects.isNull;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.ToNumberPolicy;
import java.io.*;
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
import org.ton.ton4j.cell.*;
import org.ton.ton4j.exporter.reader.*;
import org.ton.ton4j.exporter.types.*;
import org.ton.ton4j.tl.types.db.block.BlockIdExt;
import org.ton.ton4j.tl.types.db.files.index.IndexValue;
import org.ton.ton4j.tlb.Account;
import org.ton.ton4j.tlb.Block;
import org.ton.ton4j.tlb.BlockHandle;
import org.ton.ton4j.utils.Utils;

@Builder
@Slf4j
public class Exporter {

  // Optimized static GSON instance - single shared instance for all threads
  // No thread-local instances needed since GSON is thread-safe for serialization
  public static final Gson gson =
      new GsonBuilder()
          .setObjectToNumberStrategy(ToNumberPolicy.LONG_OR_DOUBLE)
          //          .registerTypeAdapter(byte[].class, new ByteArrayToHexTypeAdapter())
          //          .registerTypeAdapter(TonHashMapAug.class, new TonHashMapAugTypeAdapter())
          //          .registerTypeAdapter(TonHashMapAugE.class, new TonHashMapAugETypeAdapter())
          //          .registerTypeAdapter(TonHashMap.class, new TonHashMapTypeAdapter())
          //          .registerTypeAdapter(TonHashMapE.class, new TonHashMapETypeAdapter())
          .disableHtmlEscaping()
          .setLenient()
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

  // All reset mechanisms removed - no caching, no performance degradation

  // High-performance decoupled processing components
  private volatile boolean useInMemoryProcessing = true; // Enable in-memory processing by default

  // Optimal writer thread count (independent of processing threads)
  private volatile int optimalWriterThreads = 32; // Default for modern SSDs

  private volatile boolean useBlockLevelParallelization = true; // Enable by default

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

  private DbReader dbReader;
  private StatusManager statusManager;

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

      if (isNull(super.showProgress)) {
        super.showProgress = false;
      }

      Exporter exporter = super.build();
      exporter.statusManager = new StatusManager();

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
   * Enable or disable in-memory processing
   *
   * @param enabled true to enable in-memory processing, false to use traditional streaming
   */
  public void setInMemoryProcessingEnabled(boolean enabled) {
    this.useInMemoryProcessing = enabled;
    log.info("In-memory processing {}", enabled ? "enabled" : "disabled");
  }

  /**
   * Check if in-memory processing is enabled
   *
   * @return true if in-memory processing is enabled
   */
  public boolean isInMemoryProcessingEnabled() {
    return useInMemoryProcessing;
  }

  /**
   * Set optimal writer thread count (independent of processing threads)
   *
   * @param writerThreads Number of writer threads (recommended: 8-64 depending on storage)
   */
  public void setOptimalWriterThreads(int writerThreads) {
    this.optimalWriterThreads = Math.max(1, Math.min(writerThreads, 128));
    log.info("Optimal writer threads set to: {}", this.optimalWriterThreads);
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
      log.debug("Shutting down rate display executor...");
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

    // Ensure dbReader and its components are properly initialized
    if (dbReader == null) {
      throw new IOException("Failed to initialize DbReader");
    }

    if (dbReader.getArchiveDbReader() == null) {
      throw new IOException("ArchiveDbReader is not initialized");
    }

    Map<String, ArchiveInfo> archiveInfos = dbReader.getArchiveDbReader().getArchiveInfos();
    if (archiveInfos == null) {
      throw new IOException("Archive infos map is null");
    }

    AtomicInteger parsedBlocksCounter = new AtomicInteger(exportStatus.getParsedBlocksCount());
    AtomicInteger nonBlocksCounter = new AtomicInteger(exportStatus.getNonBlocksCount());
    AtomicInteger errorCounter = new AtomicInteger(0);
    long totalPacks = archiveInfos.size();

    // Update total packages if it has changed
    if (exportStatus.getTotalPackages() != totalPacks) {
      exportStatus.setTotalPackages(totalPacks);
      if (statusManager != null) {
        statusManager.saveStatus(exportStatus);
      }
    }

    long startTime = System.currentTimeMillis();

    // PerformanceProfiler completely disabled to eliminate atomic contention
    // globalProfiler.setParallelThreads(parallelThreads);
    // globalProfiler.recordWallClockStart();

    // Create thread pool
    ExecutorService executor = Executors.newFixedThreadPool(parallelThreads);
    currentProcessingExecutor = executor; // Store reference for shutdown coordination
    List<Future<Void>> futures = new ArrayList<>();

    // Phase 2: Track processed blocks for reset mechanism
    AtomicInteger globalProcessedBlocks = new AtomicInteger(0);

    // Create a separate thread for periodic rate display
    ScheduledExecutorService rateDisplayExecutor = null;
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

      // Simple counter-based approach - count scheduler runs, report every 6 runs (60 seconds)
      final AtomicInteger schedulerRunCount = new AtomicInteger(0);

      rateDisplayExecutor.scheduleWithFixedDelay(
          () -> {
            try {
              //              System.out.println("DEBUG: Scheduler thread starting execution...");
              long currentTime = System.currentTimeMillis();
              long elapsedSeconds = (currentTime - startTime) / 1000;
              if (elapsedSeconds > 0) {
                double blocksPerSecond = parsedBlocksCounter.get() / (double) elapsedSeconds;
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
                    "Block rate: %.2f blocks/sec (total: %d blocks, elapsed: %ds, progress: %.1f%%, ETA: %s)%n",
                    blocksPerSecond,
                    parsedBlocksCounter.get(),
                    elapsedSeconds,
                    progressPercentage,
                    timeRemainingStr);

                // Print performance analysis every 6 scheduler runs (60 seconds)
                int runCount = schedulerRunCount.incrementAndGet();
                //                System.out.println(                    "DEBUG: Scheduler run " +
                // runCount + " (elapsed: " + elapsedSeconds + "s)");
                if (runCount % 3
                    == 0) { // Every 3 runs = 30 seconds (since scheduler runs every 10s)
                  //                  System.out.println(
                  //                      "DEBUG: Triggering performance report at run "
                  //                          + runCount
                  //                          + " (elapsed: "
                  //                          + elapsedSeconds
                  //                          + "s)");
                  // PerformanceProfiler completely disabled to eliminate atomic contention
                  // try {
                  //   globalProfiler.printReport();
                  // } catch (Exception e) {
                  //   System.out.println("DEBUG: Error in performance report: " + e.getMessage());
                  //   e.printStackTrace();
                  // }
                }
              }
              //              System.out.println("DEBUG: Scheduler thread execution completed
              // successfully");
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

    for (Map.Entry<String, ArchiveInfo> entry : archiveInfos.entrySet()) {
      if (shutdownRequested) {
        log.info("Shutdown requested, stopping submission of new packages");
        break;
      }

      String archiveKey = entry.getKey();
      ArchiveInfo archiveInfo = entry.getValue();

      // Skip already processed packages
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
                // Check shutdown signal at the beginning of each task
                if (shutdownRequested) {
                  //                  log.debug("Shutdown requested, skipping archive: {}",
                  // archiveKey);
                  return null;
                }
                try {
                  // Use streaming processing instead of loading all blocks into memory
                  int localParsedBlocks = 0;
                  int localNonBlocks = 0;

                  // Use static GSON instance - thread-safe for serialization
                  //        Gson localGson = gson;

                  // Choose processing approach based on configuration
                  if (useInMemoryProcessing) {
                    // Use in-memory processing for maximum performance
                    if (archiveInfo.getIndexPath() == null) {
                      localParsedBlocks =
                          processFilesPackageInMemory(
                              archiveKey,
                              archiveInfo,
                              outputWriter,
                              deserialized,
                              errorFilePath,
                              parsedBlocksCounter,
                              nonBlocksCounter,
                              errorCounter);
                    } else {
                      localParsedBlocks =
                          processTraditionalArchiveInMemory(
                              archiveKey,
                              archiveInfo,
                              outputWriter,
                              deserialized,
                              gson,
                              errorFilePath,
                              parsedBlocksCounter,
                              nonBlocksCounter,
                              errorCounter);
                    }
                  } else {
                    // Use traditional streaming approach
                    if (archiveInfo.getIndexPath() == null) {
                      localParsedBlocks =
                          processFilesPackageStreaming(
                              archiveKey,
                              archiveInfo,
                              outputWriter,
                              deserialized,
                              gson,
                              errorFilePath,
                              parsedBlocksCounter,
                              nonBlocksCounter,
                              errorCounter);
                    } else {
                      localParsedBlocks =
                          processTraditionalArchiveStreaming(
                              archiveKey,
                              archiveInfo,
                              outputWriter,
                              deserialized,
                              gson,
                              errorFilePath,
                              parsedBlocksCounter,
                              nonBlocksCounter,
                              errorCounter);
                    }
                  }

                  // Mark package as processed (optimized to avoid synchronized file I/O)
                  exportStatus.markPackageProcessed(archiveKey, localParsedBlocks, localNonBlocks);

                  // Save status less frequently to avoid performance degradation
                  // Only save every 10th package to reduce file I/O overhead
                  if (statusManager != null && exportStatus.getProcessedCount() % 10 == 0) {
                    // Use separate thread for status saving to avoid blocking processing
                    CompletableFuture.runAsync(
                        () -> {
                          try {
                            statusManager.saveStatus(exportStatus);
                          } catch (Exception e) {
                            log.warn("Error saving export status: {}", e.getMessage());
                          }
                        });
                  }

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
      log.debug("Shutting down rate display executor...");
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
      statusManager.saveStatus(exportStatus);
      System.out.printf(
          "Export completed successfully. Processed %s/%s packages.%n",
          exportStatus.getProcessedCount(), exportStatus.getTotalPackages());
      // Clean up status file after successful completion
      statusManager.deleteStatus();
    } else {
      // Save status without marking as completed for potential resume
      statusManager.saveStatus(exportStatus);
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
      // Get total packages count - reuse dbReader to avoid duplicate scanning
      if (dbReader == null) {
        dbReader = new DbReader(tonDatabaseRootPath);
      }
      long totalPackages = dbReader.getArchiveDbReader().getArchiveInfos().size();
      // Don't close dbReader here as we'll reuse it in exportDataWithStatus

      exportStatus =
          statusManager.createNewStatus(
              totalPackages, "file", outputToFile, deserialized, parallelThreads);
      statusManager.saveStatus(exportStatus);
      log.info("Starting new export to file: {}", outputToFile);
    }

    // Use reasonable default buffer sizes and queue capacity
    int queueCapacity = 10000; // Conservative default queue capacity
    int bufferSizeMB = 64; // Conservative default buffer size per writer thread

    // Use existing AsyncFileWriter instead of HighPerformanceFileWriter to avoid massive queue
    // accumulation
    File outputFile = new File(outputToFile);
    String errorFilePath = new File(outputFile.getParent(), "errors.txt").getAbsolutePath();

    // Create AsyncFileWriter with reasonable queue size (much smaller than
    // HighPerformanceFileWriter)
    try (AsyncFileWriter asyncWriter =
        new AsyncFileWriter(
            outputToFile,
            isResume,
            5000, // Small queue capacity (vs 200,000 in HighPerformanceFileWriter)
            256 * 1024, // 256KB buffer
            1000)) { // Flush every 1000 lines

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
    ExportStatus exportStatus = statusManager.loadStatus();

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
        statusManager.deleteStatus();
        exportStatus = null;
      }
    }

    // Create new status if not resuming
    if (exportStatus == null) {
      // Get total packages count - reuse dbReader to avoid duplicate scanning
      if (dbReader == null) {
        dbReader = new DbReader(tonDatabaseRootPath);
      }
      long totalPackages = dbReader.getArchiveDbReader().getArchiveInfos().size();
      // Don't close dbReader here as we'll reuse it in exportDataWithStatus

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
    ExportStatus exportStatus = statusManager.loadStatus();

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
        statusManager.deleteStatus();
        exportStatus = null;
      }
    }

    dbReader = new DbReader(tonDatabaseRootPath);

    // Get all archive entries
    Map<String, ArchiveInfo> archiveInfos = dbReader.getArchiveDbReader().getArchiveInfos();

    // Create new status if not resuming
    if (exportStatus == null) {
      long totalPackages = archiveInfos.size();
      exportStatus =
          statusManager.createNewStatus(
              totalPackages, "objects", null, deserialized, parallelThreads);
      statusManager.saveStatus(exportStatus);
      log.info("Starting new export to objects stream");
    }

    // Create a custom ForkJoinPool with the specified parallelism level
    ForkJoinPool customThreadPool = new ForkJoinPool(parallelThreads);

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
                      Map<String, byte[]> localBlocks = new HashMap<>();
                      int localParsedBlocks = 0;
                      int localNonBlocks = 0;

                      // Read blocks from archive
                      if (archiveInfo.getIndexPath() == null) {
                        dbReader
                            .getArchiveDbReader()
                            .readFromFilesPackage(archiveKey, archiveInfo, localBlocks);
                      } else {
                        dbReader
                            .getArchiveDbReader()
                            .readFromTraditionalArchive(archiveKey, archiveInfo, localBlocks);
                      }

                      // Convert to ExportedBlock objects and count blocks/non-blocks
                      List<ExportedBlock> exportedBlocks = new ArrayList<>();

                      for (Map.Entry<String, byte[]> kv : localBlocks.entrySet()) {
                        try {
                          Cell c = CellBuilder.beginCell().fromBoc(kv.getValue()).endCell();
                          long magic = c.getBits().preReadUint(32).longValue();

                          if (magic == 0x11ef55aaL) {
                            Block deserializedBlock = null;

                            if (deserialized) {
                              try {
                                deserializedBlock = Block.deserialize(CellSlice.beginParse(c));
                              } catch (Throwable e) {
                                log.debug(
                                    "Error deserializing block {}: {}",
                                    kv.getKey(),
                                    e.getMessage());
                                // Continue with null deserializedBlock
                              }
                            }

                            ExportedBlock exportedBlock =
                                ExportedBlock.builder()
                                    .archiveKey(archiveKey)
                                    .blockKey(kv.getKey())
                                    .rawData(kv.getValue())
                                    .deserializedBlock(deserializedBlock)
                                    .isDeserialized(deserialized && deserializedBlock != null)
                                    .build();

                            exportedBlocks.add(exportedBlock);
                            localParsedBlocks++;
                          } else {
                            localNonBlocks++;
                          }
                        } catch (Throwable e) {
                          log.debug("Error processing block {}: {}", kv.getKey(), e.getMessage());
                          localNonBlocks++;
                        }
                      }

                      // Mark package as processed and save status
                      synchronized (finalExportStatus) { // todo same as for file
                        finalExportStatus.markPackageProcessed(
                            archiveKey, localParsedBlocks, localNonBlocks);
                        statusManager.saveStatus(finalExportStatus);
                      }

                      if (showProgress) {
                        System.out.printf(
                            "progress: %5.1f%% %6d/%d archive %s%n",
                            finalExportStatus.getProgressPercentage(),
                            finalExportStatus.getProcessedCount(),
                            finalExportStatus.getTotalPackages(),
                            archiveKey);
                      } else {
                        log.debug(
                            "Completed reading archive {}: {} entries",
                            archiveKey,
                            localBlocks.size());
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
                  finalExportStatus.markCompleted();
                  statusManager.saveStatus(finalExportStatus);
                  log.info(
                      "Completed objects export: {} blocks, {} non-blocks processed",
                      finalExportStatus.getParsedBlocksCount(),
                      finalExportStatus.getNonBlocksCount());
                  // Clean up status file after successful completion
                  statusManager.deleteStatus();
                } catch (Exception e) {
                  log.error("Error finalizing export status: {}", e.getMessage());
                }
              });

      // Return a stream that uses the custom thread pool for parallel operations
      return new ParallelStreamWrapper<>(wrappedStream, customThreadPool);

    } catch (Exception e) {
      customThreadPool.shutdown();
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
      AtomicInteger errorCounter)
      throws IOException {

    long startTime = System.nanoTime();

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
                    Exporter.gson,
                    errorFilePath,
                    parsedBlocksCounter,
                    nonBlocksCounter,
                    errorCounter);

                // Track local increments
                int afterParsed = parsedBlocksCounter.get();
                int afterNonBlocks = nonBlocksCounter.get();

                localParsedBlocks.addAndGet(afterParsed - beforeParsed);
                localNonBlocks.addAndGet(afterNonBlocks - beforeNonBlocks);
              }
            }
          });

      long totalTime = System.nanoTime() - startTime;
      //      log.debug(
      //          "Processed {} blocks using streaming in {}ms, packName {}, packSize {}",
      //          localParsedBlocks.get(),
      //          totalTime / 1_000_000,
      //          archiveInfo.getPackagePath(),
      //          archiveInfo.getPackageSize());

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

  /** Process Files package using streaming approach to avoid loading all blocks into memory */
  private int processFilesPackageStreaming(
      String archiveKey,
      ArchiveInfo archiveInfo,
      OutputWriter outputWriter,
      boolean deserialized,
      Gson localGson,
      String errorFilePath,
      AtomicInteger parsedBlocksCounter,
      AtomicInteger nonBlocksCounter,
      AtomicInteger errorCounter)
      throws IOException {

    AtomicInteger localParsedBlocks = new AtomicInteger(0);
    AtomicInteger localNonBlocks = new AtomicInteger(0);

    // Use streaming approach with callback to process blocks one by one
    dbReader
        .getArchiveDbReader()
        .streamFromFilesPackage(
            archiveKey,
            archiveInfo,
            (blockKey, blockData) -> {
              int beforeParsed = parsedBlocksCounter.get();
              int beforeNonBlocks = nonBlocksCounter.get();

              processBlockData(
                  blockKey,
                  blockData,
                  outputWriter,
                  deserialized,
                  localGson,
                  errorFilePath,
                  parsedBlocksCounter,
                  nonBlocksCounter,
                  errorCounter);

              // Track local increments
              int afterParsed = parsedBlocksCounter.get();
              int afterNonBlocks = nonBlocksCounter.get();

              localParsedBlocks.addAndGet(afterParsed - beforeParsed);
              localNonBlocks.addAndGet(afterNonBlocks - beforeNonBlocks);
            });

    return localParsedBlocks.get();
  }

  /** Process traditional archive using streaming approach to avoid memory accumulation */
  private int processTraditionalArchiveInMemory(
      String archiveKey,
      ArchiveInfo archiveInfo,
      OutputWriter outputWriter,
      boolean deserialized,
      Gson localGson,
      String errorFilePath,
      AtomicInteger parsedBlocksCounter,
      AtomicInteger nonBlocksCounter,
      AtomicInteger errorCounter)
      throws IOException {

    long startTime = System.nanoTime();

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
                    localGson,
                    errorFilePath,
                    parsedBlocksCounter,
                    nonBlocksCounter,
                    errorCounter);

                // Track local increments
                int afterParsed = parsedBlocksCounter.get();
                int afterNonBlocks = nonBlocksCounter.get();

                localParsedBlocks.addAndGet(afterParsed - beforeParsed);
                localNonBlocks.addAndGet(afterNonBlocks - beforeNonBlocks);
              }
            }
          });

      long totalTime = System.nanoTime() - startTime;
      //      log.debug(
      //          "Processed {} blocks from traditional archive using streaming in {}ms",
      //          localParsedBlocks.get(),
      //          totalTime / 1_000_000);

      return localParsedBlocks.get();
    }
  }

  /**
   * Process traditional archive using streaming approach to avoid loading all blocks into memory
   */
  private int processTraditionalArchiveStreaming(
      String archiveKey,
      ArchiveInfo archiveInfo,
      OutputWriter outputWriter,
      boolean deserialized,
      Gson localGson,
      String errorFilePath,
      AtomicInteger parsedBlocksCounter,
      AtomicInteger nonBlocksCounter,
      AtomicInteger errorCounter)
      throws IOException {

    AtomicInteger localParsedBlocks = new AtomicInteger(0);
    AtomicInteger localNonBlocks = new AtomicInteger(0);

    // Use streaming approach with callback to process blocks one by one
    dbReader
        .getArchiveDbReader()
        .streamFromTraditionalArchive(
            archiveKey,
            archiveInfo,
            (blockKey, blockData) -> {
              int beforeParsed = parsedBlocksCounter.get();
              int beforeNonBlocks = nonBlocksCounter.get();

              processBlockData(
                  blockKey,
                  blockData,
                  outputWriter,
                  deserialized,
                  localGson,
                  errorFilePath,
                  parsedBlocksCounter,
                  nonBlocksCounter,
                  errorCounter);

              // Track local increments
              int afterParsed = parsedBlocksCounter.get();
              int afterNonBlocks = nonBlocksCounter.get();

              localParsedBlocks.addAndGet(afterParsed - beforeParsed);
              localNonBlocks.addAndGet(afterNonBlocks - beforeNonBlocks);
            });

    return localParsedBlocks.get();
  }

  // Global performance profiler for measurements (shared across all threads)
  private static final PerformanceProfiler globalProfiler = new PerformanceProfiler();

  // All reset mechanisms removed - no caching, no performance degradation

  /** Process individual block data with optimized performance (profiling disabled) */
  private void processBlockData(
      String blockKey,
      byte[] blockData,
      OutputWriter outputWriter,
      boolean deserialized,
      Gson localGson,
      String errorFilePath,
      AtomicInteger parsedBlocksCounter,
      AtomicInteger nonBlocksCounter,
      AtomicInteger errorCounter) {

    // Removed globalProfiler calls - they were causing performance degradation
    // due to atomic operations on shared counters across 32 threads

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

          //          StopWatch stopWatch = new StopWatch();
          //          stopWatch.start();

          // JSON serialization
          String jsonBlock = localGson.toJson(block);

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
          //          log.info("jsoned {}", stopWatch.getTime());

          // Clear JSON string reference immediately after use
          jsonBlock = null;
        } else {
          // Write raw BOC in hex format - no deserialization needed
          lineToWrite = Utils.bytesToHex(blockData);
        }

        // Write to output
        outputWriter.writeLine(lineToWrite);

        int currentParsedCount = parsedBlocksCounter.getAndIncrement();
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

    for (Map.Entry<String, ArchiveInfo> s :
        dbReader.getArchiveDbReader().getArchiveInfos().entrySet()) {
      log.info("Archive {}: {}", s.getKey(), s.getValue());
    }
    log.info(
        "total archive packs found: {}", dbReader.getArchiveDbReader().getArchiveInfos().size());
  }

  /**
   * Gets the very last (most recently added) deserialized block from the RocksDB database. This
   * optimized version uses GlobalIndexDbReader to get temp package timestamps directly from the
   * global index, then reads temp packages directly from the files database.
   *
   * @return The most recently added Block of masterchain, or null if no blocks are found
   * @throws IOException If an I/O error occurs while reading the database
   */
  public Block getLast() throws IOException {

    try (GlobalIndexDbReader globalIndexReader = new GlobalIndexDbReader(tonDatabaseRootPath)) {
      IndexValue mainIndex = globalIndexReader.getMainIndexIndexValue();

      if (mainIndex == null || mainIndex.getTempPackages().isEmpty()) {
        log.warn("No temp packages found in global index");
        return null;
      }

      // Get temp package timestamps (they are Unix timestamps)
      List<Integer> tempPackageTimestamps = mainIndex.getTempPackages();
      log.debug(
          "Found {} temp packages in global index: {}",
          tempPackageTimestamps.size(),
          tempPackageTimestamps);

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
      log.debug(
          "Found {} temp packages in global index: {}",
          tempPackageTimestamps.size(),
          tempPackageTimestamps);

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

  public Pair<Cell, Block> getLastAsPair() throws IOException {

    try (GlobalIndexDbReader globalIndexReader = new GlobalIndexDbReader(tonDatabaseRootPath)) {
      IndexValue mainIndex = globalIndexReader.getMainIndexIndexValue();

      if (mainIndex == null || mainIndex.getTempPackages().isEmpty()) {
        log.warn("No temp packages found in global index");
        return null;
      }

      // Get temp package timestamps (they are Unix timestamps)
      List<Integer> tempPackageTimestamps = mainIndex.getTempPackages();
      log.debug(
          "Found {} temp packages in global index: {}",
          tempPackageTimestamps.size(),
          tempPackageTimestamps);

      // Sort timestamps in descending order (most recent first)
      List<Integer> sortedTimestamps = new ArrayList<>(tempPackageTimestamps);

      // sort in descending order
      sortedTimestamps.sort(Collections.reverseOrder());

      // get the top (most recent, with the biggest timestamp)
      Integer packageTimestamp = sortedTimestamps.get(0);
      try (TempPackageIndexReader tempIndexReader =
          new TempPackageIndexReader(tonDatabaseRootPath, packageTimestamp)) {
        return tempIndexReader.getLastAsPair();
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
  public TreeMap<BlockId, Block> getLast(int limit) throws IOException {
    dbReader = new DbReader(tonDatabaseRootPath);

    try {
      try (GlobalIndexDbReader globalIndexReader = new GlobalIndexDbReader(tonDatabaseRootPath)) {
        IndexValue mainIndex = globalIndexReader.getMainIndexIndexValue();

        if (mainIndex == null || mainIndex.getTempPackages().isEmpty()) {
          log.warn("No temp packages found in global index");
          return null;
        }

        // Get temp package timestamps (they are Unix timestamps)
        List<Integer> tempPackageTimestamps = mainIndex.getTempPackages();
        log.debug(
            "Found {} temp packages in global index: {}",
            tempPackageTimestamps.size(),
            tempPackageTimestamps);

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
    } finally {
      if (dbReader != null) {
        dbReader.close();
      }
    }
  }

  // way to slow, will be reworked one day
  private Account getAccountByAddress(Address address) throws IOException {
    try (CellDbReader cellDbReader = new CellDbReader(tonDatabaseRootPath)) {
      return cellDbReader.retrieveAccountByAddress(address).getAccount();
    }
  }

  /**
   * Gets account state by address following the same database access patterns as the original C++
   * TON node implementation. This method implements the raw.getAccountState() TL API functionality.
   *
   * <p>The process follows these steps: 1. Get the latest masterchain block from the files database
   * (same as getLast()) 2. Extract shard prefix from the address using Address.getShardAsLong() 3.
   * Resolve the appropriate shard for the account 4. Read the shard state from the most recent
   * block 5. Navigate the account dictionary in the shard state 6. Extract and return the account
   * state
   *
   * <p>Note: This implementation uses the files database (temp packages) to get recent blocks and
   * follows the original C++ implementation patterns for fast account state retrieval.
   *
   * @param address The account address to look up
   * @return The Account state, or null if not found
   * @throws IOException If an I/O error occurs while reading the database
   */
  // way to slow, will be reworked one day
  private Account getAccountState(Address address) throws IOException {
    log.debug("Getting account state for address: {}", address.toString(false));

    try {
      // Step 1: Get the latest masterchain block using the same approach as getLast()
      // This uses the files database (temp packages) where recent blocks are stored
      Block latestMcBlock = getLast();
      if (latestMcBlock == null) {
        log.debug("No latest masterchain block found in files database");
        return null;
      }

      log.debug(
          "Latest masterchain block found: workchain={}, shard={}, seqno={}",
          latestMcBlock.getBlockInfo().getShard().getWorkchain(),
          latestMcBlock.getBlockInfo().getShard().convertShardIdentToShard().toString(16),
          latestMcBlock.getBlockInfo().getSeqno());

      // Step 2: Extract shard prefix from address
      long shardPrefix = address.getShardAsLong();
      log.debug(
          "Address {} has shard prefix: 0x{}",
          address.toString(false),
          Long.toHexString(shardPrefix));

      // Step 3: Resolve the appropriate shard for the account
      // In TON, accounts are distributed across shards based on their address hash
      // For the account's workchain, we need to find the shard that contains this address

      // Step 4: Find and read the appropriate shard state
      Block shardBlock = findShardBlockForAddress(address, latestMcBlock);
      if (shardBlock == null) {
        log.debug("No shard block found for address: {}", address.toString(false));
        return null;
      }

      log.debug(
          "Found shard block for address: workchain={}, shard={}, seqno={}",
          shardBlock.getBlockInfo().getShard().getWorkchain(),
          shardBlock.getBlockInfo().getShard().convertShardIdentToShard().toString(16),
          shardBlock.getBlockInfo().getSeqno());

      // Step 5: Navigate the account dictionary in the shard state
      // The shard block contains the shard state with account dictionary
      Account account = extractAccountFromShardState(address, shardBlock);

      if (account != null) {
        log.debug("Successfully retrieved account state for address: {}", address.toString(false));
      } else {
        log.debug("No account state found for address: {}", address.toString(false));
      }

      return account;

    } catch (Exception e) {
      log.error(
          "Error getting account state for address {}: {}",
          address.toString(false),
          e.getMessage());
      throw new IOException("Failed to get account state: " + e.getMessage(), e);
    }
  }

  /**
   * Finds the appropriate shard block for the given address. This method searches through temp
   * packages to find the shard block that contains the address.
   *
   * @param address The address to find the shard for
   * @param masterchainBlock The latest masterchain block
   * @return The shard block containing the address, or null if not found
   * @throws IOException If an I/O error occurs
   */
  // way to slow, will be reworked one day
  private Block findShardBlockForAddress(Address address, Block masterchainBlock)
      throws IOException {
    try {
      // Use the same approach as getLast() to search through temp packages
      try (GlobalIndexDbReader globalIndexReader = new GlobalIndexDbReader(tonDatabaseRootPath)) {
        IndexValue mainIndex = globalIndexReader.getMainIndexIndexValue();

        if (mainIndex == null || mainIndex.getTempPackages().isEmpty()) {
          log.debug("No temp packages found for shard block lookup");
          return null;
        }

        // Get temp package timestamps and sort them (most recent first)
        List<Integer> tempPackageTimestamps = mainIndex.getTempPackages();
        List<Integer> sortedTimestamps = new ArrayList<>(tempPackageTimestamps);
        sortedTimestamps.sort(Collections.reverseOrder());

        // Search through temp packages to find the appropriate shard block
        for (Integer packageTimestamp : sortedTimestamps) {
          try (TempPackageIndexReader tempIndexReader =
              new TempPackageIndexReader(tonDatabaseRootPath, packageTimestamp)) {

            // Get all blocks from this temp package
            Map<BlockId, Block> blocks = tempIndexReader.getAllBlocks();

            // Look for shard blocks in the target workchain
            for (Map.Entry<BlockId, Block> entry : blocks.entrySet()) {
              BlockId blockId = entry.getKey();
              Block block = entry.getValue();

              // Check if this is a shard block for the target workchain
              if (blockId.getWorkchain() == address.wc && blockId.getWorkchain() != -1) {
                // Check if this shard contains the address
                if (shardContainsAddress(blockId.shard, address.getShardAsLong())) {
                  log.debug(
                      "Found shard block for address in temp package {}: workchain={}, shard=0x{}",
                      packageTimestamp,
                      blockId.getWorkchain(),
                      blockId.getShard());
                  return block;
                }
              }
            }
          } catch (Exception e) {
            log.debug("Error searching temp package {}: {}", packageTimestamp, e.getMessage());
            continue;
          }
        }

        // If no specific shard block found, try to use the masterchain block
        // In some cases, account state might be accessible through masterchain
        log.debug("No specific shard block found, using masterchain block");
        return masterchainBlock;
      }
    } catch (Exception e) {
      log.warn("Error finding shard block for address: {}", e.getMessage());
      return masterchainBlock; // Fallback to masterchain block
    }
  }

  /**
   * Checks if a shard contains the given address based on shard prefix matching.
   *
   * @param shardId The shard ID
   * @param addressShardPrefix The address shard prefix
   * @return True if the shard contains the address
   */
  private boolean shardContainsAddress(long shardId, long addressShardPrefix) {
    // Simplified shard matching - in practice, this would involve more complex shard tree logic
    // For now, we'll use a basic prefix matching approach
    return (shardId & 0xF000000000000000L) == (addressShardPrefix & 0xF000000000000000L);
  }

  /**
   * Extracts account state from the shard state contained in the shard block. This method navigates
   * the account dictionary in the shard state to find the specific account.
   *
   * @param address The address to look up
   * @param shardBlock The shard block containing the state
   * @return The Account state, or null if not found
   * @throws IOException If an I/O error occurs
   */
  private Account extractAccountFromShardState(Address address, Block shardBlock)
      throws IOException {
    try {
      // The shard block contains state information
      // For now, we'll use a simplified approach that leverages existing cell database access
      // In a full implementation, this would parse the shard state TLB structure directly

      // Create a BlockIdExt for the shard block to use with StateDbReader
      BlockIdExt shardBlockId =
          BlockIdExt.builder()
              .workchain(shardBlock.getBlockInfo().getShard().getWorkchain())
              .shard(shardBlock.getBlockInfo().getShard().convertShardIdentToShard().longValue())
              .seqno(shardBlock.getBlockInfo().getSeqno())
              .rootHash(new byte[32]) // Simplified - would extract from block
              .fileHash(new byte[32]) // Simplified - would extract from block
              .build();

      // Use StateDbReader to get block handle and state hash
      try (StateDbReader stateDbReader = new StateDbReader(tonDatabaseRootPath)) {
        byte[] blockHandle = stateDbReader.getBlockHandle(shardBlockId);
        byte[] stateHash = stateDbReader.getStateHash(shardBlockId);

        if (blockHandle != null && stateHash != null) {
          log.debug("Found block handle and state hash for shard block");

          // For now, fall back to the existing cell database approach for account extraction
          // This provides the account dictionary navigation functionality
          // In a full implementation, we would parse the shard state directly here
          try (CellDbReader cellDbReader = new CellDbReader(tonDatabaseRootPath)) {
            return cellDbReader.retrieveAccountByAddress(address).getAccount();
          }
        } else {
          log.debug("No block handle or state hash found, using direct cell database access");

          // Direct cell database access as fallback
          try (CellDbReader cellDbReader = new CellDbReader(tonDatabaseRootPath)) {
            return cellDbReader.retrieveAccountByAddress(address).getAccount();
          }
        }
      }

    } catch (Exception e) {
      log.warn("Error extracting account from shard state: {}", e.getMessage());

      // Final fallback to direct cell database access
      try (CellDbReader cellDbReader = new CellDbReader(tonDatabaseRootPath)) {
        return cellDbReader.retrieveAccountByAddress(address).getAccount();
      }
    }
  }

  //  public Account getAccountByAddress(Address address) throws IOException {
  //    Block block = getLast();
  //  }

  // review required
  private BlockHandle getLastBlockHandle() {

    try {
      // Step 1: Get the latest masterchain block using the same approach as getLast()
      // This uses the files database (temp packages) where recent blocks are stored
      Block latestMcBlock = getLast();
      if (latestMcBlock == null) {
        log.debug("No latest masterchain block found in files database");
        return null;
      }
      try (StateDbReader stateReader = new StateDbReader(tonDatabaseRootPath)) {

        byte[] blockHandleBytes = stateReader.getBlockHandle(BlockIdExt.builder().build());
        log.info("blockHandle bytes: {}", blockHandleBytes);
        // TODO: Convert bytes to BlockHandle object if needed
        return null; // Placeholder return
      }
    } catch (Exception e) {
      log.error("Error getting last block handle: {}", e.getMessage());
      return null;
    }
  }
}
