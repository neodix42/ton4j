package org.ton.java.liteclient;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.ton.java.liteclient.exception.IncompleteDump;
import org.ton.java.liteclient.exception.ParsingError;

/**
 * Performance tests for LiteClientParser class. These tests measure execution time for various
 * parsing operations.
 */
@Slf4j
@RunWith(JUnit4.class)
public class LiteClientParserPerformanceTest {

  private static final int WARMUP_ITERATIONS = 5;
  private static final int TEST_ITERATIONS = 10;

  private String lastCommandOutput;
  private String bySeqCommandOutput;
  private String blockDump;
  private String dumptransTxsCommandOutput;

  @Before
  public void setup() throws IOException {
    // Load test data
    lastCommandOutput =
        IOUtils.toString(
            Objects.requireNonNull(getClass().getResourceAsStream("/last.log")),
            StandardCharsets.UTF_8);
    bySeqCommandOutput =
        IOUtils.toString(
            Objects.requireNonNull(getClass().getResourceAsStream("/byseqno.log")),
            StandardCharsets.UTF_8);
    blockDump =
        IOUtils.toString(
            Objects.requireNonNull(getClass().getResourceAsStream("/dumpblock.log")),
            StandardCharsets.UTF_8);
    dumptransTxsCommandOutput =
        IOUtils.toString(
            Objects.requireNonNull(getClass().getResourceAsStream("/dumptrans.log")),
            StandardCharsets.UTF_8);
  }

  @Test
  public void testParseLastPerformance() {
    log.info("=== Parse Last Command Performance Test ===");

    // Warm-up
    for (int i = 0; i < WARMUP_ITERATIONS; i++) {
      LiteClientParser.parseLast(lastCommandOutput);
    }

    // Test
    long[] executionTimes = new long[TEST_ITERATIONS];
    for (int i = 0; i < TEST_ITERATIONS; i++) {
      long startTime = System.nanoTime();
      LiteClientParser.parseLast(lastCommandOutput);
      long endTime = System.nanoTime();
      executionTimes[i] = endTime - startTime;
    }

    // Calculate and log results
    double avgTime = calculateAverage(executionTimes);
    log.info(
        "parseLast average execution time: {} ns ({} ms)",
        String.format("%.2f", avgTime),
        String.format("%.2f", avgTime / 1_000_000));
  }

  @Test
  public void testParseBySeqnoPerformance() throws IncompleteDump, ParsingError {
    log.info("=== Parse BySeqno Command Performance Test ===");

    // Warm-up
    for (int i = 0; i < WARMUP_ITERATIONS; i++) {
      try {
        LiteClientParser.parseBySeqno(bySeqCommandOutput);
      } catch (Exception e) {
        // Ignore exceptions during warm-up
      }
    }

    // Test
    long[] executionTimes = new long[TEST_ITERATIONS];
    for (int i = 0; i < TEST_ITERATIONS; i++) {
      long startTime = System.nanoTime();
      LiteClientParser.parseBySeqno(bySeqCommandOutput);
      long endTime = System.nanoTime();
      executionTimes[i] = endTime - startTime;
    }

    // Calculate and log results
    double avgTime = calculateAverage(executionTimes);
    log.info(
        "parseBySeqno average execution time: {} ns ({} ms)",
        String.format("%.2f", avgTime),
        String.format("%.2f", avgTime / 1_000_000));
  }

  @Test
  public void testParseDumpblockPerformance() throws IncompleteDump, ParsingError {
    log.info("=== Parse Dumpblock Command Performance Test ===");

    // Warm-up
    for (int i = 0; i < WARMUP_ITERATIONS; i++) {
      try {
        LiteClientParser.parseDumpblock(blockDump, false, false);
      } catch (Exception e) {
        // Ignore exceptions during warm-up
      }
    }

    // Test
    long[] executionTimes = new long[TEST_ITERATIONS];
    for (int i = 0; i < TEST_ITERATIONS; i++) {
      long startTime = System.nanoTime();
      LiteClientParser.parseDumpblock(blockDump, false, false);
      long endTime = System.nanoTime();
      executionTimes[i] = endTime - startTime;
    }

    // Calculate and log results
    double avgTime = calculateAverage(executionTimes);
    log.info(
        "parseDumpblock average execution time: {} ns ({} ms)",
        String.format("%.2f", avgTime),
        String.format("%.2f", avgTime / 1_000_000));
  }

  @Test
  public void testParseDumpTransPerformance() {
    log.info("=== Parse DumpTrans Command Performance Test ===");

    // Warm-up
    for (int i = 0; i < WARMUP_ITERATIONS; i++) {
      LiteClientParser.parseDumpTrans(dumptransTxsCommandOutput, true);
    }

    // Test
    long[] executionTimes = new long[TEST_ITERATIONS];
    for (int i = 0; i < TEST_ITERATIONS; i++) {
      long startTime = System.nanoTime();
      LiteClientParser.parseDumpTrans(dumptransTxsCommandOutput, true);
      long endTime = System.nanoTime();
      executionTimes[i] = endTime - startTime;
    }

    // Calculate and log results
    double avgTime = calculateAverage(executionTimes);
    log.info(
        "parseDumpTrans average execution time: {} ns ({} ms)",
        String.format("%.2f", avgTime),
        String.format("%.2f", avgTime / 1_000_000));
  }

  // Removed test for private methods

  // Removed test for private methods

  // Removed test for private methods

  /** Helper method to calculate the average of an array of longs. */
  private double calculateAverage(long[] values) {
    long sum = 0;
    for (long value : values) {
      sum += value;
    }
    return (double) sum / values.length;
  }
}
