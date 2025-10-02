package org.ton.ton4j.exporter.reader;

import static org.ton.ton4j.exporter.reader.StateDbReader.*;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.ton.ton4j.cell.Cell;
import org.ton.ton4j.exporter.types.StateFileInfo;
import org.ton.ton4j.exporter.types.StateFileType;
import org.ton.ton4j.tl.types.db.block.BlockIdExt;
import org.ton.ton4j.tl.types.db.state.GcBlockId;
import org.ton.ton4j.tl.types.db.state.InitBlockId;
import org.ton.ton4j.tl.types.db.state.PersistentStateDescriptionShards;
import org.ton.ton4j.tl.types.db.state.PersistentStateDescriptionsList;
import org.ton.ton4j.tl.types.db.state.key.PersistentStateDescriptionShardsKey;
import org.ton.ton4j.utils.Utils;

@Slf4j
public class TestStateDbReader {

  public static final String TON_DB_ROOT_PATH =
      "/home/neodix/gitProjects/MyLocalTon/myLocalTon/genesis/db";

  @Test
  public void testStateDbReader() throws IOException {
    //    InitBlockId initBlockIdA = InitBlockId.builder().build();
    //    log.info("initBlockIdA: {}", Utils.sha256(InitBlockId.builder().build().serialize()));

    log.info("initBlockKeyHash {}", Utils.bytesToHex(INIT_BLOCK_KEY_HASH));
    log.info("shardClientKeyHash {}", Utils.bytesToHex(SHARD_CLIENT_KEY_HASH));

    RocksDbWrapper stateDb = new RocksDbWrapper(TON_DB_ROOT_PATH + "/state");
    stateDb.forEach(
        (key, value) -> {
          String s = new String(key);
          String v = new String(value);

          log.info(
              "key: {}, value (size {}): {}",
              Utils.bytesToHex(key),
              value.length,
              Utils.bytesToHex(value));
          if (value.length == 84) {
            InitBlockId initBlockId = InitBlockId.deserialize(ByteBuffer.wrap(value));
            log.info("initBlockId: {}", initBlockId);
          }
          //          log.info("key: {}, value : {}", s, v);
        });
    stateDb.close();
  }

  @Test
  public void testStateDbReaderGetLastBlockIdExtBlockIdext() throws IOException {
    RocksDbWrapper stateDb = new RocksDbWrapper(TON_DB_ROOT_PATH + "/state");
    byte[] value = stateDb.get(SHARD_CLIENT_KEY_HASH);
    InitBlockId initBlockId = InitBlockId.deserialize(ByteBuffer.wrap(value));
    log.info("initBlockId: {}", initBlockId);
    log.info("blockExtId: {}", initBlockId.getBlock());
    stateDb.close();
  }

  @Test
  public void testStateDbReaderGetInitBlockId() throws IOException {
    RocksDbWrapper stateDb = new RocksDbWrapper(TON_DB_ROOT_PATH + "/state");
    byte[] value = stateDb.get(INIT_BLOCK_KEY_HASH);
    InitBlockId initBlockId = InitBlockId.deserialize(ByteBuffer.wrap(value));
    log.info("initBlockId: {}", initBlockId);
    log.info("blockExtId: {}", initBlockId.getBlock());
    stateDb.close();
  }

  @Test
  public void testStateDbReaderGetGcBlockId() throws IOException {
    RocksDbWrapper stateDb = new RocksDbWrapper(TON_DB_ROOT_PATH + "/state");
    byte[] value = stateDb.get(GC_BLOCK_KEY_HASH);
    GcBlockId gcBlockId = GcBlockId.deserialize(ByteBuffer.wrap(value));
    log.info("gcBlockId: {}", gcBlockId);
    log.info("blockExtId: {}", gcBlockId.getBlock());
    stateDb.close();
  }

  @Test
  public void testStateDbReaderGetPersistentStateDescriptionShards() throws IOException {
    RocksDbWrapper stateDb = new RocksDbWrapper(TON_DB_ROOT_PATH + "/state");
    byte[] keyHash =
        Utils.sha256AsArray(
            PersistentStateDescriptionShardsKey.builder()
                .masterchainSeqno(226831)
                .build()
                .serialize());
    byte[] value = stateDb.get(keyHash);
    assert value != null;
    PersistentStateDescriptionShards persistentStateDescriptionShards =
        PersistentStateDescriptionShards.deserialize(ByteBuffer.wrap(value));
    log.info("persistentStateDescriptionShards: {}", persistentStateDescriptionShards);
    stateDb.close();
  }

  @Test
  public void testStateDbReaderGetPersistentStateDescriptionsList() throws IOException {
    RocksDbWrapper stateDb = new RocksDbWrapper(TON_DB_ROOT_PATH + "/state");
    byte[] value = stateDb.get(PERSISTENT_STATE_DESC_LIST_KEY_HASH);
    assert value != null;
    PersistentStateDescriptionsList persistentStateDescriptionsList =
        PersistentStateDescriptionsList.deserialize(ByteBuffer.wrap(value));
    log.info("persistentStateDescriptionShards: {}", persistentStateDescriptionsList);

    stateDb.close();
  }

  @Test
  public void testReadStateDb() throws IOException {
    // Update this path to point to your actual TON database

    log.info("Opening State database: {}", TON_DB_ROOT_PATH);

    try (StateDbReader stateReader = new StateDbReader(TON_DB_ROOT_PATH)) {
      // Get all state file keys
      List<String> stateKeys = stateReader.getStateFileKeys();
      log.info("Found {} state files", stateKeys.size());

      // Show first 10 state files
      log.info("First 10 state files:");
      for (int i = 0; i < Math.min(10, stateKeys.size()); i++) {
        String key = stateKeys.get(i);
        StateFileInfo info = stateReader.getStateFileInfo(key);
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

    for (StateFileType type : StateFileType.values()) {
      List<StateFileInfo> files = stateReader.getStateFilesByType(type);
      log.info("  {}: {} files", type, files.size());

      // Show sample files for each type
      if (!files.isEmpty()) {
        log.info("    Sample files:");
        for (int i = 0; i < Math.min(3, files.size()); i++) {
          StateFileInfo info = files.get(i);
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
      List<StateFileInfo> files = stateReader.getStateFilesByWorkchain(workchain);
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
        StateFileInfo info = stateReader.getStateFileInfo(key);
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
    List<StateFileInfo> recentStates = stateReader.getStateFilesBySeqnoRange(0, 1000);
    log.info("  States with seqno 0-1000: {} files", recentStates.size());

    // Test zero state lookup
    List<StateFileInfo> zeroStates = stateReader.getStateFilesByType(StateFileType.ZERO_STATE);
    if (!zeroStates.isEmpty()) {
      log.info("  Found {} zero states", zeroStates.size());

      // Try to read the first zero state
      StateFileInfo zeroState = zeroStates.get(0);
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
    List<StateFileInfo> persistentStates =
        stateReader.getStateFilesByType(StateFileType.PERSISTENT_STATE);
    if (!persistentStates.isEmpty()) {
      log.info("  Found {} persistent states", persistentStates.size());

      // Try to read the first persistent state
      StateFileInfo persistentState = persistentStates.get(0);
      try {
        BlockIdExt blockId =
            BlockIdExt.builder()
                .workchain(persistentState.workchain)
                .shard(Long.parseUnsignedLong(persistentState.shard, 16))
                .seqno((int) persistentState.seqno)
                .build();

        BlockIdExt mcBlockId =
            BlockIdExt.builder()
                .workchain(-1)
                .shard(-9223372036854775808L) // 0x8000000000000000
                .seqno((int) persistentState.seqno)
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
  public void testReadStateDbGetLastBlockIdExtBlockIdExt2() throws IOException {
    // Update this path to point to your actual TON database

    log.info("Opening State database: {}", TON_DB_ROOT_PATH);

    try (StateDbReader stateReader = new StateDbReader(TON_DB_ROOT_PATH)) {
      // Get all state file keys
      long startTime = System.currentTimeMillis();
      BlockIdExt last = stateReader.getLastBlockIdExt();
      long endTime = System.currentTimeMillis() - startTime;
      log.info("elapsed {}ms, last: {}", endTime, last);
    }
  }
}
