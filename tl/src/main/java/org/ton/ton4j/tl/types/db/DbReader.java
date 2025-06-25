package org.ton.ton4j.tl.types.db;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import org.ton.ton4j.tl.liteserver.responses.BlockIdExt;
import org.ton.ton4j.tl.types.db.block.ArchivedInfo;
import org.ton.ton4j.tl.types.db.block.BlockInfo;
import org.ton.ton4j.tl.types.db.block.PackedInfo;
import org.ton.ton4j.tl.types.db.blockdb.Lru;
import org.ton.ton4j.tl.types.db.blockdb.Value;
import org.ton.ton4j.tl.types.db.blockdb.key.LruKey;
import org.ton.ton4j.tl.types.db.blockdb.key.ValueKey;
import org.ton.ton4j.tl.types.db.filedb.key.BlockFileKey;
import org.ton.ton4j.tl.types.db.filedb.key.BlockInfoKey;
import org.ton.ton4j.tl.types.db.filedb.key.CandidateKey;
import org.ton.ton4j.tl.types.db.filedb.key.CandidateRefKey;
import org.ton.ton4j.tl.types.db.filedb.key.EmptyKey;
import org.ton.ton4j.tl.types.db.filedb.key.PersistentStateFileKey;
import org.ton.ton4j.tl.types.db.filedb.key.ProofKey;
import org.ton.ton4j.tl.types.db.filedb.key.ProofLinkKey;
import org.ton.ton4j.tl.types.db.filedb.key.SignaturesKey;
import org.ton.ton4j.tl.types.db.filedb.key.ZeroStateFileKey;
import org.ton.ton4j.tl.types.db.files.key.IndexKey;
import org.ton.ton4j.tl.types.db.files.key.PackageKey;
import org.ton.ton4j.tl.types.db.files.package_.FirstBlock;
import org.ton.ton4j.tl.types.db.lt.desc.DescKey;
import org.ton.ton4j.tl.types.db.lt.el.ElKey;
import org.ton.ton4j.tl.types.db.lt.shard.ShardKey;
import org.ton.ton4j.tl.types.db.lt.status.StatusKey;
import org.ton.ton4j.tl.types.db.root.BlockDbKey;
import org.ton.ton4j.tl.types.db.root.CellDbKey;
import org.ton.ton4j.tl.types.db.root.Config;
import org.ton.ton4j.tl.types.db.root.ConfigKey;
import org.ton.ton4j.tl.types.db.root.DbDescription;
import org.ton.ton4j.tl.types.db.state.AsyncSerializer;
import org.ton.ton4j.tl.types.db.state.DbVersion;
import org.ton.ton4j.tl.types.db.state.DestroyedSessions;
import org.ton.ton4j.tl.types.db.state.GcBlockId;
import org.ton.ton4j.tl.types.db.state.Hardforks;
import org.ton.ton4j.tl.types.db.state.InitBlockId;
import org.ton.ton4j.tl.types.db.state.PersistentStateDescriptionHeader;
import org.ton.ton4j.tl.types.db.state.PersistentStateDescriptionShards;
import org.ton.ton4j.tl.types.db.state.PersistentStateDescriptionsList;
import org.ton.ton4j.tl.types.db.state.ShardClient;
import org.ton.ton4j.tl.types.db.state.key.AsyncSerializerKey;
import org.ton.ton4j.tl.types.db.state.key.DbVersionKey;
import org.ton.ton4j.tl.types.db.state.key.DestroyedSessionsKey;
import org.ton.ton4j.tl.types.db.state.key.GcBlockIdKey;
import org.ton.ton4j.tl.types.db.state.key.HardforksKey;
import org.ton.ton4j.tl.types.db.state.key.InitBlockIdKey;
import org.ton.ton4j.tl.types.db.state.key.PersistentStateDescriptionListsKey;
import org.ton.ton4j.tl.types.db.state.key.PersistentStateDescriptionShardsKey;
import org.ton.ton4j.tl.types.db.state.key.ShardClientKey;

/**
 * DbReader is responsible for reading TON blockchain database files and deserializing them
 * according to the TL schema.
 */
public class DbReader {

  private final String dbPath;
  private final Map<String, Object> cache = new HashMap<>();

  /**
   * Creates a new DbReader instance.
   *
   * @param dbPath Path to the RocksDB database directory
   */
  public DbReader(String dbPath) {
    this.dbPath = dbPath;
  }

  /**
   * Reads a file from the database and deserializes it according to the TL schema.
   *
   * @param filePath Relative path to the file within the database directory
   * @param keyType Type of the key used to access the data
   * @param valueType Type of the value to deserialize
   * @return Deserialized object
   * @throws IOException If an I/O error occurs
   */
  public Object readFile(String filePath, Class<?> keyType, Class<?> valueType) throws IOException {
    String cacheKey = filePath + ":" + keyType.getName() + ":" + valueType.getName();
    if (cache.containsKey(cacheKey)) {
      return cache.get(cacheKey);
    }

    Path path = Paths.get(dbPath, filePath);
    if (!Files.exists(path)) {
      throw new IOException("File not found: " + path);
    }

    byte[] data = Files.readAllBytes(path);
    ByteBuffer buffer = ByteBuffer.wrap(data);

    Object result = deserialize(buffer, valueType);
    cache.put(cacheKey, result);
    return result;
  }

  /**
   * Deserializes a ByteBuffer into an object of the specified type.
   *
   * @param buffer ByteBuffer containing the serialized data
   * @param type Type of the object to deserialize
   * @return Deserialized object
   */
  @SuppressWarnings("unchecked")
  private Object deserialize(ByteBuffer buffer, Class<?> type) {
    // Root types
    if (type == DbDescription.class) {
      return DbDescription.deserialize(buffer);
    } else if (type == CellDbKey.class) {
      return CellDbKey.deserialize(buffer);
    } else if (type == BlockDbKey.class) {
      return BlockDbKey.deserialize(buffer);
    } else if (type == Config.class) {
      return Config.deserialize(buffer);
    } else if (type == ConfigKey.class) {
      return ConfigKey.deserialize(buffer);
    }

    // CellDB types
    else if (type == org.ton.ton4j.tl.types.db.celldb.Value.class) {
      return org.ton.ton4j.tl.types.db.celldb.Value.deserialize(buffer);
    } else if (type == org.ton.ton4j.tl.types.db.celldb.key.Value.class) {
      return org.ton.ton4j.tl.types.db.celldb.key.Value.deserialize(buffer);
    }

    // Block types
    else if (type == BlockInfo.class) {
      return BlockInfo.deserialize(buffer);
    } else if (type == PackedInfo.class) {
      return PackedInfo.deserialize(buffer);
    } else if (type == ArchivedInfo.class) {
      return ArchivedInfo.deserialize(buffer);
    }

    // BlockDB types
    else if (type == Value.class) {
      return Value.deserialize(buffer);
    } else if (type == Lru.class) {
      return Lru.deserialize(buffer);
    } else if (type == LruKey.class) {
      return LruKey.deserialize(buffer);
    } else if (type == ValueKey.class) {
      return ValueKey.deserialize(buffer);
    }

    // FileDB types
    else if (type == EmptyKey.class) {
      return EmptyKey.deserialize(buffer);
    } else if (type == BlockFileKey.class) {
      return BlockFileKey.deserialize(buffer);
    } else if (type == ZeroStateFileKey.class) {
      return ZeroStateFileKey.deserialize(buffer);
    } else if (type == PersistentStateFileKey.class) {
      return PersistentStateFileKey.deserialize(buffer);
    } else if (type == ProofKey.class) {
      return ProofKey.deserialize(buffer);
    } else if (type == ProofLinkKey.class) {
      return ProofLinkKey.deserialize(buffer);
    } else if (type == SignaturesKey.class) {
      return SignaturesKey.deserialize(buffer);
    } else if (type == CandidateKey.class) {
      return CandidateKey.deserialize(buffer);
    } else if (type == CandidateRefKey.class) {
      return CandidateRefKey.deserialize(buffer);
    } else if (type == BlockInfoKey.class) {
      return BlockInfoKey.deserialize(buffer);
    } else if (type == org.ton.ton4j.tl.types.db.filedb.Value.class) {
      return org.ton.ton4j.tl.types.db.filedb.Value.deserialize(buffer);
    }

    // Files types
    else if (type == IndexKey.class) {
      return IndexKey.deserialize(buffer);
    } else if (type == PackageKey.class) {
      return PackageKey.deserialize(buffer);
    } else if (type == org.ton.ton4j.tl.types.db.files.index.Value.class) {
      return org.ton.ton4j.tl.types.db.files.index.Value.deserialize(buffer);
    } else if (type == FirstBlock.class) {
      return FirstBlock.deserialize(buffer);
    } else if (type == org.ton.ton4j.tl.types.db.files.package_.Value.class) {
      return org.ton.ton4j.tl.types.db.files.package_.Value.deserialize(buffer);
    }

    // State types
    else if (type == DestroyedSessionsKey.class) {
      return DestroyedSessionsKey.deserialize(buffer);
    } else if (type == DestroyedSessions.class) {
      return DestroyedSessions.deserialize(buffer);
    } else if (type == InitBlockIdKey.class) {
      return InitBlockIdKey.deserialize(buffer);
    } else if (type == InitBlockId.class) {
      return InitBlockId.deserialize(buffer);
    } else if (type == GcBlockIdKey.class) {
      return GcBlockIdKey.deserialize(buffer);
    } else if (type == GcBlockId.class) {
      return GcBlockId.deserialize(buffer);
    } else if (type == ShardClientKey.class) {
      return ShardClientKey.deserialize(buffer);
    } else if (type == ShardClient.class) {
      return ShardClient.deserialize(buffer);
    } else if (type == AsyncSerializerKey.class) {
      return AsyncSerializerKey.deserialize(buffer);
    } else if (type == AsyncSerializer.class) {
      return AsyncSerializer.deserialize(buffer);
    } else if (type == HardforksKey.class) {
      return HardforksKey.deserialize(buffer);
    } else if (type == Hardforks.class) {
      return Hardforks.deserialize(buffer);
    } else if (type == DbVersionKey.class) {
      return DbVersionKey.deserialize(buffer);
    } else if (type == DbVersion.class) {
      return DbVersion.deserialize(buffer);
    } else if (type == PersistentStateDescriptionShardsKey.class) {
      return PersistentStateDescriptionShardsKey.deserialize(buffer);
    } else if (type == PersistentStateDescriptionShards.class) {
      return PersistentStateDescriptionShards.deserialize(buffer);
    } else if (type == PersistentStateDescriptionListsKey.class) {
      return PersistentStateDescriptionListsKey.deserialize(buffer);
    } else if (type == PersistentStateDescriptionHeader.class) {
      return PersistentStateDescriptionHeader.deserialize(buffer);
    } else if (type == PersistentStateDescriptionsList.class) {
      return PersistentStateDescriptionsList.deserialize(buffer);
    }

    // LT types
    else if (type == ElKey.class) {
      return ElKey.deserialize(buffer);
    } else if (type == org.ton.ton4j.tl.types.db.lt.el.Value.class) {
      return org.ton.ton4j.tl.types.db.lt.el.Value.deserialize(buffer);
    } else if (type == DescKey.class) {
      return DescKey.deserialize(buffer);
    } else if (type == org.ton.ton4j.tl.types.db.lt.desc.Value.class) {
      return org.ton.ton4j.tl.types.db.lt.desc.Value.deserialize(buffer);
    } else if (type == ShardKey.class) {
      return ShardKey.deserialize(buffer);
    } else if (type == org.ton.ton4j.tl.types.db.lt.shard.Value.class) {
      return org.ton.ton4j.tl.types.db.lt.shard.Value.deserialize(buffer);
    } else if (type == StatusKey.class) {
      return StatusKey.deserialize(buffer);
    } else if (type == org.ton.ton4j.tl.types.db.lt.status.Value.class) {
      return org.ton.ton4j.tl.types.db.lt.status.Value.deserialize(buffer);
    }

    // BlockIdExt
    else if (type == BlockIdExt.class) {
      return BlockIdExt.deserialize(buffer);
    }

    throw new IllegalArgumentException("Unsupported type: " + type.getName());
  }

  /** Clears the cache. */
  public void clearCache() {
    cache.clear();
  }

  /**
   * Gets a list of all files in the database directory.
   *
   * @return Array of file paths
   */
  public String[] listFiles() {
    File dir = new File(dbPath);
    return dir.list();
  }
}
