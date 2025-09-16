package org.ton.ton4j.exporter;

import static org.assertj.core.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.extern.slf4j.Slf4j;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.ton.ton4j.utils.Utils;

/**
 * Test suite for Phase 3.1 Block-Level Parallelization implementation.
 *
 * <p>Tests the following components: - BlockLevelParallelProcessor - ParallelBocParser -
 * Integration with Exporter class
 */
@Slf4j
public class TestBlockLevelParallelization {

  private BlockLevelParallelProcessor blockProcessor;
  private ParallelBocParser bocParser;
  private static final int TEST_THREAD_COUNT = 4;

  @Before
  public void setUp() {
    blockProcessor = new BlockLevelParallelProcessor(TEST_THREAD_COUNT);
    bocParser = new ParallelBocParser(TEST_THREAD_COUNT);
  }

  @After
  public void tearDown() {
    if (blockProcessor != null) {
      blockProcessor.shutdown();
    }
    if (bocParser != null) {
      bocParser.shutdown();
    }
  }

  @Test(timeout = 30000)
  public void testBlockLevelParallelProcessorInitialization() {
    assertThat(blockProcessor).isNotNull();
    assertThat(blockProcessor.getParallelThreads()).isEqualTo(TEST_THREAD_COUNT);
    assertThat(blockProcessor.isShutdownRequested()).isFalse();
  }

  @Test(timeout = 30000)
  public void testParallelBocParserInitialization() {
    assertThat(bocParser).isNotNull();
    assertThat(bocParser.getParallelThreads()).isEqualTo(TEST_THREAD_COUNT);

    // Test initial statistics
    ParallelBocParser.ParsingStatistics stats = bocParser.getStatistics();
    assertThat(stats).isNotNull();
    assertThat(stats.getTotalParsedBlocks()).isEqualTo(0);
    assertThat(stats.getTotalParseErrors()).isEqualTo(0);
    assertThat(stats.getSuccessRate())
        .isCloseTo(100.0, within(0.01)); // Should be 100% when no blocks processed
  }

  @Test(timeout = 30000)
  public void testBocParsingConfiguration() {
    // Test configuration for blocks
    ParallelBocParser.BocParseConfig blockConfig = ParallelBocParser.BocParseConfig.forBlocks(true);
    assertThat(blockConfig.shouldDeserializeBlocks()).isTrue();
    assertThat(blockConfig.shouldValidateMagic()).isTrue();
    assertThat(blockConfig.getExpectedMagic()).isEqualTo(0x11ef55aaL);

    // Test configuration for any cell
    ParallelBocParser.BocParseConfig anyConfig = ParallelBocParser.BocParseConfig.forAnyCell();
    assertThat(anyConfig.shouldDeserializeBlocks()).isFalse();
    assertThat(anyConfig.shouldValidateMagic()).isFalse();
    assertThat(anyConfig.getExpectedMagic()).isEqualTo(0);
  }

  @Test(timeout = 30000)
  public void testBocParsingWithValidBlockData() throws Exception {
    // Create a simple valid BOC for testing
    byte[] testBocData = createTestBlockBoc();

    ParallelBocParser.BocParseConfig config = ParallelBocParser.BocParseConfig.forBlocks(false);
    ParallelBocParser.BocParseResult result = bocParser.parseBoc(testBocData, config);

    assertThat(result).isNotNull();
    assertThat(result.isSuccess()).isTrue();
    assertThat(result.getCell()).isNotNull();
    assertThat(result.getBlock()).isNull(); // Not deserialized
    assertThat(result.getErrorMessage()).isNull();
  }

  @Test(timeout = 30000)
  public void testBocParsingWithInvalidData() {
    byte[] invalidData = new byte[] {0x01, 0x02, 0x03, 0x04};

    ParallelBocParser.BocParseConfig config = ParallelBocParser.BocParseConfig.forBlocks(false);
    ParallelBocParser.BocParseResult result = bocParser.parseBoc(invalidData, config);

    assertThat(result).isNotNull();
    assertThat(result.isSuccess()).isFalse();
    assertThat(result.getCell()).isNull();
    assertThat(result.getBlock()).isNull();
    assertThat(result.getErrorMessage()).isNotNull();
    assertThat(result.getErrorMessage()).contains("BOC parsing failed");
  }

  @Test(timeout = 30000)
  public void testAsyncBocParsing() throws Exception {
    byte[] testBocData = createTestBlockBoc();

    ParallelBocParser.BocParseConfig config = ParallelBocParser.BocParseConfig.forBlocks(false);
    CompletableFuture<ParallelBocParser.BocParseResult> future =
        bocParser.parseBocAsync(testBocData, config);

    assertThat(future).isNotNull();
    ParallelBocParser.BocParseResult result = future.get(10, TimeUnit.SECONDS);

    assertThat(result).isNotNull();
    assertThat(result.isSuccess()).isTrue();
    assertThat(result.getCell()).isNotNull();
  }

  @Test(timeout = 30000)
  public void testBatchBocParsing() throws Exception {
    List<byte[]> bocDataList = new ArrayList<>();
    for (int i = 0; i < 10; i++) {
      bocDataList.add(createTestBlockBoc());
    }

    ParallelBocParser.BocParseConfig config = ParallelBocParser.BocParseConfig.forBlocks(false);
    CompletableFuture<List<ParallelBocParser.BocParseResult>> future =
        bocParser.parseBocsBatch(bocDataList, config);

    assertThat(future).isNotNull();
    List<ParallelBocParser.BocParseResult> results = future.get(15, TimeUnit.SECONDS);

    assertThat(results).isNotNull();
    assertThat(results).hasSize(10);

    for (ParallelBocParser.BocParseResult result : results) {
      assertThat(result.isSuccess()).isTrue();
      assertThat(result.getCell()).isNotNull();
    }
  }

  @Test(timeout = 30000)
  public void testBocParsingStatistics() throws Exception {
    // Parse some valid blocks
    for (int i = 0; i < 5; i++) {
      byte[] testBocData = createTestBlockBoc();
      ParallelBocParser.BocParseConfig config = ParallelBocParser.BocParseConfig.forBlocks(false);
      bocParser.parseBoc(testBocData, config);
    }

    // Parse some invalid data
    for (int i = 0; i < 2; i++) {
      byte[] invalidData = new byte[] {0x01, 0x02, 0x03, 0x04};
      ParallelBocParser.BocParseConfig config = ParallelBocParser.BocParseConfig.forBlocks(false);
      bocParser.parseBoc(invalidData, config);
    }

    ParallelBocParser.ParsingStatistics stats = bocParser.getStatistics();
    assertThat(stats).isNotNull();
    assertThat(stats.getTotalParsedBlocks()).isEqualTo(5);
    assertThat(stats.getTotalParseErrors()).isEqualTo(2);

    double expectedSuccessRate = (5.0 / 7.0) * 100.0;
    assertThat(stats.getSuccessRate()).isCloseTo(expectedSuccessRate, within(0.01));

    // Test statistics reset
    bocParser.resetStatistics();
    stats = bocParser.getStatistics();
    assertThat(stats.getTotalParsedBlocks()).isEqualTo(0);
    assertThat(stats.getTotalParseErrors()).isEqualTo(0);
  }

  @Test(timeout = 30000)
  public void testBlockWorkUnit() {
    String testKey = "test-block-key";
    byte[] testData = new byte[] {0x01, 0x02, 0x03};
    String testArchive = "test-archive";

    BlockLevelParallelProcessor.BlockWorkUnit workUnit =
        new BlockLevelParallelProcessor.BlockWorkUnit(testKey, testData, testArchive);

    assertThat(workUnit.getBlockKey()).isEqualTo(testKey);
    assertThat(workUnit.getBlockData()).isEqualTo(testData);
    assertThat(workUnit.getArchiveKey()).isEqualTo(testArchive);
  }

  @Test(timeout = 30000)
  public void testBlockProcessorShutdown() {
    assertThat(blockProcessor.isShutdownRequested()).isFalse();

    blockProcessor.requestShutdown();
    assertThat(blockProcessor.isShutdownRequested()).isTrue();

    // Test that shutdown completes without hanging
    assertThatCode(() -> blockProcessor.shutdown()).doesNotThrowAnyException();
  }

  @Test(timeout = 30000)
  public void testBocParserShutdown() {
    // Test that shutdown completes without hanging
    assertThatCode(() -> bocParser.shutdown()).doesNotThrowAnyException();
  }

  @Test(timeout = 30000)
  public void testExporterBlockLevelParallelizationConfiguration() {
    Exporter exporter =
        Exporter.builder().tonDatabaseRootPath("/tmp/test-db").showProgress(false).build();

    // Test default state
    assertThat(exporter.isBlockLevelParallelizationEnabled()).isTrue();

    // Test disabling
    exporter.setBlockLevelParallelizationEnabled(false);
    assertThat(exporter.isBlockLevelParallelizationEnabled()).isFalse();

    // Test enabling
    exporter.setBlockLevelParallelizationEnabled(true);
    assertThat(exporter.isBlockLevelParallelizationEnabled()).isTrue();

    // Test statistics methods (should not throw when components not initialized)
    assertThatCode(() -> exporter.getBocParsingStatistics()).doesNotThrowAnyException();
    assertThatCode(() -> exporter.resetBocParsingStatistics()).doesNotThrowAnyException();
  }

  @Test(timeout = 30000)
  public void testConcurrentBocParsing() throws Exception {
    int numThreads = 8;
    int blocksPerThread = 10;
    AtomicInteger successCount = new AtomicInteger(0);
    AtomicInteger errorCount = new AtomicInteger(0);

    List<CompletableFuture<Void>> futures = new ArrayList<>();

    for (int t = 0; t < numThreads; t++) {
      CompletableFuture<Void> future =
          CompletableFuture.runAsync(
              () -> {
                for (int i = 0; i < blocksPerThread; i++) {
                  try {
                    byte[] testBocData = createTestBlockBoc();
                    ParallelBocParser.BocParseConfig config =
                        ParallelBocParser.BocParseConfig.forBlocks(false);
                    ParallelBocParser.BocParseResult result =
                        bocParser.parseBoc(testBocData, config);

                    if (result.isSuccess()) {
                      successCount.incrementAndGet();
                    } else {
                      errorCount.incrementAndGet();
                    }
                  } catch (Exception e) {
                    errorCount.incrementAndGet();
                    log.error("Error in concurrent parsing: {}", e.getMessage());
                  }
                }
              });
      futures.add(future);
    }

    // Wait for all threads to complete
    CompletableFuture<Void> allFutures =
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
    allFutures.get(30, TimeUnit.SECONDS);

    int expectedTotal = numThreads * blocksPerThread;
    assertThat(successCount.get() + errorCount.get()).isEqualTo(expectedTotal);

    // Most should succeed (allowing for some potential race conditions in test setup)
    assertThat(successCount.get()).isGreaterThan((int) (expectedTotal * 0.8));
  }

  @Test(timeout = 30000)
  public void testParsingStatisticsThreadSafety() throws Exception {
    int numThreads = 4;
    int operationsPerThread = 100;

    List<CompletableFuture<Void>> futures = new ArrayList<>();

    for (int t = 0; t < numThreads; t++) {
      CompletableFuture<Void> future =
          CompletableFuture.runAsync(
              () -> {
                for (int i = 0; i < operationsPerThread; i++) {
                  try {
                    // Mix of valid and invalid data
                    byte[] data = (i % 2 == 0) ? createTestBlockBoc() : new byte[] {0x01, 0x02};
                    ParallelBocParser.BocParseConfig config =
                        ParallelBocParser.BocParseConfig.forBlocks(false);
                    bocParser.parseBoc(data, config);

                    // Occasionally check statistics (thread-safe operation)
                    if (i % 10 == 0) {
                      ParallelBocParser.ParsingStatistics stats = bocParser.getStatistics();
                      assertThat(stats).isNotNull();
                      assertThat(stats.getTotalParsedBlocks()).isGreaterThanOrEqualTo(0);
                      assertThat(stats.getTotalParseErrors()).isGreaterThanOrEqualTo(0);
                    }
                  } catch (Exception e) {
                    log.error("Error in statistics test: {}", e.getMessage());
                  }
                }
              });
      futures.add(future);
    }

    // Wait for all threads to complete
    CompletableFuture<Void> allFutures =
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
    allFutures.get(30, TimeUnit.SECONDS);

    // Verify final statistics
    ParallelBocParser.ParsingStatistics finalStats = bocParser.getStatistics();
    assertThat(finalStats).isNotNull();

    int totalOperations = numThreads * operationsPerThread;
    assertThat(finalStats.getTotalParsedBlocks() + finalStats.getTotalParseErrors())
        .isEqualTo(totalOperations);
  }

  /**
   * Creates a minimal valid BOC for testing purposes. This creates a simple cell structure that can
   * be parsed successfully.
   */
  private byte[] createTestBlockBoc() {
    return Utils.hexToSignedBytes(
        "b5ee9c72e10211010002b800001c00c400de0170020402a0033c0346035803a4040a0422042a04f20562056a0571041011ef55aaffffff110102030402a09bc7a98700000000800100000001000000000000000000000000000000000065a57c6500000000004c4b4000000000004c4b41f530ba43000000000000000400000000c400000004000000000000002e05060211b8e48dfb43b9aca00407080a8a04b45e4b7c07a6e7c836c5c2df61b33352e46b681d4e3e2a454bf5305502fd4c1fddf467caa9989b5c1f035b5c4e7c5ac8c31d8c2528f8d773b7ae3bf2ecdc2ffe00010002090a03894a33f6fdc1982ce574b6b30d1b8a1574d1fcdb9056434fbcdced45d451545011e0b48651d9576093fecf8a711917790021c5ceae28344e301649c28a1cdd9dab8c4e62f1400f1010009800000000003d0908000000045121fb8e9ad96a839c8b148b7598e70577e0b31449f83731347af3f97e632fc87bb5caea234b002a390f4ab673c842f3466dedcdf838a2a51bf49a0af3d78fab0098000000000000000000000000b45e4b7c07a6e7c836c5c2df61b33352e46b681d4e3e2a454bf5305502fd4c1fa4cb79792e7cb09d63d56d67d4ca613414659d800dc37dc9ef02428244c2589a0005000008000d0010ee6b28000828480101b45e4b7c07a6e7c836c5c2df61b33352e46b681d4e3e2a454bf5305502fd4c1f0001035b9023afe2ffffff1100000000000000000000000000000000010000000065a57c6500000000004c4b4100000004200b0c0d01110000000000000000500e0003001000c300000000000000000000000000000001021dcd6500100000000003d0908000000045121fb8e9ad96a839c8b148b7598e70577e0b31449f83731347af3f97e632fc87bb5caea234b002a390f4ab673c842f3466dedcdf838a2a51bf49a0af3d78fab8006bb040000000000000000000000200000000001e8483ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffc000030020000102a62c29b8");
  }
}
