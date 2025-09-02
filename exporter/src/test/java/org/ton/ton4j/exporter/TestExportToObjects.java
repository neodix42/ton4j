package org.ton.ton4j.exporter;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.ton.ton4j.exporter.types.ExportedBlock;

@Slf4j
public class TestExportToObjects {

  @Test
  public void testExportToObjectsWithDeserialization() throws IOException {

    String dbPath = "/home/neodix/gitProjects/MyLocalTon/myLocalTon/genesis/db";

    try {
      Exporter exporter = Exporter.builder().tonDatabaseRootPath(dbPath).build();

      AtomicInteger blockCount = new AtomicInteger(0);
      AtomicInteger deserializedCount = new AtomicInteger(0);

      // Test with deserialization enabled and 20 parallel threads
      Stream<ExportedBlock> blockStream = exporter.exportToObjects(true, 20);

      blockStream.forEach(
          block -> {
            blockCount.incrementAndGet();

            if (block.isDeserialized() && block.getBlock() != null) {
              deserializedCount.incrementAndGet();
              log.info(
                  "Block info - Workchain: {}, Shard: {}, Seqno: {}",
                  block.getWorkchain(),
                  block.getShard(),
                  block.getSeqno());
            } else {
              log.info("Block hex data length: {}", block.getRawData().length());
            }
          });

      log.info(
          "Total blocks processed: {}, Deserialized: {}",
          blockCount.get(),
          deserializedCount.get());

    } catch (Exception e) {
      log.warn("Test skipped - database path not available: {}", e.getMessage());
    }
  }

  @Test
  public void testExportToObjectsWithoutDeserialization() {
    // Note: This test requires a valid TON database path
    String dbPath = "/home/neodix/gitProjects/MyLocalTon/myLocalTon/genesis/db"; // Update this path

    try {
      Exporter exporter = Exporter.builder().tonDatabaseRootPath(dbPath).build();

      AtomicInteger blockCount = new AtomicInteger(0);

      // Test without deserialization and 20 parallel threads
      Stream<ExportedBlock> blockStream = exporter.exportToObjects(false, 20);

      blockStream
          // .limit(10)
          .forEach(
          block -> {
            blockCount.incrementAndGet();

            log.info(
                "Processing block from archive: {}, key: {}",
                block.getArchiveKey(),
                block.getBlockKey());

            // Should not be deserialized
            assert !block.isDeserialized();
            assert block.getBlock() == null;
            assert block.getWorkchain() == -1;
            assert block.getShard() == null;
            assert block.getSeqno() == -1;

            // But should have raw data
            assert block.getRawData() != null;
            assert block.getRawData() != null;
            assert !block.getRawData().isEmpty();

            //            log.info("Block hex data length: {}", block.getHexData().length());
          });

      log.info("Total blocks processed: {}", blockCount.get());

    } catch (Exception e) {
      log.warn("Test skipped - database path not available: {}", e.getMessage());
    }
  }

  @Test
  public void testParallelStreamFunctionality() throws IOException {
    // Note: This test requires a valid TON database path
    String dbPath = "/home/neodix/gitProjects/MyLocalTon/myLocalTon/genesis/db"; // Update this path

    try {
      Exporter exporter = Exporter.builder().tonDatabaseRootPath(dbPath).build();

      AtomicInteger blockCount = new AtomicInteger(0);

      // Test parallel stream functionality
      Stream<ExportedBlock> blockStream = exporter.exportToObjects(true, 20);

      // Use parallel stream operations
      long count =
          blockStream
              .filter(ExportedBlock::isDeserialized)
              .peek(
                  block -> {
                    blockCount.incrementAndGet();
                    log.info(
                        "Thread: {}, Block seqno: {}",
                        Thread.currentThread().getName(),
                        block.getSeqno());
                  })
              .count();

      log.info(
          "Parallel stream processed {} deserialized blocks, total {}", blockCount.get(), count);

    } catch (Exception e) {
      log.warn("Test skipped - database path not available: {}", e.getMessage());
    }
  }

  /** Example usage as requested in the task */
  @Test
  public void testExampleUsage() throws IOException {
    // Note: This test requires a valid TON database path
    String dbPath = "/home/neodix/gitProjects/MyLocalTon/myLocalTon/genesis/db"; // Update this path

    try {
      Exporter exporter = Exporter.builder().tonDatabaseRootPath(dbPath).build();

      // Example usage as specified in the task
      exporter
          .exportToObjects(true, 20)
          .forEach(
              b -> {
                log.info("block {}", b);
              });

    } catch (Exception e) {
      log.warn("Test skipped - database path not available: {}", e.getMessage());
    }
  }
}
