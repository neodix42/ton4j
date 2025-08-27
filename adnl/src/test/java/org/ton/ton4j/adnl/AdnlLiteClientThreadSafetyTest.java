package org.ton.ton4j.adnl;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.ton.ton4j.tl.liteserver.responses.MasterchainInfo;
import org.ton.ton4j.utils.Utils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/** Test class to verify thread safety of AdnlLiteClient */
@Slf4j
public class AdnlLiteClientThreadSafetyTest {

  @Test
  public void testConcurrentQueries() throws Exception {

    AdnlLiteClient client =
        AdnlLiteClient.builder()
            .configUrl(Utils.getGlobalConfigUrlTestnetGithub())
            .maxRetries(3)
            .queryTimeout(30)
            .build();

    try {
      // Number of concurrent threads
      int threadCount = 10;
      // Number of queries per thread
      int queriesPerThread = 5;

      ExecutorService executor = Executors.newFixedThreadPool(threadCount);
      List<Future<Boolean>> futures = new ArrayList<>();
      AtomicInteger successCount = new AtomicInteger(0);
      AtomicInteger errorCount = new AtomicInteger(0);

      log.info("Starting {} threads with {} queries each", threadCount, queriesPerThread);

      // Submit tasks to executor
      for (int i = 0; i < threadCount; i++) {
        final int threadId = i;
        Future<Boolean> future =
            executor.submit(
                () -> {
                  boolean allSuccessful = true;

                  for (int j = 0; j < queriesPerThread; j++) {
                    try {
                      log.info("Thread {} executing query {}", threadId, j + 1);

                      // Execute getMasterchainInfo query
                      MasterchainInfo info = client.getMasterchainInfo();

                      // Verify response
                      assertNotNull(info, "MasterchainInfo should not be null");
                      assertNotNull(info.getLast(), "Last block should not be null");
                      assertTrue(info.getLast().getSeqno() > 0, "Seqno should be positive");

                      successCount.incrementAndGet();
                      log.info(
                          "Thread {} query {} successful - seqno: {}",
                          threadId,
                          j + 1,
                          info.getLast().getSeqno());

                      // Small delay between queries to simulate real usage
                      Thread.sleep(500);

                    } catch (Exception e) {
                      log.error("Thread {} query {} failed: {}", threadId, j + 1, e.getMessage());
                      errorCount.incrementAndGet();
                      allSuccessful = false;
                    }
                  }

                  return allSuccessful;
                });

        futures.add(future);
      }
      executor.shutdown();
      executor.awaitTermination(10, TimeUnit.SECONDS);

      // Log results
      int totalQueries = threadCount * queriesPerThread;
      log.info("Test completed:");
      log.info("Total queries: {}", totalQueries);
      log.info("Successful queries: {}", successCount.get());
      log.info("Failed queries: {}", errorCount.get());
      log.info("Success rate: {}%", (successCount.get() * 100.0) / totalQueries);

      // Assertions
      assertTrue(successCount.get() > 0, "At least some queries should succeed");

      // We expect most queries to succeed, but allow for some network issues
      double successRate = (successCount.get() * 100.0) / totalQueries;
      assertTrue(
          successRate >= 80.0,
          String.format("Success rate should be at least 80%%, but was %.1f%%", successRate));

      log.info("Thread safety test passed!");

    } finally {
      client.close();
    }
  }

  @Test
  public void testSequentialVsConcurrentPerformance() throws Exception {
    AdnlLiteClient client =
        AdnlLiteClient.builder()
            .configUrl(Utils.getGlobalConfigUrlTestnetGithub())
            .maxRetries(3)
            .queryTimeout(30)
            .build();

    try {
      int queryCount = 10;

      // Test sequential execution
      log.info("Testing sequential execution of {} queries", queryCount);
      long sequentialStart = System.currentTimeMillis();

      for (int i = 0; i < queryCount; i++) {
        MasterchainInfo info = client.getMasterchainInfo();
        assertNotNull(info);
        log.info("Sequential query {} completed - seqno: {}", i + 1, info.getLast().getSeqno());
      }

      long sequentialTime = System.currentTimeMillis() - sequentialStart;
      log.info("Sequential execution took {} ms", sequentialTime);

      // Test concurrent execution
      log.info("Testing concurrent execution of {} queries", queryCount);
      long concurrentStart = System.currentTimeMillis();

      ExecutorService executor = Executors.newFixedThreadPool(queryCount);
      List<Future<MasterchainInfo>> futures = new ArrayList<>();

      for (int i = 0; i < queryCount; i++) {
        final int queryId = i;
        Future<MasterchainInfo> future =
            executor.submit(
                () -> {
                  MasterchainInfo info = client.getMasterchainInfo();
                  log.info(
                      "Concurrent query {} completed - seqno: {}",
                      queryId + 1,
                      info.getLast().getSeqno());
                  return info;
                });
        futures.add(future);
      }

      // Wait for all concurrent queries to complete
      for (Future<MasterchainInfo> future : futures) {
        MasterchainInfo info = future.get(30, TimeUnit.SECONDS);
        assertNotNull(info);
      }

      executor.shutdown();
      executor.awaitTermination(10, TimeUnit.SECONDS);

      long concurrentTime = System.currentTimeMillis() - concurrentStart;
      log.info("Concurrent execution took {} ms", concurrentTime);

      // Concurrent execution should be faster (or at least not significantly slower)
      double speedup = (double) sequentialTime / concurrentTime;
      log.info("Speedup factor: {:.2f}x", speedup);

      // We expect some speedup, but network latency might limit it
      assertTrue(
          speedup >= 0.8,
          String.format(
              "Concurrent execution should not be significantly slower than sequential. Speedup: %.2fx",
              speedup));

      log.info("Performance test passed!");

    } finally {
      client.close();
    }
  }
}
