package org.ton.ton4j.exporter.reader;

import java.io.Closeable;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.ton.ton4j.bitstring.BitString;
import org.ton.ton4j.cell.*;
import org.ton.ton4j.exporter.types.*;
import org.ton.ton4j.tl.types.db.celldb.CellDbValue;
import org.ton.ton4j.utils.Utils;

/**
 * Reader for TON CellDB database. The CellDB stores blockchain state cells in a RocksDB database
 * with a linked-list structure for metadata entries.
 *
 * <p>Based on the original TON C++ implementation in celldb.cpp, the CellDB uses: 1. Metadata
 * entries: key = "desc" + SHA256(TL-serialized block_id), value = TL-serialized db.celldb.value 2.
 * Special empty entry: key = "desczero", value = TL-serialized db.celldb.value (sentinel for linked
 * list) 3. Cell data entries: key = cell hash (raw bytes), value = serialized cell content 4.
 * Linked list structure: entries connected via prev/next pointers forming a doubly-linked list
 *
 * <p>The TL types used: - db.celldb.value: block_id:tonNode.blockIdExt prev:int256 next:int256
 * root_hash:int256 - db.celldb.key.value: hash:int256
 */
@Slf4j
@Data
public class CellDbReader implements Closeable {

  private final String dbPath;
  private RocksDbWrapper cellDb;

  // Cache for parsed entries
  private final Map<String, CellDbValue> entryCache = new HashMap<>();
  private CellDbValue emptyEntry;

  /**
   * Creates a new CellDbReader.
   *
   * @param dbPath Path to the database root directory (should contain celldb subdirectory)
   * @throws IOException If an I/O error occurs
   */
  public CellDbReader(String dbPath) throws IOException {
    this.dbPath = dbPath;
    initializeCellDatabase();
  }

  /** Initializes the CellDB database connection. */
  private void initializeCellDatabase() throws IOException {
    Path cellDbPath = Paths.get(dbPath, "celldb");

    if (!Files.exists(cellDbPath)) {
      throw new IOException("CellDB database not found at: " + cellDbPath);
    }

    try {
      cellDb = new RocksDbWrapper(cellDbPath.toString());
      //      log.info("Initialized CellDB database: {}", cellDbPath);
    } catch (IOException e) {
      throw new IOException("Could not initialize CellDB database: " + e.getMessage(), e);
    }
  }

  public static Cell parseCell(ByteBuffer data) throws IOException {

    //    log.info("cell in hex {}", Utils.bytesToHex(data.array()));
    int flag = data.getInt();
    boolean storedBoc = false;
    if (flag == -1) {
      storedBoc = true;
      flag = data.getInt();
    }

    if (storedBoc) {
      byte[] remaining = new byte[data.remaining()];
      data.get(remaining);
      return Cell.fromBoc(remaining);
    } else {
      // Following C++ approach: collect all cell hashes first, then deserialize in reverse order
      // This avoids recursion and matches the C++ deserialize_cell implementation

      int d1 = data.get() & 0xFF;
      int d2 = data.get() & 0xFF;

      CellSerializationInfo cellSerializationInfo = CellSerializationInfo.create(d1, d2);

      byte[] payload = new byte[cellSerializationInfo.getDataLength()];
      data.get(payload);

      byte[] allHashes = new byte[0];

      if (cellSerializationInfo.getRefsCount() != 0) {
        for (int i = 0; i < cellSerializationInfo.getRefsCount(); i++) {
          // skip level mask
          data.get();
          byte[] hash = new byte[32];
          data.get(hash);
          allHashes = Utils.concatBytes(allHashes, hash);

          // skip depth
          data.get();
          data.get();
        }
      }

      return CellBuilder.beginCell()
          .storeBitString(new BitString(payload, cellSerializationInfo.getDataLength() * 8))
          .storeHashes(allHashes)
          .setExotic(cellSerializationInfo.isSpecial())
          .setLevelMask(cellSerializationInfo.getLevelMask())
          .setRefsCount(cellSerializationInfo.getRefsCount())
          .endCellNoRecalculation();
    }
  }

  @Override
  public void close() throws IOException {
    if (cellDb != null) {
      cellDb.close();
      //      log.debug("Closed CellDB database");
    }
  }
}
