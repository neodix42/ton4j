package org.ton.ton4j.exporter.reader;

import static org.ton.ton4j.exporter.reader.CellDbReader.parseCell;

import java.io.IOException;
import java.nio.ByteBuffer;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.ton.ton4j.address.Address;
import org.ton.ton4j.cell.Cell;
import org.ton.ton4j.cell.CellSlice;
import org.ton.ton4j.tl.types.db.block.BlockIdExt;
import org.ton.ton4j.tl.types.db.celldb.CellDbValue;
import org.ton.ton4j.tlb.ShardStateUnsplit;
import org.ton.ton4j.utils.Utils;

@Slf4j
public class TestCellDbReader {

  private static final String TEST_DB_PATH =
      "/home/neodix/gitProjects/MyLocalTon/myLocalTon/genesis/db";

  @Test
  public void testCellDbReader() throws IOException {
    RocksDbWrapper cellDb = new RocksDbWrapper(TEST_DB_PATH + "/celldb");
    cellDb.forEach(
        (key, value) -> {
          String s = new String(key);
          if (s.startsWith("desc")) {
            // meta data
            log.info("block hash: {}, value: {}", s.substring(4), CellDbValue.deserialize(value));
          } else if (s.startsWith("desczero")) {
            log.info("empty");
          }
          //          else {
          //            // raw cell
          //            log.info(
          //                "cell hash: {}, value (size {}): {}",
          //                Utils.bytesToHex(key),
          //                value.length,
          //                Utils.bytesToHex(value));
          //            // Cell cell = CellBuilder.beginCell().storeBytes(value).endCell();
          //          }
        });
    cellDb.close();
  }

  /** WIP */
  @Test
  public void testCellDbReaderGetAccountBalance() throws IOException {
    RocksDbWrapper cellDb = new RocksDbWrapper(TEST_DB_PATH + "/celldb");
    StateDbReader stateReader = new StateDbReader(TEST_DB_PATH);
    BlockIdExt blockIdExt = stateReader.getLastBlockIdExt();
    log.info("last: {}", blockIdExt);
    //    byte[] key = Utils.sha256AsArray(last.serializeBoxed());
    // construct key = "desc+base64(serialized(last))"
    String key = "desc" + Utils.bytesToBase64(Utils.sha256AsArray(blockIdExt.serializeBoxed()));
    byte[] value = cellDb.get(key.getBytes());
    log.info("key: {}, value: {}", key, Utils.bytesToHex(value));

    CellDbValue cellDbValue = CellDbValue.deserialize(ByteBuffer.wrap(value));
    log.info("cellDbValue: {}", cellDbValue);
    byte[] shardStateRootHash = cellDbValue.rootHash;

    // find full cell containing ShardStateUnsplit by shardStateRootHash
    byte[] rawShardStateUnsplit = cellDb.get(shardStateRootHash);
    log.info("rawShardStateUnsplit: {}", Utils.bytesToHex(rawShardStateUnsplit)); // top level cell

    //    rawShardStateUnsplit = Utils.slice(rawShardStateUnsplit, 6, rawShardStateUnsplit.length -
    // 6);
    //    ShardStateUnsplit shardStateUnsplit =
    //        ShardStateUnsplit.deserializeWithoutRefs(
    //            CellSlice.beginParse(Cell.fromBytesUnlimited(rawShardStateUnsplit)));
    //    rawShardStateUnsplit[4] = 2; // limit by first 2 refs only

    Cell c = parseCell(ByteBuffer.wrap(rawShardStateUnsplit));
    log.info("getMaxLevel: {}, getDepthLevels: {}", c.getMaxLevel(), c.getDepthLevels());

    ShardStateUnsplit shardStateUnsplitWithoutRefs =
        ShardStateUnsplit.deserializeWithoutRefs(CellSlice.beginParse(c));
    log.info("deserialized shardStateUnsplitWithoutRefs");
    log.info("shardStateUnsplitWithoutRefs: {}", shardStateUnsplitWithoutRefs);

    ShardStateUnsplit shardStateUnsplitWith2Refs =
        ShardStateUnsplit.deserializeWith2RefsOnly(CellSlice.beginParse(c));
    log.info("deserialized shardStateUnsplitWith2Refs");
    //    log.info("shardStateUnsplitWith2Refs: {}", shardStateUnsplitWith2Refs);
    //    log.info(
    //        "accounts {}",
    //        shardStateUnsplitWith2Refs.getShardAccounts().getShardAccountsAsList().size());
    log.info(
        "ShardAccount balance: {}",
        Utils.formatNanoValue(
            shardStateUnsplitWith2Refs
                .getShardAccounts()
                .getShardAccountByAddress(
                    Address.of(
                        "-1:0000000000000000000000000000000000000000000000000000000000000000"))
                .getBalance()));

    log.info(
        "ShardAccount balance: {}",
        Utils.formatNanoValue(
            shardStateUnsplitWith2Refs
                .getShardAccounts()
                .getShardAccountByAddress(
                    Address.of(
                        "-1:22f53b7d9aba2cef44755f7078b01614cd4dde2388a1729c2c386cf8f9898afe"))
                .getBalance()));
    //    for (ShardAccount shardAccount :
    //        shardStateUnsplitWith2Refs.getShardAccounts().getShardAccountsAsList()) {
    //      log.info(
    //          "acc {}, balance {}",
    //          shardAccount.getAccount().getAddress().toAddress().toRaw(),
    //          Utils.formatNanoValue(shardAccount.getBalance()));
    //    }

    // too slow, too big
    //    ShardStateUnsplit shardStateUnsplit =
    // ShardStateUnsplit.deserialize(CellSlice.beginParse(c));
    //    log.info("shardStateUnsplit: {}", shardStateUnsplit);

    cellDb.close();
  }
}
