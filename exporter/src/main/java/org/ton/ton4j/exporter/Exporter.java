package org.ton.ton4j.exporter;

import static java.util.Objects.isNull;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.ToNumberPolicy;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.LoggerFactory;
import org.ton.ton4j.address.Address;
import org.ton.ton4j.cell.Cell;
import org.ton.ton4j.cell.CellBuilder;
import org.ton.ton4j.cell.CellSlice;
import org.ton.ton4j.exporter.reader.*;
import org.ton.ton4j.exporter.types.ArchiveInfo;
import org.ton.ton4j.exporter.types.BlockId;
import org.ton.ton4j.exporter.types.ExportStatus;
import org.ton.ton4j.exporter.types.ExportedBlock;
import org.ton.ton4j.tl.types.db.files.index.IndexValue;
import org.ton.ton4j.tlb.Account;
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
                      log.info("Error parsing block {}: {}", entry.getKey(), e.getMessage());
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
                    System.out.printf(
                        "Completed reading archive %s: entries %d, progress: %.1f%% (%d/%d)%n",
                        archiveKey,
                        localBlocks.size(),
                        exportStatus.getProgressPercentage(),
                        exportStatus.getProcessedCount(),
                        exportStatus.getTotalPackages());
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

    // Create a synchronized PrintWriter for thread-safe immediate file writing
    // Use append mode if resuming
    try (PrintWriter writer =
        new PrintWriter(new FileWriter(outputToFile, StandardCharsets.UTF_8, isResume))) {

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
          exportDataWithStatus(
              fileWriter, deserialized, parallelThreads, showProgress, exportStatus);
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
    boolean isResume = false;

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
        isResume = true;
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
                      synchronized (finalExportStatus) {
                        finalExportStatus.markPackageProcessed(
                            archiveKey, localParsedBlocks, localNonBlocks);
                        statusManager.saveStatus(finalExportStatus);
                      }

                      if (showProgress) {
                        System.out.printf(
                            "Completed reading archive %s: entries %d, progress: %.1f%% (%d/%d)%n",
                            archiveKey,
                            localBlocks.size(),
                            finalExportStatus.getProgressPercentage(),
                            finalExportStatus.getProcessedCount(),
                            finalExportStatus.getTotalPackages());
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

  /** Wrapper class to provide a Stream that uses a custom ForkJoinPool for parallel operations */
  private static class ParallelStreamWrapper<T> implements Stream<T> {
    private final Stream<T> delegate;
    private final java.util.concurrent.ForkJoinPool customPool;

    public ParallelStreamWrapper(Stream<T> delegate, java.util.concurrent.ForkJoinPool customPool) {
      this.delegate = delegate;
      this.customPool = customPool;
    }

    @Override
    public Stream<T> parallel() {
      return this;
    }

    @Override
    public Stream<T> sequential() {
      return delegate.sequential();
    }

    @Override
    public boolean isParallel() {
      return true;
    }

    // Delegate all other Stream methods to the underlying stream
    @Override
    public Stream<T> filter(java.util.function.Predicate<? super T> predicate) {
      return new ParallelStreamWrapper<>(delegate.filter(predicate), customPool);
    }

    @Override
    public <R> Stream<R> map(java.util.function.Function<? super T, ? extends R> mapper) {
      return new ParallelStreamWrapper<>(delegate.map(mapper), customPool);
    }

    @Override
    public <R> Stream<R> flatMap(
        java.util.function.Function<? super T, ? extends Stream<? extends R>> mapper) {
      return new ParallelStreamWrapper<>(delegate.flatMap(mapper), customPool);
    }

    @Override
    public void forEach(java.util.function.Consumer<? super T> action) {
      try {
        customPool.submit(() -> delegate.parallel().forEach(action)).get();
      } catch (Exception e) {
        throw new RuntimeException(e);
      } finally {
        customPool.shutdown();
        try {
          if (!customPool.awaitTermination(60, TimeUnit.SECONDS)) {
            customPool.shutdownNow();
          }
        } catch (InterruptedException ex) {
          customPool.shutdownNow();
          Thread.currentThread().interrupt();
        }
      }
    }

    @Override
    public void forEachOrdered(java.util.function.Consumer<? super T> action) {
      try {
        customPool.submit(() -> delegate.parallel().forEachOrdered(action)).get();
      } catch (Exception e) {
        throw new RuntimeException(e);
      } finally {
        customPool.shutdown();
      }
    }

    @Override
    public java.util.Iterator<T> iterator() {
      return delegate.iterator();
    }

    @Override
    public java.util.Spliterator<T> spliterator() {
      return delegate.spliterator();
    }

    @Override
    public long count() {
      try {
        return customPool.submit(() -> delegate.parallel().count()).get();
      } catch (Exception e) {
        throw new RuntimeException(e);
      } finally {
        customPool.shutdown();
      }
    }

    @Override
    public java.util.Optional<T> min(java.util.Comparator<? super T> comparator) {
      try {
        return customPool.submit(() -> delegate.parallel().min(comparator)).get();
      } catch (Exception e) {
        throw new RuntimeException(e);
      } finally {
        customPool.shutdown();
      }
    }

    @Override
    public java.util.Optional<T> max(java.util.Comparator<? super T> comparator) {
      try {
        return customPool.submit(() -> delegate.parallel().max(comparator)).get();
      } catch (Exception e) {
        throw new RuntimeException(e);
      } finally {
        customPool.shutdown();
      }
    }

    @Override
    public boolean anyMatch(java.util.function.Predicate<? super T> predicate) {
      try {
        return customPool.submit(() -> delegate.parallel().anyMatch(predicate)).get();
      } catch (Exception e) {
        throw new RuntimeException(e);
      } finally {
        customPool.shutdown();
      }
    }

    @Override
    public boolean allMatch(java.util.function.Predicate<? super T> predicate) {
      try {
        return customPool.submit(() -> delegate.parallel().allMatch(predicate)).get();
      } catch (Exception e) {
        throw new RuntimeException(e);
      } finally {
        customPool.shutdown();
      }
    }

    @Override
    public boolean noneMatch(java.util.function.Predicate<? super T> predicate) {
      try {
        return customPool.submit(() -> delegate.parallel().noneMatch(predicate)).get();
      } catch (Exception e) {
        throw new RuntimeException(e);
      } finally {
        customPool.shutdown();
      }
    }

    @Override
    public java.util.Optional<T> findFirst() {
      try {
        return customPool.submit(() -> delegate.parallel().findFirst()).get();
      } catch (Exception e) {
        throw new RuntimeException(e);
      } finally {
        customPool.shutdown();
      }
    }

    @Override
    public java.util.Optional<T> findAny() {
      try {
        return customPool.submit(() -> delegate.parallel().findAny()).get();
      } catch (Exception e) {
        throw new RuntimeException(e);
      } finally {
        customPool.shutdown();
      }
    }

    @Override
    public Object[] toArray() {
      try {
        return customPool.submit(() -> delegate.parallel().toArray()).get();
      } catch (Exception e) {
        throw new RuntimeException(e);
      } finally {
        customPool.shutdown();
      }
    }

    @Override
    public <A> A[] toArray(java.util.function.IntFunction<A[]> generator) {
      try {
        return customPool.submit(() -> delegate.parallel().toArray(generator)).get();
      } catch (Exception e) {
        throw new RuntimeException(e);
      } finally {
        customPool.shutdown();
      }
    }

    @Override
    public T reduce(T identity, java.util.function.BinaryOperator<T> accumulator) {
      try {
        return customPool.submit(() -> delegate.parallel().reduce(identity, accumulator)).get();
      } catch (Exception e) {
        throw new RuntimeException(e);
      } finally {
        customPool.shutdown();
      }
    }

    @Override
    public java.util.Optional<T> reduce(java.util.function.BinaryOperator<T> accumulator) {
      try {
        return customPool.submit(() -> delegate.parallel().reduce(accumulator)).get();
      } catch (Exception e) {
        throw new RuntimeException(e);
      } finally {
        customPool.shutdown();
      }
    }

    @Override
    public <U> U reduce(
        U identity,
        java.util.function.BiFunction<U, ? super T, U> accumulator,
        java.util.function.BinaryOperator<U> combiner) {
      try {
        return customPool
            .submit(() -> delegate.parallel().reduce(identity, accumulator, combiner))
            .get();
      } catch (Exception e) {
        throw new RuntimeException(e);
      } finally {
        customPool.shutdown();
      }
    }

    @Override
    public <R> R collect(
        java.util.function.Supplier<R> supplier,
        java.util.function.BiConsumer<R, ? super T> accumulator,
        java.util.function.BiConsumer<R, R> combiner) {
      try {
        return customPool
            .submit(() -> delegate.parallel().collect(supplier, accumulator, combiner))
            .get();
      } catch (Exception e) {
        throw new RuntimeException(e);
      } finally {
        customPool.shutdown();
      }
    }

    @Override
    public <R, A> R collect(java.util.stream.Collector<? super T, A, R> collector) {
      try {
        return customPool.submit(() -> delegate.parallel().collect(collector)).get();
      } catch (Exception e) {
        throw new RuntimeException(e);
      } finally {
        customPool.shutdown();
      }
    }

    @Override
    public Stream<T> distinct() {
      return new ParallelStreamWrapper<>(delegate.distinct(), customPool);
    }

    @Override
    public Stream<T> sorted() {
      return new ParallelStreamWrapper<>(delegate.sorted(), customPool);
    }

    @Override
    public Stream<T> sorted(java.util.Comparator<? super T> comparator) {
      return new ParallelStreamWrapper<>(delegate.sorted(comparator), customPool);
    }

    @Override
    public Stream<T> peek(java.util.function.Consumer<? super T> action) {
      return new ParallelStreamWrapper<>(delegate.peek(action), customPool);
    }

    @Override
    public Stream<T> limit(long maxSize) {
      return new ParallelStreamWrapper<>(delegate.limit(maxSize), customPool);
    }

    @Override
    public Stream<T> skip(long n) {
      return new ParallelStreamWrapper<>(delegate.skip(n), customPool);
    }

    @Override
    public java.util.stream.IntStream mapToInt(java.util.function.ToIntFunction<? super T> mapper) {
      return delegate.mapToInt(mapper);
    }

    @Override
    public java.util.stream.LongStream mapToLong(
        java.util.function.ToLongFunction<? super T> mapper) {
      return delegate.mapToLong(mapper);
    }

    @Override
    public java.util.stream.DoubleStream mapToDouble(
        java.util.function.ToDoubleFunction<? super T> mapper) {
      return delegate.mapToDouble(mapper);
    }

    @Override
    public java.util.stream.IntStream flatMapToInt(
        java.util.function.Function<? super T, ? extends java.util.stream.IntStream> mapper) {
      return delegate.flatMapToInt(mapper);
    }

    @Override
    public java.util.stream.LongStream flatMapToLong(
        java.util.function.Function<? super T, ? extends java.util.stream.LongStream> mapper) {
      return delegate.flatMapToLong(mapper);
    }

    @Override
    public java.util.stream.DoubleStream flatMapToDouble(
        java.util.function.Function<? super T, ? extends java.util.stream.DoubleStream> mapper) {
      return delegate.flatMapToDouble(mapper);
    }

    @Override
    public Stream<T> takeWhile(java.util.function.Predicate<? super T> predicate) {
      return new ParallelStreamWrapper<>(delegate.takeWhile(predicate), customPool);
    }

    @Override
    public Stream<T> dropWhile(java.util.function.Predicate<? super T> predicate) {
      return new ParallelStreamWrapper<>(delegate.dropWhile(predicate), customPool);
    }

    @Override
    public void close() {
      delegate.close();
      customPool.shutdown();
    }

    @Override
    public Stream<T> onClose(Runnable closeHandler) {
      return new ParallelStreamWrapper<>(delegate.onClose(closeHandler), customPool);
    }

    @Override
    public Stream<T> unordered() {
      return new ParallelStreamWrapper<>(delegate.unordered(), customPool);
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
   * Gets the very last (most recently added) block from the RocksDB database. This optimized
   * version uses GlobalIndexDbReader to get temp package timestamps directly from the global index,
   * then reads temp packages directly from the files database.
   *
   * @return The most recently added Block of masterchain, or null if no blocks are found
   * @throws IOException If an I/O error occurs while reading the database
   */
  public Block getLast() throws IOException {
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
          return tempIndexReader.getLast();
        }
      }
    } finally {
      if (dbReader != null) {
        dbReader.close();
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

  public Account getAccountByAddress(Address address) throws IOException {
    try (CellDbReaderOptimized cellDbReader = new CellDbReaderOptimized(tonDatabaseRootPath)) {
      return cellDbReader.retrieveAccountByAddress(address).getAccount();
    }
  }

  //  public Account getAccountByAddress(Address address) throws IOException {
  //    Block block = getLast();
  //  }
}
