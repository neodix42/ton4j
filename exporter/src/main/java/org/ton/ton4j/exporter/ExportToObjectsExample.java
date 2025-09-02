package org.ton.ton4j.exporter;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.extern.slf4j.Slf4j;
import org.ton.ton4j.exporter.types.ExportedBlock;

/** Example demonstrating the usage of exportToObjects() method */
@Slf4j
public class ExportToObjectsExample {

  public static void main(String[] args) throws IOException {
    if (args.length < 1) {
      System.out.println(
          "Usage: java ExportToObjectsExample <database_path> [parallel_threads] [deserialized]");
      System.out.println("Example: java ExportToObjectsExample /var/ton-work/db 4 true");
      return;
    }

    String databasePath = args[0];
    int parallelThreads = args.length > 1 ? Integer.parseInt(args[1]) : 4;
    boolean deserialized = args.length > 2 ? Boolean.parseBoolean(args[2]) : true;

    log.info(
        "Starting export with database path: {}, threads: {}, deserialized: {}",
        databasePath,
        parallelThreads,
        deserialized);

    Exporter exporter = Exporter.builder().tonDatabaseRootPath(databasePath).build();

    AtomicInteger blockCount = new AtomicInteger(0);
    AtomicInteger deserializedCount = new AtomicInteger(0);

    // Example usage as specified in the task
    exporter
        .exportToObjects(deserialized, parallelThreads)
        .forEach(
            b -> {
              int count = blockCount.incrementAndGet();

              if (b.isDeserialized()) {
                deserializedCount.incrementAndGet();
                log.info(
                    "Block #{}: workchain={}, shard={}, seqno={}, archive={}",
                    count,
                    b.getWorkchain(),
                    b.getShard(),
                    b.getSeqno(),
                    b.getArchiveKey());
              } else {
                log.info(
                    "Block #{}: bytes_length={}, archive={}",
                    count,
                    b.getRawDataBytes().length,
                    b.getArchiveKey());
              }

              // Limit output for demonstration
              if (count >= 100) {
                log.info("Stopping after {} blocks for demonstration", count);
                return;
              }
            });

    log.info(
        "Processed {} blocks total, {} deserialized", blockCount.get(), deserializedCount.get());
  }

  /** Example showing different ways to use the stream */
  public static void demonstrateStreamOperations(String databasePath) throws IOException {
    Exporter exporter = Exporter.builder().tonDatabaseRootPath(databasePath).build();

    log.info("=== Example 1: Simple forEach ===");
    exporter
        .exportToObjects(true, 4)
        .limit(5)
        .forEach(
            b -> {
              log.info("block {}", b);
            });

    log.info("=== Example 2: Filter and count ===");
    long masterchainBlocks =
        exporter
            .exportToObjects(true, 4)
            .filter(b -> b.getWorkchain() == -1) // Masterchain blocks
            .limit(100)
            .count();
    log.info("Found {} masterchain blocks", masterchainBlocks);

    log.info("=== Example 3: Parallel processing with custom logic ===");
    exporter
        .exportToObjects(true, 8)
        .filter(ExportedBlock::isDeserialized)
        .limit(50)
        .forEach(
            block -> {
              // Custom processing logic
              String threadName = Thread.currentThread().getName();
              log.info(
                  "Thread {}: Processing block seqno {} from shard {}",
                  threadName,
                  block.getSeqno(),
                  block.getShard());

              // Example: Extract transactions
              if (block.getBlock() != null) {
                int txCount = block.getBlock().getAllTransactions().size();
                log.info("Block has {} transactions", txCount);
              }
            });
  }
}
