package org.ton.ton4j.exporter.reader;

import static org.junit.Assert.*;

import java.io.IOException;
import java.util.*;
import lombok.extern.slf4j.Slf4j;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.ton.ton4j.address.Address;
import org.ton.ton4j.tlb.ShardAccount;
import org.ton.ton4j.tlb.ShardAccounts;

/**
 * Test class for CellDbReaderOptimized demonstrating performance optimizations and comparing
 * against the original sequential implementation.
 */
@Slf4j
public class TestCellDbReaderOptimized {

  private static final String TON_DB_ROOT_PATH =
      "/home/neodix/gitProjects/MyLocalTon/myLocalTon/genesis/db";

  private CellDbReaderOptimized optimizedReader;
  private CellDbReader originalReader;

  @Before
  public void setUp() throws IOException {
    log.info("Setting up optimized CellDB reader tests");

    optimizedReader = new CellDbReaderOptimized(TON_DB_ROOT_PATH);
    originalReader = new CellDbReader(TON_DB_ROOT_PATH);
  }

  @After
  public void tearDown() throws IOException {
    if (optimizedReader != null) {
      optimizedReader.close();
    }
    if (originalReader != null) {
      originalReader.close();
    }
  }

  @Test
  public void testDirectStateRootAccess() throws IOException {
    log.info("=== Testing Strategy 1: Direct State Root Access ===");

    // Get a sample state root hash from the original reader
    Map<String, org.ton.ton4j.tl.types.db.celldb.Value> entries =
        originalReader.getAllCellEntries();
    String sampleStateRoot = null;

    for (org.ton.ton4j.tl.types.db.celldb.Value entry : entries.values()) {
      if (entry.getRootHash() != null && !entry.getRootHash().isEmpty()) {
        sampleStateRoot = entry.getRootHash();
        break;
      }
    }

    assertNotNull("Should find at least one state root", sampleStateRoot);

    // Test direct access
    long startTime = System.currentTimeMillis();
    ShardAccounts shardAccounts = optimizedReader.getShardAccountsDirect(sampleStateRoot);
    long directTime = System.currentTimeMillis() - startTime;

    log.info("Direct state root access took {}ms", directTime);
    log.info("ShardAccounts result: {}", shardAccounts != null ? "Found" : "Not found");

    // Test caching - second access should be faster
    startTime = System.currentTimeMillis();
    ShardAccounts cachedAccounts = optimizedReader.getShardAccountsDirect(sampleStateRoot);
    long cachedTime = System.currentTimeMillis() - startTime;

    log.info("Cached state root access took {}ms", cachedTime);
    assertTrue("Cached access should be faster", cachedTime <= directTime);
  }

  @Test
  public void testAccountCaching() throws IOException {
    log.info("=== Testing Strategy 2: Account Caching ===");

    // Clear caches to start fresh
    optimizedReader.clearCaches();

    // Get performance stats before
    Map<String, Object> statsBefore = optimizedReader.getPerformanceStatistics();
    log.info("Stats before: {}", statsBefore);

    // Create test addresses
    Set<Address> testAddresses = createTestAddresses();

    // Get a sample state root
    String sampleStateRoot = getSampleStateRoot();
    if (sampleStateRoot == null) {
      log.warn("No sample state root found, skipping test");
      return;
    }

    // First access - should be cache miss
    long startTime = System.currentTimeMillis();
    for (Address address : testAddresses) {
      ShardAccount account = optimizedReader.getAccountByAddress(address, sampleStateRoot);
      log.debug(
          "Account lookup for {}: {}", address.toRaw(), account != null ? "Found" : "Not found");
    }
    long firstAccessTime = System.currentTimeMillis() - startTime;

    // Second access - should be cache hit (even for null results)
    startTime = System.currentTimeMillis();
    for (Address address : testAddresses) {
      ShardAccount account = optimizedReader.getAccountByAddress(address, sampleStateRoot);
      log.debug(
          "Cached lookup for {}: {}", address.toRaw(), account != null ? "Found" : "Not found");
    }
    long cachedAccessTime = System.currentTimeMillis() - startTime;

    // Get performance stats after
    Map<String, Object> statsAfter = optimizedReader.getPerformanceStatistics();
    log.info("Stats after: {}", statsAfter);

    log.info("First access time: {}ms", firstAccessTime);
    log.info("Cached access time: {}ms", cachedAccessTime);

    // Verify caching is working - we should have cache hits from the second round
    long cacheHits = (Long) statsAfter.get("cache_hits");
    long cacheMisses = (Long) statsAfter.get("cache_misses");

    // We expect cache misses equal to the number of test addresses (first access)
    // and cache hits equal to the number of test addresses (second access)
    assertEquals(
        "Should have cache misses equal to test addresses", testAddresses.size(), cacheMisses);
    assertEquals("Should have cache hits equal to test addresses", testAddresses.size(), cacheHits);
    assertTrue("Cached access should be faster or equal", cachedAccessTime <= firstAccessTime);

    double hitRatio = (Double) statsAfter.get("hit_ratio");
    assertEquals("Hit ratio should be 0.5", 0.5, hitRatio, 0.01);
    log.info("Cache hit ratio: {}", hitRatio);
  }

  @Test
  public void testBatchProcessing() throws IOException {
    log.info("=== Testing Strategy 4: Batch Processing with Filtering ===");

    String sampleStateRoot = getSampleStateRoot();
    if (sampleStateRoot == null) {
      log.warn("No sample state root found, skipping test");
      return;
    }

    Set<Address> targetAddresses = createTestAddresses();

    // Test batch processing
    long startTime = System.currentTimeMillis();
    Map<Address, ShardAccount> batchResults =
        optimizedReader.getSpecificAccounts(sampleStateRoot, targetAddresses);
    long batchTime = System.currentTimeMillis() - startTime;

    // Test individual processing for comparison
    startTime = System.currentTimeMillis();
    Map<Address, ShardAccount> individualResults = new HashMap<>();
    for (Address address : targetAddresses) {
      ShardAccount account = optimizedReader.getAccountByAddress(address, sampleStateRoot);
      if (account != null) {
        individualResults.put(address, account);
      }
    }
    long individualTime = System.currentTimeMillis() - startTime;

    log.info("Batch processing: {} accounts in {}ms", batchResults.size(), batchTime);
    log.info(
        "Individual processing: {} accounts in {}ms", individualResults.size(), individualTime);

    // Results should be consistent
    assertEquals(
        "Batch and individual results should match", batchResults.size(), individualResults.size());
  }

  @Test
  public void testStateRootIndexing() throws IOException {
    log.info("=== Testing Strategy 5: State Root Indexing ===");

    // Clear caches and build index
    optimizedReader.clearCaches();

    long startTime = System.currentTimeMillis();
    optimizedReader.buildStateRootIndex();
    long indexBuildTime = System.currentTimeMillis() - startTime;

    log.info("Index build time: {}ms", indexBuildTime);

    Map<String, Object> stats = optimizedReader.getPerformanceStatistics();
    boolean indexBuilt = (Boolean) stats.get("index_built");
    int indexedRoots = (Integer) stats.get("indexed_state_roots");

    assertTrue("Index should be built", indexBuilt);
    assertTrue("Should have indexed some state roots", indexedRoots > 0);

    log.info("Indexed {} state roots", indexedRoots);

    // Test sequence number lookup
    if (indexedRoots > 0) {
      // Try to get accounts by sequence number
      ShardAccounts accountsBySeqno = optimizedReader.getShardAccountsBySeqno(1000L);
      log.info("Accounts by seqno 1000: {}", accountsBySeqno != null ? "Found" : "Not found");

      // Test with address and sequence number
      Address testAddress = createTestAddresses().iterator().next();
      ShardAccount accountBySeqno = optimizedReader.getAccountByAddressAndSeqno(testAddress, 1000L);
      log.info(
          "Account {} by seqno 1000: {}",
          testAddress.toRaw(),
          accountBySeqno != null ? "Found" : "Not found");
    }
  }

  @Test
  public void testPerformanceComparison() throws IOException {
    log.info("=== Testing Performance Comparison: Optimized vs Original ===");

    // Get sample data
    String sampleStateRoot = getSampleStateRoot();
    if (sampleStateRoot == null) {
      log.warn("No sample state root found, skipping performance test");
      return;
    }

    Set<Address> testAddresses = createTestAddresses();

    // Test original implementation (if we had a way to get accounts)
    // For now, just test the optimized version multiple times

    // Clear caches for fair comparison
    optimizedReader.clearCaches();

    // First run (cold cache)
    long startTime = System.currentTimeMillis();
    Map<Address, ShardAccount> coldResults =
        optimizedReader.getSpecificAccounts(sampleStateRoot, testAddresses);
    long coldTime = System.currentTimeMillis() - startTime;

    // Second run (warm cache)
    startTime = System.currentTimeMillis();
    Map<Address, ShardAccount> warmResults =
        optimizedReader.getSpecificAccounts(sampleStateRoot, testAddresses);
    long warmTime = System.currentTimeMillis() - startTime;

    log.info("Cold cache: {} accounts in {}ms", coldResults.size(), coldTime);
    log.info("Warm cache: {} accounts in {}ms", warmResults.size(), warmTime);

    // Warm cache should be significantly faster
    if (warmTime > 0) {
      double speedup = (double) coldTime / warmTime;
      log.info("Cache speedup: {}x", speedup);
      assertTrue("Warm cache should provide speedup", speedup >= 1.0);
    }

    // Results should be consistent
    assertEquals("Cold and warm results should match", coldResults.size(), warmResults.size());
  }

  @Test
  public void testCacheManagement() throws IOException {
    log.info("=== Testing Cache Management ===");

    optimizedReader.clearCaches();

    Map<String, Object> initialStats = optimizedReader.getPerformanceStatistics();
    assertEquals("Account cache should be empty", 0, initialStats.get("account_cache_size"));
    assertEquals("State cache should be empty", 0, initialStats.get("state_cache_size"));

    // Populate caches
    String sampleStateRoot = getSampleStateRoot();
    if (sampleStateRoot != null) {
      Set<Address> testAddresses = createTestAddresses();
      optimizedReader.getSpecificAccounts(sampleStateRoot, testAddresses);
    }

    Map<String, Object> populatedStats = optimizedReader.getPerformanceStatistics();
    int accountCacheSize = (Integer) populatedStats.get("account_cache_size");
    int stateCacheSize = (Integer) populatedStats.get("state_cache_size");

    log.info("Account cache size: {}", accountCacheSize);
    log.info("State cache size: {}", stateCacheSize);

    // Clear caches again
    optimizedReader.clearCaches();

    Map<String, Object> clearedStats = optimizedReader.getPerformanceStatistics();
    assertEquals("Account cache should be cleared", 0, clearedStats.get("account_cache_size"));
    assertEquals("State cache should be cleared", 0, clearedStats.get("state_cache_size"));
    assertEquals("Cache hits should be reset", 0L, clearedStats.get("cache_hits"));
    assertEquals("Cache misses should be reset", 0L, clearedStats.get("cache_misses"));
  }

  @Test
  public void testErrorHandling() {
    log.info("=== Testing Error Handling ===");

    // Test with invalid state root
    ShardAccounts invalidResult = optimizedReader.getShardAccountsDirect("invalid_hash");
    assertNull("Invalid state root should return null", invalidResult);

    // Test with null parameters
    ShardAccount nullResult = optimizedReader.getAccountByAddress(null, "some_hash");
    assertNull("Null address should return null", nullResult);

    nullResult = optimizedReader.getAccountByAddress(createTestAddresses().iterator().next(), null);
    assertNull("Null state root should return null", nullResult);

    // Test with empty target addresses
    Map<Address, ShardAccount> emptyResult =
        optimizedReader.getSpecificAccounts("some_hash", new HashSet<>());
    assertTrue("Empty addresses should return empty map", emptyResult.isEmpty());
  }

  @Test
  public void testStatisticsAccuracy() throws IOException {
    log.info("=== Testing Statistics Accuracy ===");

    optimizedReader.clearCaches();

    String sampleStateRoot = getSampleStateRoot();
    if (sampleStateRoot == null) {
      log.warn("No sample state root found, skipping statistics test");
      return;
    }

    Address testAddress = createTestAddresses().iterator().next();

    // First access - should be cache miss
    ShardAccount result1 = optimizedReader.getAccountByAddress(testAddress, sampleStateRoot);

    Map<String, Object> stats1 = optimizedReader.getPerformanceStatistics();
    assertEquals("Should have 1 cache miss", 1L, stats1.get("cache_misses"));
    assertEquals("Should have 0 cache hits", 0L, stats1.get("cache_hits"));

    // Second access - should be cache hit (even for null results)
    ShardAccount result2 = optimizedReader.getAccountByAddress(testAddress, sampleStateRoot);

    Map<String, Object> stats2 = optimizedReader.getPerformanceStatistics();
    assertEquals("Should still have 1 cache miss", 1L, stats2.get("cache_misses"));
    assertEquals("Should have 1 cache hit", 1L, stats2.get("cache_hits"));

    double hitRatio = (Double) stats2.get("hit_ratio");
    assertEquals("Hit ratio should be 0.5", 0.5, hitRatio, 0.01);

    // Results should be consistent (both null or both non-null)
    assertEquals("Results should be consistent", result1, result2);

    log.info("Final statistics: {}", stats2);
  }

  // Helper methods

  private Set<Address> createTestAddresses() {
    Set<Address> addresses = new HashSet<>();

    try {
      // Add some example addresses (these would be real TON addresses in actual tests)
      addresses.add(Address.of("EQD4FPq-PRDieyQKkizFTRtSDyucUIqrj0v_zXJmqaDp6_0t"));
      addresses.add(Address.of("EQAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAM9c"));
      addresses.add(Address.of("EQBvW8Z5huBkMJYdnfAEM5JqTNkuWX3diqYENkWsIL0XggGG"));
    } catch (Exception e) {
      log.warn("Could not create test addresses: {}", e.getMessage());
      // Create a minimal set with just one address
      try {
        addresses.add(Address.of("EQAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAM9c"));
      } catch (Exception e2) {
        log.error("Could not create even basic test address", e2);
      }
    }

    return addresses;
  }

  private String getSampleStateRoot() throws IOException {
    Map<String, org.ton.ton4j.tl.types.db.celldb.Value> entries =
        originalReader.getAllCellEntries();

    for (org.ton.ton4j.tl.types.db.celldb.Value entry : entries.values()) {
      if (entry.getRootHash() != null && !entry.getRootHash().isEmpty()) {
        return entry.getRootHash();
      }
    }

    return null;
  }

  @Test
  public void testConfigurationDefaults() {
    log.info("=== Testing Configuration Defaults ===");

    Map<String, Object> stats = optimizedReader.getPerformanceStatistics();

    assertNotNull("Statistics should not be null", stats);
    assertTrue("Should have cache statistics", stats.containsKey("cache_hits"));
    assertTrue("Should have cache statistics", stats.containsKey("cache_misses"));
    assertTrue("Should have hit ratio", stats.containsKey("hit_ratio"));

    // Initial values should be zero
    assertEquals("Initial cache hits should be 0", 0L, stats.get("cache_hits"));
    assertEquals("Initial cache misses should be 0", 0L, stats.get("cache_misses"));
    assertEquals("Initial hit ratio should be 0.0", 0.0, stats.get("hit_ratio"));

    log.info("Configuration test passed: {}", stats);
  }

  @Test
  public void testIndexBuildingIdempotency() throws IOException {
    log.info("=== Testing Index Building Idempotency ===");

    optimizedReader.clearCaches();

    // Build index first time
    long startTime = System.currentTimeMillis();
    optimizedReader.buildStateRootIndex();
    long firstBuildTime = System.currentTimeMillis() - startTime;

    Map<String, Object> stats1 = optimizedReader.getPerformanceStatistics();
    boolean indexBuilt1 = (Boolean) stats1.get("index_built");
    int indexedRoots1 = (Integer) stats1.get("indexed_state_roots");

    // Build index second time - should be no-op
    startTime = System.currentTimeMillis();
    optimizedReader.buildStateRootIndex();
    long secondBuildTime = System.currentTimeMillis() - startTime;

    Map<String, Object> stats2 = optimizedReader.getPerformanceStatistics();
    boolean indexBuilt2 = (Boolean) stats2.get("index_built");
    int indexedRoots2 = (Integer) stats2.get("indexed_state_roots");

    assertTrue("Index should be built after first call", indexBuilt1);
    assertTrue("Index should still be built after second call", indexBuilt2);
    assertEquals("Indexed roots count should be same", indexedRoots1, indexedRoots2);
    assertTrue("Second build should be much faster (no-op)", secondBuildTime < firstBuildTime);

    log.info("First build: {}ms, Second build: {}ms", firstBuildTime, secondBuildTime);
    log.info("Indexed {} state roots", indexedRoots1);
  }
}
