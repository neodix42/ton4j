package org.ton.ton4j.tl.types.db;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import org.rocksdb.Options;
import org.rocksdb.ReadOptions;
import org.rocksdb.RocksDB;
import org.rocksdb.RocksDBException;
import org.rocksdb.RocksIterator;
import org.ton.ton4j.tl.types.db.files.GlobalIndexKey;
import org.ton.ton4j.tl.types.db.files.GlobalIndexValue;

/** A wrapper for RocksDB operations. */
public class RocksDbWrapper implements Closeable {

  static {
    RocksDB.loadLibrary();
  }

  private final RocksDB db;
  private final ReadOptions readOptions;

  /**
   * Opens a RocksDB database in read-only mode.
   *
   * @param path Path to the RocksDB database
   * @throws IOException If an I/O error occurs
   */
  public RocksDbWrapper(String path) throws IOException {
    try {
      Options options = new Options();
      options.setCreateIfMissing(false);
      options.setErrorIfExists(false);

      readOptions = new ReadOptions();

      db = RocksDB.openReadOnly(options, path);
    } catch (RocksDBException e) {
      throw new IOException("Failed to open RocksDB: " + e.getMessage(), e);
    }
  }

  /**
   * Gets a value by key.
   *
   * @param key The key
   * @return The value, or null if the key doesn't exist
   * @throws IOException If an I/O error occurs
   */
  public GlobalIndexValue get(byte[] key) throws IOException {
    try {
      return GlobalIndexValue.deserialize(db.get(readOptions, key));
    } catch (RocksDBException e) {
      throw new IOException("Failed to get value: " + e.getMessage(), e);
    }
  }

  /**
   * Gets all key-value pairs in the database.
   *
   * @return A list of key-value pairs
   */
  public List<Map.Entry<GlobalIndexKey, GlobalIndexValue>> getAll() {
    List<Map.Entry<GlobalIndexKey, GlobalIndexValue>> entries = new ArrayList<>();

    try (RocksIterator iterator = db.newIterator(readOptions)) {
      for (iterator.seekToFirst(); iterator.isValid(); iterator.next()) {
        entries.add(
            Map.entry(
                GlobalIndexKey.deserialize(iterator.key()),
                GlobalIndexValue.deserialize(iterator.value())));
      }
    }

    return entries;
  }

  /**
   * Iterates through all key-value pairs in the database.
   *
   * @param consumer Consumer for key-value pairs
   */
  public void forEach(BiConsumer<GlobalIndexKey, GlobalIndexValue> consumer) {
    try (RocksIterator iterator = db.newIterator(readOptions)) {
      for (iterator.seekToFirst(); iterator.isValid(); iterator.next()) {
        consumer.accept(
            GlobalIndexKey.deserialize(iterator.key()),
            GlobalIndexValue.deserialize(iterator.value()));
      }
    }
  }

  /**
   * Gets statistics about the RocksDB database.
   *
   * @return A string containing statistics about the database
   * @throws IOException If an I/O error occurs
   */
  public String getStats() throws IOException {
    if (db == null) {
      throw new IOException("Database is not open");
    }

    try {
      return db.getProperty("rocksdb.stats");
    } catch (RocksDBException e) {
      throw new IOException("Failed to get database stats: " + e.getMessage(), e);
    }
  }

  @Override
  public void close() throws IOException {
    if (db != null) {
      db.close();
    }
  }
}
