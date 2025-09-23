package org.ton.ton4j.exporter;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.ton.ton4j.exporter.types.ExportStatus;

/** Manages the persistence of export status to/from status.json file */
@Slf4j
public class StatusManager {

  private static final String STATUS_FILE_NAME = "status.json";
  private static volatile StatusManager instance;
  private static final Object lock = new Object();

  private final Gson gson;
  @Getter private final Path statusFilePath;
  private final ReentrantReadWriteLock rwLock = new ReentrantReadWriteLock();

  private StatusManager() {
    this.gson = new GsonBuilder().setPrettyPrinting().create();
    this.statusFilePath = Paths.get(STATUS_FILE_NAME);
  }

  private StatusManager(String customPath) {
    this.gson = new GsonBuilder().setPrettyPrinting().create();
    this.statusFilePath = Paths.get(customPath, STATUS_FILE_NAME);
  }

  /**
   * Gets the singleton instance of StatusManager
   *
   * @return StatusManager instance
   */
  public static StatusManager getInstance() {
    if (instance == null) {
      synchronized (lock) {
        if (instance == null) {
          instance = new StatusManager();
        }
      }
    }
    return instance;
  }

  /**
   * Gets the singleton instance of StatusManager with custom path
   *
   * @param customPath custom path for status file
   * @return StatusManager instance
   */
  public static StatusManager getInstance(String customPath) {
    if (instance == null) {
      synchronized (lock) {
        if (instance == null) {
          instance = new StatusManager(customPath);
        }
      }
    }
    return instance;
  }

  /** Resets the singleton instance (mainly for testing) */
  public static void resetInstance() {
    synchronized (lock) {
      instance = null;
    }
  }

  /**
   * Loads existing export status from status.json file
   *
   * @return ExportStatus if file exists and is valid, null otherwise
   */
  public ExportStatus loadStatus() {
    try {
      if (!Files.exists(statusFilePath)) {
        log.debug("Status file does not exist: {}", statusFilePath);
        return null;
      }

      String jsonContent = Files.readString(statusFilePath, StandardCharsets.UTF_8);
      ExportStatus status = gson.fromJson(jsonContent, ExportStatus.class);

      if (status != null) {
        log.info(
            "Loaded export status: {} packages processed out of {}",
            status.getProcessedCount(),
            status.getTotalPackages());
        return status;
      }
    } catch (Exception e) {
      log.warn("Failed to load status file {}: {}", statusFilePath, e.getMessage());
    }
    return null;
  }

  /**
   * Saves export status to status.json file with thread safety
   *
   * @param status the export status to save
   */
  public void saveStatus(ExportStatus status) {
    if (status == null) {
      log.warn("Cannot save null export status");
      return;
    }

    rwLock.writeLock().lock();
    try {
      // Create parent directories if they don't exist
      Path parentDir = statusFilePath.getParent();
      if (parentDir != null && !Files.exists(parentDir)) {
        Files.createDirectories(parentDir);
      }

      // Create a deep copy of the status to avoid concurrent modification during serialization
      ExportStatus statusCopy = createStatusCopy(status);

      String jsonContent = gson.toJson(statusCopy);
      if (jsonContent == null || jsonContent.trim().isEmpty()) {
        log.warn("Generated JSON content is null or empty for status: {}", status);
        return;
      }

      Files.writeString(statusFilePath, jsonContent, StandardCharsets.UTF_8);

    } catch (Exception e) {
      // ignore
    } finally {
      rwLock.writeLock().unlock();
    }
  }

  /**
   * Creates a thread-safe copy of ExportStatus to avoid concurrent modification during
   * serialization
   */
  private ExportStatus createStatusCopy(ExportStatus original) {
    synchronized (original) {
      ExportStatus copy = new ExportStatus();
      copy.setExportId(original.getExportId());
      copy.setStartTime(original.getStartTime());
      copy.setLastUpdate(original.getLastUpdate());
      copy.setTotalPackages(original.getTotalPackages());
      copy.setProcessedCount(original.getProcessedCount());
      copy.setParsedBlocksCount(original.getParsedBlocksCount());
      copy.setNonBlocksCount(original.getNonBlocksCount());
      copy.setExportType(original.getExportType());
      copy.setOutputFile(original.getOutputFile());
      copy.setDeserialized(original.isDeserialized());
      copy.setParallelThreads(original.getParallelThreads());
      copy.setCompleted(original.isCompleted());

      // Create a copy of the processed packages set
      copy.getProcessedPackages().addAll(original.getProcessedPackages());

      return copy;
    }
  }

  /**
   * Creates a new export status with a unique ID
   *
   * @param totalPackages total number of packages to process
   * @param exportType "file" or "stdout"
   * @param outputFile output file path (null for stdout exports)
   * @param deserialized whether blocks are deserialized
   * @param parallelThreads number of parallel threads
   * @return new ExportStatus instance
   */
  public ExportStatus createNewStatus(
      long totalPackages,
      String exportType,
      String outputFile,
      boolean deserialized,
      int parallelThreads) {
    String exportId = UUID.randomUUID().toString();
    return new ExportStatus(
        exportId, totalPackages, exportType, outputFile, deserialized, parallelThreads);
  }

  /** Deletes the status file */
  public void deleteStatus() {
    try {
      if (Files.exists(statusFilePath)) {
        Files.delete(statusFilePath);
        System.out.println("Deleted status file: " + statusFilePath);
      }
    } catch (Exception e) {
      log.warn("Failed to delete status file {}: {}", statusFilePath, e.getMessage());
    }
  }

  /**
   * Checks if a status file exists
   *
   * @return true if status file exists
   */
  public boolean statusExists() {
    return Files.exists(statusFilePath);
  }
}
