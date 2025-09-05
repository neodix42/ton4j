package org.ton.ton4j.exporter.reader;

import java.io.Closeable;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.rocksdb.Options;
import org.rocksdb.RocksDB;
import org.rocksdb.RocksDBException;
import org.ton.ton4j.cell.Cell;
import org.ton.ton4j.cell.CellBuilder;
import org.ton.ton4j.exporter.types.BlockId;
import org.ton.ton4j.exporter.types.StateFileInfo;
import org.ton.ton4j.exporter.types.StateFileType;
import org.ton.ton4j.tl.types.db.block.BlockIdExt;
import org.ton.ton4j.tl.types.db.files.index.IndexValue;
import org.ton.ton4j.tlb.Block;
import org.ton.ton4j.utils.Utils;

/**
 * Specialized reader for TON archive state database. Handles files stored in the archive/states
 * directory.
 */
@Slf4j
public class StateDbReader implements Closeable {

  private final String dbPath;
  private final String statesPath;
  private final Map<String, StateFileInfo> stateFiles = new HashMap<>();

  // RocksDB components for state database access
  private RocksDB stateRocksDb;
  private Options stateDbOptions;
  private final String stateDbPath;

  /**
   * Creates a new StateDbReader.
   *
   * @param dbPath Path to the database root directory
   * @throws IOException If an I/O error occurs
   */
  public StateDbReader(String dbPath) throws IOException {
    this.dbPath = dbPath;
    this.statesPath = Paths.get(dbPath, "archive", "states").toString();
    this.stateDbPath = Paths.get(dbPath, "state").toString();

    // Initialize RocksDB for state database access
    initializeRocksDb();

    // Discover all state files
    discoverStateFiles();
  }

  /**
   * Initializes RocksDB for state database access.
   *
   * @throws IOException If RocksDB initialization fails
   */
  private void initializeRocksDb() throws IOException {
    try {
      RocksDB.loadLibrary();
      stateDbOptions = new Options().setCreateIfMissing(false);

      Path stateDbDir = Paths.get(stateDbPath);
      if (Files.exists(stateDbDir)) {
        stateRocksDb = RocksDB.openReadOnly(stateDbOptions, stateDbPath);
        log.debug("Opened RocksDB state database at: {}", stateDbPath);
      } else {
        log.warn("State database directory not found: {}", stateDbPath);
      }
    } catch (RocksDBException e) {
      log.warn("Failed to initialize RocksDB state database: {}", e.getMessage());
      // Continue without RocksDB - fall back to file-based access only
    }
  }

  /**
   * Discovers all state files in the archive/states directory.
   *
   * @throws IOException If an I/O error occurs
   */
  private void discoverStateFiles() throws IOException {
    Path statesDir = Paths.get(statesPath);
    if (!Files.exists(statesDir)) {
      log.warn("States directory not found: {}", statesDir);
      return;
    }

    List<Path> stateFilePaths =
        Files.list(statesDir).filter(Files::isRegularFile).collect(Collectors.toList());

    for (Path stateFile : stateFilePaths) {
      try {
        String filename = stateFile.getFileName().toString();
        StateFileInfo info = parseStateFilename(filename);
        if (info != null) {
          info.filePath = stateFile.toString();
          stateFiles.put(filename, info);
          log.debug("Discovered state file: {} -> {}", filename, info);
        } else {
          log.warn("Could not parse state filename: {}", filename);
        }
      } catch (Exception e) {
        log.warn("Error processing state file {}: {}", stateFile, e.getMessage());
      }
    }

    log.info("Discovered {} state files in {}", stateFiles.size(), statesPath);
  }

  /**
   * Parses a state filename to extract metadata.
   *
   * <p>State filename formats (based on actual TON database files): - Zero state:
   * zerostate_{workchain}_{file_hash} (simplified format) - Persistent state:
   * state_{workchain}_{shard}_{seqno}_{root_hash}_{file_hash} - Split account state:
   * split_account_{workchain}_{shard}_{seqno}_{effective_shard}_{root_hash}_{file_hash} - Split
   * persistent state: split_state_{workchain}_{shard}_{seqno}_{root_hash}_{file_hash}
   *
   * @param filename The state filename to parse
   * @return StateFileInfo object with parsed metadata, or null if parsing fails
   */
  private StateFileInfo parseStateFilename(String filename) {
    try {
      // Zero state pattern (simplified): zerostate_{workchain}_{file_hash}
      Pattern zeroStatePattern = Pattern.compile("^zerostate_(-?\\d+)_([0-9a-fA-F]+)$");
      Matcher zeroMatcher = zeroStatePattern.matcher(filename);
      if (zeroMatcher.matches()) {
        StateFileInfo info = new StateFileInfo();
        info.type = StateFileType.ZERO_STATE;
        info.workchain = Integer.parseInt(zeroMatcher.group(1));
        info.shard = "8000000000000000"; // Default shard for zero state
        info.fileHash = zeroMatcher.group(2);
        info.rootHash = zeroMatcher.group(2); // Use file hash as root hash for zero state
        info.seqno = 0; // Zero state has seqno 0
        return info;
      }

      // Zero state pattern (full): zerostate_{workchain}_{shard}_{root_hash}_{file_hash}
      Pattern zeroStateFullPattern =
          Pattern.compile("^zerostate_(-?\\d+)_([0-9a-fA-F]+)_([0-9a-fA-F]+)_([0-9a-fA-F]+)$");
      Matcher zeroFullMatcher = zeroStateFullPattern.matcher(filename);
      if (zeroFullMatcher.matches()) {
        StateFileInfo info = new StateFileInfo();
        info.type = StateFileType.ZERO_STATE;
        info.workchain = Integer.parseInt(zeroFullMatcher.group(1));
        info.shard = zeroFullMatcher.group(2);
        info.rootHash = zeroFullMatcher.group(3);
        info.fileHash = zeroFullMatcher.group(4);
        info.seqno = 0; // Zero state has seqno 0
        return info;
      }

      // Persistent state pattern: state_{workchain}_{shard}_{seqno}_{root_hash}_{file_hash}
      Pattern persistentStatePattern =
          Pattern.compile("^state_(-?\\d+)_([0-9a-fA-F]+)_(\\d+)_([0-9a-fA-F]+)_([0-9a-fA-F]+)$");
      Matcher persistentMatcher = persistentStatePattern.matcher(filename);
      if (persistentMatcher.matches()) {
        StateFileInfo info = new StateFileInfo();
        info.type = StateFileType.PERSISTENT_STATE;
        info.workchain = Integer.parseInt(persistentMatcher.group(1));
        info.shard = persistentMatcher.group(2);
        info.seqno = Long.parseLong(persistentMatcher.group(3));
        info.rootHash = persistentMatcher.group(4);
        info.fileHash = persistentMatcher.group(5);
        return info;
      }

      // Split account state pattern:
      // split_account_{workchain}_{shard}_{seqno}_{effective_shard}_{root_hash}_{file_hash}
      Pattern splitAccountPattern =
          Pattern.compile(
              "^split_account_(-?\\d+)_([0-9a-fA-F]+)_(\\d+)_([0-9a-fA-F]+)_([0-9a-fA-F]+)_([0-9a-fA-F]+)$");
      Matcher splitAccountMatcher = splitAccountPattern.matcher(filename);
      if (splitAccountMatcher.matches()) {
        StateFileInfo info = new StateFileInfo();
        info.type = StateFileType.SPLIT_ACCOUNT_STATE;
        info.workchain = Integer.parseInt(splitAccountMatcher.group(1));
        info.shard = splitAccountMatcher.group(2);
        info.seqno = Long.parseLong(splitAccountMatcher.group(3));
        info.effectiveShard = splitAccountMatcher.group(4);
        info.rootHash = splitAccountMatcher.group(5);
        info.fileHash = splitAccountMatcher.group(6);
        return info;
      }

      // Split persistent state pattern:
      // split_state_{workchain}_{shard}_{seqno}_{root_hash}_{file_hash}
      Pattern splitStatePattern =
          Pattern.compile(
              "^split_state_(-?\\d+)_([0-9a-fA-F]+)_(\\d+)_([0-9a-fA-F]+)_([0-9a-fA-F]+)$");
      Matcher splitStateMatcher = splitStatePattern.matcher(filename);
      if (splitStateMatcher.matches()) {
        StateFileInfo info = new StateFileInfo();
        info.type = StateFileType.SPLIT_PERSISTENT_STATE;
        info.workchain = Integer.parseInt(splitStateMatcher.group(1));
        info.shard = splitStateMatcher.group(2);
        info.seqno = Long.parseLong(splitStateMatcher.group(3));
        info.rootHash = splitStateMatcher.group(4);
        info.fileHash = splitStateMatcher.group(5);
        return info;
      }

      return null;
    } catch (Exception e) {
      log.warn("Error parsing state filename {}: {}", filename, e.getMessage());
      return null;
    }
  }

  /**
   * Gets all available state file keys.
   *
   * @return List of state file keys (filenames)
   */
  public List<String> getStateFileKeys() {
    return new ArrayList<>(stateFiles.keySet());
  }

  /**
   * Gets state file information by filename.
   *
   * @param filename The state filename
   * @return StateFileInfo object, or null if not found
   */
  public StateFileInfo getStateFileInfo(String filename) {
    return stateFiles.get(filename);
  }

  /**
   * Reads a zero state file by block ID.
   *
   * @param blockId The block ID of the zero state
   * @return The zero state data, or null if not found
   * @throws IOException If an I/O error occurs
   */
  public byte[] readZeroState(BlockIdExt blockId) throws IOException {
    // Find zero state file matching the block ID
    for (StateFileInfo info : stateFiles.values()) {
      if (info.type == StateFileType.ZERO_STATE
          && info.workchain == blockId.getWorkchain()
          && matchesShard(info.shard, blockId.getShard())) {
        return Files.readAllBytes(Paths.get(info.filePath));
      }
    }
    return null;
  }

  /**
   * Reads a persistent state file by block ID and masterchain block ID.
   *
   * @param blockId The block ID of the shard
   * @param mcBlockId The masterchain block ID
   * @return The persistent state data, or null if not found
   * @throws IOException If an I/O error occurs
   */
  public byte[] readPersistentState(BlockIdExt blockId, BlockIdExt mcBlockId) throws IOException {
    // Find persistent state file matching the block IDs
    for (StateFileInfo info : stateFiles.values()) {
      if (info.type == StateFileType.PERSISTENT_STATE
          && info.workchain == blockId.getWorkchain()
          && matchesShard(info.shard, blockId.getShard())
          && info.seqno == mcBlockId.getSeqno()) {
        return Files.readAllBytes(Paths.get(info.filePath));
      }
    }
    return null;
  }

  /**
   * Reads a state file by filename.
   *
   * @param filename The state filename
   * @return The state data, or null if not found
   * @throws IOException If an I/O error occurs
   */
  public byte[] readStateFile(String filename) throws IOException {
    StateFileInfo info = stateFiles.get(filename);
    if (info == null) {
      return null;
    }
    return Files.readAllBytes(Paths.get(info.filePath));
  }

  /**
   * Reads and deserializes a state file as a Cell.
   *
   * @param filename The state filename
   * @return The deserialized Cell, or null if not found
   * @throws IOException If an I/O error occurs
   */
  public Cell readStateFileAsCell(String filename) throws IOException {
    byte[] data = readStateFile(filename);
    if (data == null) {
      return null;
    }

    try {
      return CellBuilder.beginCell().fromBoc(data).endCell();
    } catch (Exception e) {
      log.warn("Error deserializing state file {} as Cell: {}", filename, e.getMessage());
      return null;
    }
  }

  /**
   * Gets all state files of a specific type.
   *
   * @param type The state file type to filter by
   * @return List of state file information matching the type
   */
  public List<StateFileInfo> getStateFilesByType(StateFileType type) {
    return stateFiles.values().stream()
        .filter(info -> info.type == type)
        .collect(Collectors.toList());
  }

  /**
   * Gets all state files for a specific workchain.
   *
   * @param workchain The workchain ID
   * @return List of state file information for the workchain
   */
  public List<StateFileInfo> getStateFilesByWorkchain(int workchain) {
    return stateFiles.values().stream()
        .filter(info -> info.workchain == workchain)
        .collect(Collectors.toList());
  }

  /**
   * Gets all state files for a specific sequence number range.
   *
   * @param minSeqno Minimum sequence number (inclusive)
   * @param maxSeqno Maximum sequence number (inclusive)
   * @return List of state file information in the sequence number range
   */
  public List<StateFileInfo> getStateFilesBySeqnoRange(long minSeqno, long maxSeqno) {
    return stateFiles.values().stream()
        .filter(info -> info.seqno >= minSeqno && info.seqno <= maxSeqno)
        .collect(Collectors.toList());
  }

  /**
   * Checks if a shard string matches a shard ID. This is a simplified implementation - in practice,
   * shard matching is more complex.
   *
   * @param shardStr The shard string from filename
   * @param shardId The shard ID to match
   * @return True if they match, false otherwise
   */
  private boolean matchesShard(String shardStr, long shardId) {
    try {
      // Convert hex string to long and compare
      long shardFromStr = Long.parseUnsignedLong(shardStr, 16);
      return shardFromStr == shardId;
    } catch (NumberFormatException e) {
      return false;
    }
  }

  /**
   * Gets a block handle from the files database (temp packages), similar to how getLast() works.
   * This method uses the same approach as exporter.getLast() to find recent blocks.
   *
   * @param blockId The block ID to look up
   * @return Block handle data, or null if not found
   */
  public byte[] getBlockHandle(BlockIdExt blockId) {
    log.debug("Getting block handle for: {}", blockId);

    try {
      // Use the same approach as getLast() - query temp packages in files database
      try (GlobalIndexDbReader globalIndexReader = new GlobalIndexDbReader(dbPath)) {
        IndexValue mainIndex = globalIndexReader.getMainIndexIndexValue();

        if (mainIndex == null || mainIndex.getTempPackages().isEmpty()) {
          log.debug("No temp packages found in global index for block handle lookup");
          return null;
        }

        // Get temp package timestamps (they are Unix timestamps)
        List<Integer> tempPackageTimestamps = mainIndex.getTempPackages();
        log.debug("Searching {} temp packages for block handle", tempPackageTimestamps.size());

        // Sort timestamps in descending order (most recent first)
        List<Integer> sortedTimestamps = new ArrayList<>(tempPackageTimestamps);
        Collections.sort(sortedTimestamps, Collections.reverseOrder());

        // Search through temp packages to find the block
        for (Integer packageTimestamp : sortedTimestamps) {
          try (TempPackageIndexReader tempIndexReader =
              new TempPackageIndexReader(dbPath, packageTimestamp)) {

            // Get all blocks from this temp package
            Map<BlockId, Block> blocks = tempIndexReader.getAllBlocks();

            // Look for the specific block
            for (Map.Entry<BlockId, Block> entry : blocks.entrySet()) {
              BlockId foundBlockId = entry.getKey();
              Block foundBlock = entry.getValue();

              // Check if this matches our target block
              if (foundBlockId.getWorkchain() == blockId.getWorkchain()
                  && foundBlockId.shard == blockId.getShard()
                  && foundBlockId.getSeqno() == blockId.getSeqno()) {

                log.debug("Found matching block in temp package {}", packageTimestamp);

                // Create a simple block handle (block info serialized)
                // In the real implementation, this would be the actual block handle format
                // todo
                String blockHandle =
                    String.format(
                        "blockhandle_%d_%016x_%d",
                        foundBlockId.getWorkchain(),
                        foundBlockId.getShard(),
                        foundBlockId.getSeqno());
                return blockHandle.getBytes();
              }
            }
          } catch (Exception e) {
            log.debug("Error searching temp package {}: {}", packageTimestamp, e.getMessage());
            continue;
          }
        }

        log.debug("Block handle not found in any temp package for: {}", blockId);
        return null;
      }
    } catch (Exception e) {
      log.warn("Error getting block handle from files database: {}", e.getMessage());
      return null;
    }
  }

  /**
   * Gets a state hash reference from the files database (temp packages), similar to how getLast()
   * works. This method uses the same approach as exporter.getLast() to find recent block states.
   *
   * @param blockId The block ID to look up
   * @return State hash data, or null if not found
   */
  public byte[] getStateHash(BlockIdExt blockId) {
    log.debug("Getting state hash for: {}", blockId);

    try {
      // Use the same approach as getLast() - query temp packages in files database
      try (GlobalIndexDbReader globalIndexReader = new GlobalIndexDbReader(dbPath)) {
        org.ton.ton4j.tl.types.db.files.index.IndexValue mainIndex =
            globalIndexReader.getMainIndexIndexValue();

        if (mainIndex == null || mainIndex.getTempPackages().isEmpty()) {
          log.debug("No temp packages found in global index for state hash lookup");
          return null;
        }

        // Get temp package timestamps (they are Unix timestamps)
        List<Integer> tempPackageTimestamps = mainIndex.getTempPackages();
        log.debug("Searching {} temp packages for state hash", tempPackageTimestamps.size());

        // Sort timestamps in descending order (most recent first)
        List<Integer> sortedTimestamps = new ArrayList<>(tempPackageTimestamps);
        Collections.sort(sortedTimestamps, Collections.reverseOrder());

        // Search through temp packages to find the block
        for (Integer packageTimestamp : sortedTimestamps) {
          try (TempPackageIndexReader tempIndexReader =
              new TempPackageIndexReader(dbPath, packageTimestamp)) {

            // Get all blocks from this temp package
            Map<BlockId, Block> blocks = tempIndexReader.getAllBlocks();

            // Look for the specific block
            for (Map.Entry<BlockId, Block> entry : blocks.entrySet()) {
              BlockId foundBlockId = entry.getKey();
              Block foundBlock = entry.getValue();

              // Check if this matches our target block
              if (foundBlockId.getWorkchain() == blockId.getWorkchain()
                  && foundBlockId.shard == blockId.getShard()
                  && foundBlockId.getSeqno() == blockId.getSeqno()) {

                log.debug("Found matching block in temp package {}", packageTimestamp);

                // Extract state hash from the block
                // For now, create a synthetic state hash since the block structure access is
                // complex

                // foundBlock.toCell().getHash(); // todo
                String stateHashStr =
                    String.format(
                        "statehash_%d_%016x_%d",
                        foundBlockId.getWorkchain(),
                        foundBlockId.getShard(),
                        foundBlockId.getSeqno());
                log.debug("Created synthetic state hash for block: {}", stateHashStr);
                return stateHashStr.getBytes();
              }
            }
          } catch (Exception e) {
            log.debug("Error searching temp package {}: {}", packageTimestamp, e.getMessage());
            continue;
          }
        }

        log.debug("State hash not found in any temp package for: {}", blockId);
        return null;
      }
    } catch (Exception e) {
      log.warn("Error getting state hash from files database: {}", e.getMessage());
      return null;
    }
  }

  /**
   * Gets the latest masterchain block ID from the state database. If not found in RocksDB, falls
   * back to using the highest sequence number from available state files.
   *
   * @return Latest masterchain block ID, or null if not found
   */
  public BlockIdExt getLatestMasterchainBlock() {
    // First try to get from RocksDB
    if (stateRocksDb != null) {
      try {
        // Look for latest masterchain block key
        byte[] keyBytes = "latest_mc_block".getBytes();
        byte[] value = stateRocksDb.get(keyBytes);

        if (value != null) {
          // Parse the block ID from the stored value
          // This is a simplified implementation - actual format may vary
          String blockIdStr = new String(value);
          String[] parts = blockIdStr.split(":");
          if (parts.length >= 4) {
            int workchain = Integer.parseInt(parts[0]);
            long shard = Long.parseUnsignedLong(parts[1], 16);
            long seqno = Long.parseLong(parts[2]);
            // parts[3] would be root hash, parts[4] would be file hash

            byte[] rootHash = new byte[32]; // Default empty hash
            byte[] fileHash = new byte[32]; // Default empty hash

            // Try to parse hashes if available
            if (parts.length >= 5) {
              try {
                rootHash = Utils.hexToSignedBytes(parts[3]);
                fileHash = Utils.hexToSignedBytes(parts[4]);
              } catch (Exception e) {
                log.warn("Error parsing hashes from latest masterchain block: {}", e.getMessage());
              }
            }

            BlockIdExt blockId =
                BlockIdExt.builder()
                    .workchain(workchain)
                    .shard(shard)
                    .seqno(seqno)
                    .rootHash(rootHash)
                    .fileHash(fileHash)
                    .build();

            log.debug("Found latest masterchain block from RocksDB: {}", blockId);
            return blockId;
          }
        }
      } catch (RocksDBException e) {
        log.warn("Error reading latest masterchain block from RocksDB: {}", e.getMessage());
        // Continue to fallback method
      }
    }

    // Fallback: Find the highest sequence number masterchain state file
    log.debug("RocksDB latest masterchain block not found, using fallback method");

    List<StateFileInfo> masterchainStates = getStateFilesByWorkchain(-1);
    if (masterchainStates.isEmpty()) {
      log.debug("No masterchain state files found");
      return null;
    }

    // Find the state with the highest sequence number
    StateFileInfo latestState =
        masterchainStates.stream().max((a, b) -> Long.compare(a.seqno, b.seqno)).orElse(null);

    if (latestState == null) {
      log.debug("No valid masterchain state found");
      return null;
    }

    // Create BlockIdExt from the latest state file
    try {
      byte[] rootHash = new byte[32];
      byte[] fileHash = new byte[32];

      // Try to parse hashes from the state file info
      if (latestState.rootHash != null && latestState.rootHash.length() >= 8) {
        try {
          rootHash = Utils.hexToSignedBytes(latestState.rootHash);
        } catch (Exception e) {
          log.debug("Could not parse root hash: {}", e.getMessage());
        }
      }

      if (latestState.fileHash != null && latestState.fileHash.length() >= 8) {
        try {
          fileHash = Utils.hexToSignedBytes(latestState.fileHash);
        } catch (Exception e) {
          log.debug("Could not parse file hash: {}", e.getMessage());
        }
      }

      BlockIdExt blockId =
          BlockIdExt.builder()
              .workchain(latestState.workchain)
              .shard(Long.parseUnsignedLong(latestState.shard, 16))
              .seqno(latestState.seqno)
              .rootHash(rootHash)
              .fileHash(fileHash)
              .build();

      log.debug("Found latest masterchain block from state files: {}", blockId);
      return blockId;

    } catch (Exception e) {
      log.warn("Error creating BlockIdExt from state file: {}", e.getMessage());
      return null;
    }
  }

  @Override
  public void close() throws IOException {
    // Close RocksDB resources
    if (stateRocksDb != null) {
      stateRocksDb.close();
      stateRocksDb = null;
    }
    if (stateDbOptions != null) {
      stateDbOptions.close();
      stateDbOptions = null;
    }
    log.debug("StateDbReader closed");
  }
}
