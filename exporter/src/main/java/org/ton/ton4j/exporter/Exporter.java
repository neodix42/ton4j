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
   * Common export logic that handles database reading and block processing
   *
   * @param outputWriter strategy for writing output lines
   * @param deserialized if true - deserialized Block TL-B object will be saved as json string,
   *     otherwise boc in hex format will be stored in a single line
   * @param parallelThreads number of parallel threads used to export a database
   * @param showProgressInfo whether to show progress information during export
   * @return array containing [parsedBlocksCounter, nonBlocksCounter, totalProcessed]
   */
  private int[] exportData(
      OutputWriter outputWriter,
      boolean deserialized,
      int parallelThreads,
      boolean showProgressInfo)
      throws IOException {
    Gson gson = new GsonBuilder().setObjectToNumberStrategy(ToNumberPolicy.LONG_OR_DOUBLE).create();

    dbReader = new DbReader(tonDatabaseRootPath);

    AtomicInteger parsedBlocksCounter = new AtomicInteger(0);
    AtomicInteger nonBlocksCounter = new AtomicInteger(0);
    AtomicInteger packsProcessed = new AtomicInteger(0);
    long totalPacks = dbReader.getArchiveDbReader().getArchiveInfos().size();

    long startTime = System.currentTimeMillis();

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
                  Map<String, byte[]> localBlocks = new HashMap<>();

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
                      } else {
                        nonBlocksCounter.getAndIncrement();
                      }

                    } catch (Throwable e) {
                      log.debug("Error parsing block {}: {}", entry.getKey(), e.getMessage());
                      // Continue processing other blocks instead of failing completely
                    }
                  }

                  if (showProgressInfo) {
                    System.out.println(
                        "Completed reading archive "
                            + archiveKey
                            + ": entries "
                            + localBlocks.size()
                            + ", "
                            + packsProcessed.getAndIncrement()
                            + "/"
                            + totalPacks);
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

    File file = new File(outputToFile);

    // Create a synchronized PrintWriter for thread-safe immediate file writing
    try (PrintWriter writer = new PrintWriter(new FileWriter(file, StandardCharsets.UTF_8))) {

      // Create file-based output writer with thread-safe synchronization
      OutputWriter fileWriter =
          line -> {
            synchronized (writer) {
              writer.println(line);
              writer.flush(); // Ensure immediate write to disk
            }
          };

      // Use common export logic with progress info enabled for file export
      int[] results = exportData(fileWriter, deserialized, parallelThreads, true);
      int parsedBlocksCounter = results[0];
      int nonBlocksCounter = results[1];

      log.info(
          "Exported {} blocks (nonBlocks {}) to file: {}",
          parsedBlocksCounter,
          nonBlocksCounter,
          outputToFile);
    }
  }

  /**
   * @param deserialized if true - deserialized Block TL-B object will be saved as json string,
   *     otherwise boc in hex format will be stored in a single line
   * @param parallelThreads number of parallel threads used to export a database
   */
  public void exportToStdout(boolean deserialized, int parallelThreads) throws IOException {
    // Disable logging for stdout export to avoid interference with output
    LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
    for (Logger logger : loggerContext.getLoggerList()) {
      logger.setLevel(Level.OFF);
    }

    // Create stdout-based output writer (System.out is already thread-safe)
    OutputWriter stdoutWriter = System.out::println;

    // Use common export logic with progress info disabled for stdout export
    int[] results = exportData(stdoutWriter, deserialized, parallelThreads, false);
    int parsedBlocksCounter = results[0];
    int nonBlocksCounter = results[1];

    for (Logger logger : loggerContext.getLoggerList()) {
      logger.setLevel(Level.INFO);
    }

    log.info("Exported {} blocks (nonBlocks {}) to stdout", parsedBlocksCounter, nonBlocksCounter);
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
