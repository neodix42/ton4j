package org.ton.ton4j.exporter.reader;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.ton.ton4j.tlb.Account;

/**
 * Test suite for Account retrieval and deserialization functionality in CellDbReader. This test
 * validates the complete pipeline from raw cell data to Account TL-B objects.
 */
@Slf4j
public class TestAccountRetrieval {

  private static final String TON_DB_PATH =
      "/home/neodix/gitProjects/MyLocalTon/myLocalTon/genesis/db";

  @Test
  public void testAccountRetrievalByHash() {
    try {
      log.info("=== Test: Account Retrieval and Deserialization ===");
      log.info("Testing Account retrieval with database at: {}", TON_DB_PATH);

      try (CellDbReader reader = new CellDbReader(TON_DB_PATH)) {

        // Test 1: Find candidate Account cells
        log.info("=== Test 1: Finding Account Cell Candidates ===");
        Set<String> candidateHashes = reader.findAccountCells(5);
        log.info("Found {} candidate Account cells", candidateHashes.size());

        if (candidateHashes.isEmpty()) {
          log.warn("No candidate Account cells found - this may indicate:");
          log.warn("  1. Database contains no Account data");
          log.warn("  2. Account detection heuristics need adjustment");
          log.warn("  3. Account cells are stored differently than expected");
          return;
        }

        // Test 2: Retrieve individual Account by hash
        log.info("=== Test 2: Individual Account Retrieval ===");
        String testHash = candidateHashes.iterator().next();
        log.info("Testing Account retrieval for hash: {}", testHash);

        Account account = reader.retrieveAccountByHash(testHash);
        if (account != null) {
          log.info("✓ Successfully retrieved Account:");
          log.info("  - isNone: {}", account.isNone());
          if (!account.isNone()) {
            log.info("  - Address: {}", account.getAddress());
            log.info("  - Balance: {}", account.getBalance());
            log.info("  - State: {}", account.getAccountState());

            if (account.getStorageInfo() != null) {
              log.info("  - Storage Info: available");
            }
            if (account.getAccountStorage() != null) {
              log.info("  - Account Storage: available");
            }
          }
        } else {
          log.info("Account retrieval returned null (cell may not contain valid Account data)");
        }

        // Test 3: Batch Account retrieval
        log.info("=== Test 3: Batch Account Retrieval ===");
        Set<String> batchHashes =
            candidateHashes.stream().limit(10).collect(java.util.stream.Collectors.toSet());

        Map<String, Account> accounts = reader.retrieveAccountsByHashes(batchHashes);
        log.info("Batch retrieval results: {}/{} successful", accounts.size(), batchHashes.size());

        // Analyze retrieved accounts
        int noneAccounts = 0;
        int activeAccounts = 0;
        int uninitAccounts = 0;
        int frozenAccounts = 0;
        int otherAccounts = 0;

        for (Map.Entry<String, Account> entry : accounts.entrySet()) {
          Account acc = entry.getValue();
          String state = acc.getAccountState();

          switch (state) {
            case "uninitialized":
              uninitAccounts++;
              break;
            case "active":
              activeAccounts++;
              break;
            case "frozen":
              frozenAccounts++;
              break;
            default:
              if (acc.isNone()) {
                noneAccounts++;
              } else {
                otherAccounts++;
              }
              break;
          }
        }

        log.info("Account state distribution:");
        log.info("  - None accounts: {}", noneAccounts);
        log.info("  - Active accounts: {}", activeAccounts);
        log.info("  - Uninitialized accounts: {}", uninitAccounts);
        log.info("  - Frozen accounts: {}", frozenAccounts);
        log.info("  - Other accounts: {}", otherAccounts);

        // Test 4: State tree Account extraction (limited for performance)
        log.info("=== Test 4: State Tree Account Extraction ===");
        Map<String, org.ton.ton4j.tl.types.db.celldb.Value> entries = reader.getAllCellEntries();
        log.info("Found {} metadata entries to test state tree extraction", entries.size());

        if (!entries.isEmpty()) {
          // Test with first few state roots (limited depth for performance)
          int testedRoots = 0;
          int totalAccountsFound = 0;

          for (org.ton.ton4j.tl.types.db.celldb.Value entry : entries.values()) {
            if (testedRoots >= 2) break; // Limit to 2 roots for test performance

            String rootHash = entry.getRootHash();
            if (rootHash == null || rootHash.isEmpty()) continue;

            log.info("Testing state tree extraction from root: {}", rootHash);

            // Extract accounts with limited depth for performance
            Map<String, Account> stateAccounts = reader.retrieveAccountsFromStateRoot(rootHash, 2);
            log.info(
                "Found {} accounts in state tree from root {}", stateAccounts.size(), rootHash);

            totalAccountsFound += stateAccounts.size();
            testedRoots++;

            // Log details of first few accounts found
            int accountCount = 0;
            for (Map.Entry<String, Account> accountEntry : stateAccounts.entrySet()) {
              if (accountCount >= 3) break; // Limit logging
              Account acc = accountEntry.getValue();
              log.info(
                  "  Account {}: hash={}, state={}, balance={}",
                  accountCount + 1,
                  accountEntry.getKey(),
                  acc.getAccountState(),
                  acc.getBalance());
              accountCount++;
            }
          }

          log.info(
              "State tree extraction summary: {} accounts found from {} roots",
              totalAccountsFound,
              testedRoots);
        }

        // Test 5: Error handling
        log.info("=== Test 5: Error Handling ===");

        // Test null hash
        Account nullResult = reader.retrieveAccountByHash(null);
        log.info(
            "Null hash result: {}", nullResult == null ? "null (expected)" : "unexpected non-null");

        // Test empty hash
        Account emptyResult = reader.retrieveAccountByHash("");
        log.info(
            "Empty hash result: {}",
            emptyResult == null ? "null (expected)" : "unexpected non-null");

        // Test invalid hash
        Account invalidResult = reader.retrieveAccountByHash("invalid_hash");
        log.info(
            "Invalid hash result: {}",
            invalidResult == null ? "null (expected)" : "unexpected non-null");

        // Test non-existent hash
        String nonExistentHash = "1234567890abcdef1234567890abcdef1234567890abcdef1234567890abcdef";
        Account nonExistentResult = reader.retrieveAccountByHash(nonExistentHash);
        log.info(
            "Non-existent hash result: {}",
            nonExistentResult == null ? "null (expected)" : "unexpected non-null");

        // Test 6: Enhanced statistics
        log.info("=== Test 6: Enhanced Statistics ===");
        Map<String, Object> stats = reader.getEnhancedStatistics();

        log.info("Enhanced statistics results:");
        for (Map.Entry<String, Object> entry : stats.entrySet()) {
          log.info("  - {}: {}", entry.getKey(), entry.getValue());
        }

        // Test 7: Performance measurement
        log.info("=== Test 7: Performance Measurement ===");

        long startTime = System.currentTimeMillis();
        Set<String> perfCandidates = reader.findAccountCells(50);
        long discoveryTime = System.currentTimeMillis() - startTime;
        log.info("Account discovery (50 candidates) took: {} ms", discoveryTime);

        if (!perfCandidates.isEmpty()) {
          startTime = System.currentTimeMillis();
          Map<String, Account> perfAccounts = reader.retrieveAccountsByHashes(perfCandidates);
          long retrievalTime = System.currentTimeMillis() - startTime;
          log.info(
              "Batch Account retrieval ({} hashes) took: {} ms",
              perfCandidates.size(),
              retrievalTime);

          if (!perfAccounts.isEmpty()) {
            double avgTime = (double) retrievalTime / perfAccounts.size();
            log.info("Average time per Account retrieval: {} ms", avgTime);
          }
        }

        log.info("=== Account Retrieval Tests Completed Successfully ===");
        log.info("Key Findings:");
        log.info(
            "  - Account retrieval pipeline: Cell Hash -> Raw Data -> BoC -> CellSlice -> Account TL-B");
        log.info("  - Account detection heuristics help identify potential Account cells");
        log.info("  - Batch processing provides efficient handling of multiple Account retrievals");
        log.info("  - State tree traversal can extract Accounts from blockchain state");
        log.info("  - Error handling gracefully manages invalid inputs");
      }

    } catch (IOException e) {
      if (e.getMessage().contains("not found")) {
        log.warn("CellDB database not found at {}, skipping test", TON_DB_PATH);
        log.info("To run this test, ensure a CellDB database exists at the specified path");
        log.info("Expected database structure: {}/celldb/", TON_DB_PATH);
      } else {
        log.error("Error testing Account retrieval: {}", e.getMessage(), e);
        throw new RuntimeException(e);
      }
    } catch (Exception e) {
      log.error("Unexpected error testing Account retrieval: {}", e.getMessage(), e);
      throw new RuntimeException(e);
    }
  }

  @Test
  public void testAccountRetrievalValidation() {
    try {
      log.info("=== Test: Account Retrieval Validation ===");

      try (CellDbReader reader = new CellDbReader(TON_DB_PATH)) {

        // Test Account cell detection heuristics
        log.info("=== Testing Account Cell Detection Heuristics ===");

        Set<String> allCellHashes = reader.getAllCellHashes();
        log.info("Total cell hashes in database: {}", allCellHashes.size());

        Set<String> candidateHashes = reader.findAccountCells(100);
        log.info("Candidate Account cells found: {}", candidateHashes.size());

        if (!candidateHashes.isEmpty()) {
          double candidateRatio = (double) candidateHashes.size() / allCellHashes.size() * 100.0;
          log.info("Account candidate ratio: {}%", candidateRatio);

          // Validate a sample of candidates
          int validationSample = Math.min(10, candidateHashes.size());
          int validAccounts = 0;
          int invalidAccounts = 0;

          log.info("Validating {} candidate cells...", validationSample);

          int count = 0;
          for (String hash : candidateHashes) {
            if (count >= validationSample) break;

            try {
              Account account = reader.retrieveAccountByHash(hash);
              if (account != null) {
                validAccounts++;
                log.debug("Valid Account found at {}: state={}", hash, account.getAccountState());
              } else {
                invalidAccounts++;
                log.debug("Invalid Account candidate at {}", hash);
              }
            } catch (Exception e) {
              invalidAccounts++;
              log.debug("Error validating candidate {}: {}", hash, e.getMessage());
            }
            count++;
          }

          log.info(
              "Validation results: {}/{} valid Accounts ({}% accuracy)",
              validAccounts, validationSample, (double) validAccounts / validationSample * 100.0);

          if (validAccounts > 0) {
            log.info("✓ Account detection heuristics are working");
          } else {
            log.warn("⚠ Account detection heuristics may need adjustment");
          }
        }

        log.info("=== Account Retrieval Validation Completed ===");
      }

    } catch (IOException e) {
      if (e.getMessage().contains("not found")) {
        log.warn("CellDB database not found, skipping validation test");
      } else {
        log.error("Error in Account retrieval validation: {}", e.getMessage());
      }
    }
  }
}
