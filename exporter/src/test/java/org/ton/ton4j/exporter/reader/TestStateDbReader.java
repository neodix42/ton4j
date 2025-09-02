package org.ton.ton4j.exporter.reader;

import java.io.IOException;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.ton.ton4j.cell.Cell;
import org.ton.ton4j.tl.types.db.block.BlockIdExt;

@Slf4j
public class TestStateDbReader {

  @Test
  public void testReadStateDb() throws IOException {
    // Update this path to point to your actual TON database
    String dbPath = "/home/neodix/gitProjects/MyLocalTon/myLocalTon/genesis/db";

    log.info("Opening State database: {}", dbPath);

    try (StateDbReader stateReader = new StateDbReader(dbPath)) {
      // Get all state file keys
      List<String> stateKeys = stateReader.getStateFileKeys();
      log.info("Found {} state files", stateKeys.size());

      // Show first 10 state files
      log.info("First 10 state files:");
      for (int i = 0; i < Math.min(10, stateKeys.size()); i++) {
        String key = stateKeys.get(i);
        StateDbReader.StateFileInfo info = stateReader.getStateFileInfo(key);
        log.info("  {}: {}", key, info);
      }

      // Analyze state files by type
      analyzeStateFilesByType(stateReader);

      // Analyze state files by workchain
      analyzeStateFilesByWorkchain(stateReader);

      // Try to read some state files
      readSampleStateFiles(stateReader, stateKeys);

      // Test specific queries
      testSpecificQueries(stateReader);
    }
  }

  private void analyzeStateFilesByType(StateDbReader stateReader) {
    log.info("Analyzing state files by type:");

    for (StateDbReader.StateFileType type : StateDbReader.StateFileType.values()) {
      List<StateDbReader.StateFileInfo> files = stateReader.getStateFilesByType(type);
      log.info("  {}: {} files", type, files.size());

      // Show sample files for each type
      if (!files.isEmpty()) {
        log.info("    Sample files:");
        for (int i = 0; i < Math.min(3, files.size()); i++) {
          StateDbReader.StateFileInfo info = files.get(i);
          log.info("      {}", info);
        }
      }
    }
  }

  private void analyzeStateFilesByWorkchain(StateDbReader stateReader) {
    log.info("Analyzing state files by workchain:");

    // Check common workchains
    int[] workchains = {-1, 0}; // Masterchain and basechain

    for (int workchain : workchains) {
      List<StateDbReader.StateFileInfo> files = stateReader.getStateFilesByWorkchain(workchain);
      log.info("  Workchain {}: {} files", workchain, files.size());

      if (!files.isEmpty()) {
        // Show sequence number range
        long minSeqno = files.stream().mapToLong(f -> f.seqno).min().orElse(0);
        long maxSeqno = files.stream().mapToLong(f -> f.seqno).max().orElse(0);
        log.info("    Sequence number range: {} - {}", minSeqno, maxSeqno);
      }
    }
  }

  private void readSampleStateFiles(StateDbReader stateReader, List<String> stateKeys) {
    log.info("Reading sample state files:");

    int samplesRead = 0;
    int maxSamples = 5;

    for (String key : stateKeys) {
      if (samplesRead >= maxSamples) {
        break;
      }

      try {
        StateDbReader.StateFileInfo info = stateReader.getStateFileInfo(key);
        byte[] data = stateReader.readStateFile(key);

        if (data != null) {
          log.info("  {}: {} bytes, type={}", key, data.length, info.type);

          // Try to parse as Cell
          try {
            Cell cell = stateReader.readStateFileAsCell(key);
            if (cell != null) {
              log.info(
                  "    Successfully parsed as Cell: {} bits, {} refs",
                  cell.getBits().getUsedBits(),
                  cell.getRefs().size());
            } else {
              log.info("    Could not parse as Cell");
            }
          } catch (Exception e) {
            log.info("    Cell parsing failed: {}", e.getMessage());
          }

          samplesRead++;
        } else {
          log.warn("  {}: Could not read file", key);
        }
      } catch (IOException e) {
        log.warn("  {}: Error reading file: {}", key, e.getMessage());
      }
    }
  }

  private void testSpecificQueries(StateDbReader stateReader) {
    log.info("Testing specific queries:");

    // Test sequence number range query
    List<StateDbReader.StateFileInfo> recentStates = stateReader.getStateFilesBySeqnoRange(0, 1000);
    log.info("  States with seqno 0-1000: {} files", recentStates.size());

    // Test zero state lookup
    List<StateDbReader.StateFileInfo> zeroStates =
        stateReader.getStateFilesByType(StateDbReader.StateFileType.ZERO_STATE);
    if (!zeroStates.isEmpty()) {
      log.info("  Found {} zero states", zeroStates.size());

      // Try to read the first zero state
      StateDbReader.StateFileInfo zeroState = zeroStates.get(0);
      try {
        BlockIdExt blockId =
            BlockIdExt.builder()
                .workchain(zeroState.workchain)
                .shard(Long.parseUnsignedLong(zeroState.shard, 16))
                .seqno(0)
                .build();

        byte[] zeroStateData = stateReader.readZeroState(blockId);
        if (zeroStateData != null) {
          log.info("    Successfully read zero state: {} bytes", zeroStateData.length);
        } else {
          log.info("    Could not read zero state");
        }
      } catch (Exception e) {
        log.warn("    Error reading zero state: {}", e.getMessage());
      }
    }

    // Test persistent state lookup
    List<StateDbReader.StateFileInfo> persistentStates =
        stateReader.getStateFilesByType(StateDbReader.StateFileType.PERSISTENT_STATE);
    if (!persistentStates.isEmpty()) {
      log.info("  Found {} persistent states", persistentStates.size());

      // Try to read the first persistent state
      StateDbReader.StateFileInfo persistentState = persistentStates.get(0);
      try {
        BlockIdExt blockId =
            BlockIdExt.builder()
                .workchain(persistentState.workchain)
                .shard(Long.parseUnsignedLong(persistentState.shard, 16))
                .seqno(persistentState.seqno)
                .build();

        BlockIdExt mcBlockId =
            BlockIdExt.builder()
                .workchain(-1)
                .shard(-9223372036854775808L) // 0x8000000000000000
                .seqno(persistentState.seqno)
                .build();

        byte[] persistentStateData = stateReader.readPersistentState(blockId, mcBlockId);
        if (persistentStateData != null) {
          log.info("    Successfully read persistent state: {} bytes", persistentStateData.length);
        } else {
          log.info("    Could not read persistent state");
        }
      } catch (Exception e) {
        log.warn("    Error reading persistent state: {}", e.getMessage());
      }
    }
  }

  @Test
  public void testStateFilenamePatterns() {
    log.info("Testing state filename parsing patterns:");

    // Test various filename patterns
    String[] testFilenames = {
      "zerostate_-1_8000000000000000_A6A0BD6608672B11B79538A50B2204E748305C12AA0DED9C16CF0006CE3AF8DB_4AC7A727E36B690590C64D1D8432E616E0A2B5B8B8B8B8B8B8B8B8B8B8B8B8B8",
      "state_0_8000000000000000_123_B1234567890ABCDEF1234567890ABCDEF1234567890ABCDEF1234567890ABCDEF_C1234567890ABCDEF1234567890ABCDEF1234567890ABCDEF1234567890ABCDEF",
      "split_account_0_8000000000000000_456_4000000000000000_D1234567890ABCDEF1234567890ABCDEF1234567890ABCDEF1234567890ABCDEF_E1234567890ABCDEF1234567890ABCDEF1234567890ABCDEF1234567890ABCDEF",
      "split_state_0_8000000000000000_789_F1234567890ABCDEF1234567890ABCDEF1234567890ABCDEF1234567890ABCDEF_A1234567890ABCDEF1234567890ABCDEF1234567890ABCDEF1234567890ABCDEF",
      "zerostate_0_557887380CA63F0710D05358E8F65FC14AFDE95C1B4ECB393BB609F573980EDE",
      "zerostate_-1_19E74429499CC327B6F21523CCC78492D5C6DFB228F0FE534B7F40BA87359B74",
      "invalid_filename_format",
      "state_invalid_format"
    };

    // Create a temporary StateDbReader to test filename parsing
    try {
      // Create a temporary directory for testing
      String tempDir = System.getProperty("java.io.tmpdir") + "/test_state_db";
      java.nio.file.Files.createDirectories(java.nio.file.Paths.get(tempDir, "archive", "states"));

      // We can't directly test the private parseStateFilename method,
      // but we can test the overall functionality by creating test files
      for (String filename : testFilenames) {
        log.info("Testing filename: {}", filename);

        // Create empty test file
        java.nio.file.Path testFile =
            java.nio.file.Paths.get(tempDir, "archive", "states", filename);
        try {
          java.nio.file.Files.write(testFile, new byte[0]);
        } catch (Exception e) {
          log.warn("Could not create test file {}: {}", filename, e.getMessage());
          continue;
        }
      }

      // Test with the temporary directory
      try (StateDbReader testReader = new StateDbReader(tempDir)) {
        List<String> discoveredFiles = testReader.getStateFileKeys();
        log.info(
            "Discovered {} valid state files out of {} test files",
            discoveredFiles.size(),
            testFilenames.length);

        for (String discoveredFile : discoveredFiles) {
          StateDbReader.StateFileInfo info = testReader.getStateFileInfo(discoveredFile);
          log.info("  {}: {}", discoveredFile, info);
        }
      }

      // Clean up test files
      try {
        java.nio.file.Files.walk(java.nio.file.Paths.get(tempDir))
            .sorted(java.util.Comparator.reverseOrder())
            .map(java.nio.file.Path::toFile)
            .forEach(java.io.File::delete);
      } catch (Exception e) {
        log.warn("Could not clean up test directory: {}", e.getMessage());
      }

    } catch (Exception e) {
      log.error("Error in filename pattern test: {}", e.getMessage());
    }
  }
}
