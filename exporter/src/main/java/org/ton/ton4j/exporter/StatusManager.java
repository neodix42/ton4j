package org.ton.ton4j.exporter;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.ton.ton4j.exporter.types.ExportStatus;

/** Manages the persistence of export status to/from status.json file */
@Slf4j
public class StatusManager {

  private static final String STATUS_FILE_NAME = "status.json";
  private final Gson gson;
  @Getter private final Path statusFilePath;

  public StatusManager() {
    this.gson = new GsonBuilder().setPrettyPrinting().create();
    this.statusFilePath = Paths.get(STATUS_FILE_NAME);
  }

  public StatusManager(String customPath) {
    this.gson = new GsonBuilder().setPrettyPrinting().create();
    this.statusFilePath = Paths.get(customPath, STATUS_FILE_NAME);
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
   * Saves export status to status.json file
   *
   * @param status the export status to save
   */
  public void saveStatus(ExportStatus status) {
    try {
      // Create parent directories if they don't exist
      Path parentDir = statusFilePath.getParent();
      if (parentDir != null && !Files.exists(parentDir)) {
        Files.createDirectories(parentDir);
      }

      String jsonContent = gson.toJson(status);
      Files.writeString(statusFilePath, jsonContent, StandardCharsets.UTF_8);

    } catch (Exception e) {
      log.error("Failed to save status file {}: {}", statusFilePath, e.getMessage());
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
