package org.ton.ton4j.exporter.reader;

import java.io.IOException;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.ton.ton4j.cell.Cell;
import org.ton.ton4j.tl.types.db.block.BlockIdExt;

@Slf4j
public class TestStateDbReader {

  public static final String TON_DB_ROOT_PATH =
      "/home/neodix/gitProjects/MyLocalTon/myLocalTon/genesis/db";

  @Test
  public void testReadStateDb() throws IOException {
    // Update this path to point to your actual TON database
    String dbPath = TON_DB_ROOT_PATH;

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
}
