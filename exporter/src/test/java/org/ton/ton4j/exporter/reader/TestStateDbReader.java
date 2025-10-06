package org.ton.ton4j.exporter.reader;

import static org.ton.ton4j.exporter.reader.StateDbReader.*;

import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.ton.ton4j.tl.types.db.block.BlockIdExt;
import org.ton.ton4j.tl.types.db.state.*;
import org.ton.ton4j.utils.Utils;

@Slf4j
public class TestStateDbReader {

  public static final String TON_DB_ROOT_PATH =
      "/home/neodix/gitProjects/MyLocalTon/myLocalTon/genesis/db";

  @Test
  public void testStateDbReader() throws IOException {

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
        });
    stateDb.close();
  }

  @Test
  public void testStateDbReaderGetLastBlockIdExt() throws IOException {
    try (StateDbReader stateReader = new StateDbReader(TON_DB_ROOT_PATH)) {
      BlockIdExt blockIdExt = stateReader.getLastBlockIdExt();
      log.info("blockIdExt: {}", blockIdExt);
    }
  }

  @Test
  public void testStateDbReaderGetInitBlockId() throws IOException {
    try (StateDbReader stateReader = new StateDbReader(TON_DB_ROOT_PATH)) {
      InitBlockId initBlockId = stateReader.getInitBlockId();
      log.info("initBlockId: {}", initBlockId);
    }
  }

  @Test
  public void testStateDbReaderGetGcBlockId() throws IOException {
    try (StateDbReader stateReader = new StateDbReader(TON_DB_ROOT_PATH)) {
      GcBlockId gcBlockId = stateReader.getGcBlockId();
      log.info("gcBlockId: {}", gcBlockId);
    }
  }

  @Test
  public void testStateDbReaderGetPersistentStateDescriptionsShards2() throws IOException {
    try (StateDbReader stateReader = new StateDbReader(TON_DB_ROOT_PATH)) {

      PersistentStateDescriptionShards persistentStateDescriptionShards =
          stateReader.getPersistentStateDescriptionsShards(239249);
      for (BlockIdExt blockIdExt : persistentStateDescriptionShards.getShardBlocks()) {
        log.info("blockIdExt: {}", blockIdExt);
      }
    }
  }

  @Test
  public void testStateDbReaderGetPersistentStateDescriptionsList() throws IOException {
    try (StateDbReader stateReader = new StateDbReader(TON_DB_ROOT_PATH)) {
      PersistentStateDescriptionsList persistentStateDescriptionsList =
          stateReader.getPersistentStateDescriptionsList();
      for (PersistentStateDescriptionHeader header : persistentStateDescriptionsList.getList()) {
        log.info("persistentStateDescriptionHeader: {}", header);
      }
    }
  }

  @Test
  public void testReadStateDbGetLastBlockIdExt() throws IOException {
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
