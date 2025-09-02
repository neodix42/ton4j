package org.ton.ton4j.indexer.reader;

import java.io.Closeable;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.ton.ton4j.cell.Cell;
import org.ton.ton4j.cell.CellBuilder;
import org.ton.ton4j.tl.types.db.block.BlockIdExt;

/**
 * Specialized reader for TON archive state database. Handles files stored in the archive/states
 * directory.
 */
@Slf4j
public class StateDbReader implements Closeable {

  private final String dbPath;
  private final String statesPath;
  private final Map<String, StateFileInfo> stateFiles = new HashMap<>();

  /**
   * Creates a new StateDbReader.
   *
   * @param dbPath Path to the database root directory
   * @throws IOException If an I/O error occurs
   */
  public StateDbReader(String dbPath) throws IOException {
    this.dbPath = dbPath;
    this.statesPath = Paths.get(dbPath, "archive", "states").toString();

    // Discover all state files
    discoverStateFiles();
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

  @Override
  public void close() throws IOException {
    // No resources to close for file-based access
    log.debug("StateDbReader closed");
  }

  /** Information about a state file. */
  public static class StateFileInfo {
    public StateFileType type;
    public int workchain;
    public String shard;
    public long seqno;
    public String effectiveShard; // Only for split account states
    public String rootHash;
    public String fileHash;
    public String filePath;

    @Override
    public String toString() {
      return String.format(
          "StateFileInfo{type=%s, workchain=%d, shard=%s, seqno=%d, effectiveShard=%s, rootHash=%s, fileHash=%s}",
          type,
          workchain,
          shard,
          seqno,
          effectiveShard,
          rootHash != null ? rootHash.substring(0, Math.min(8, rootHash.length())) + "..." : null,
          fileHash != null ? fileHash.substring(0, Math.min(8, fileHash.length())) + "..." : null);
    }
  }

  /** Types of state files. */
  public enum StateFileType {
    ZERO_STATE,
    PERSISTENT_STATE,
    SPLIT_ACCOUNT_STATE,
    SPLIT_PERSISTENT_STATE
  }
}
