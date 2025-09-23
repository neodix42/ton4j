package org.ton.ton4j.exporter.types;

import com.google.gson.annotations.SerializedName;
import java.time.Instant;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Represents the export status for tracking processed packages */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExportStatus {

  @SerializedName("export_id")
  private String exportId;

  @SerializedName("start_time")
  private String startTime;

  @SerializedName("last_update")
  private volatile String lastUpdate;

  @SerializedName("total_packages")
  private volatile long totalPackages;

  @SerializedName("processed_packages")
  private Set<String> processedPackages = ConcurrentHashMap.newKeySet();

  @SerializedName("processed_count")
  private volatile int processedCount;

  @SerializedName("parsed_blocks_count")
  private volatile int parsedBlocksCount;

  @SerializedName("non_blocks_count")
  private volatile int nonBlocksCount;

  @SerializedName("export_type")
  private String exportType; // "file" or "stdout"

  @SerializedName("output_file")
  private String outputFile; // only for file exports

  @SerializedName("deserialized")
  private boolean deserialized;

  @SerializedName("parallel_threads")
  private int parallelThreads;

  @SerializedName("completed")
  private volatile boolean completed;

  public ExportStatus(
      String exportId,
      long totalPackages,
      String exportType,
      String outputFile,
      boolean deserialized,
      int parallelThreads) {
    this.exportId = exportId;
    this.startTime = Instant.now().toString();
    this.lastUpdate = this.startTime;
    this.totalPackages = totalPackages;
    this.processedPackages = ConcurrentHashMap.newKeySet();
    this.processedCount = 0;
    this.parsedBlocksCount = 0;
    this.nonBlocksCount = 0;
    this.exportType = exportType;
    this.outputFile = outputFile;
    this.deserialized = deserialized;
    this.parallelThreads = parallelThreads;
    this.completed = false;
  }

  public synchronized void markPackageProcessed(String packageKey, int blocksFound, int nonBlocksFound) {
    if (!processedPackages.contains(packageKey)) {
      processedPackages.add(packageKey);
      processedCount++;
      parsedBlocksCount += blocksFound;
      nonBlocksCount += nonBlocksFound;
      lastUpdate = Instant.now().toString();
    }
  }

  public boolean isPackageProcessed(String packageKey) {
    return processedPackages.contains(packageKey);
  }

  public synchronized void markCompleted() {
    this.completed = true;
    this.lastUpdate = Instant.now().toString();
  }

  public double getProgressPercentage() {
    if (totalPackages == 0) return 0.0;
    return (double) processedCount / totalPackages * 100.0;
  }
  
  public synchronized void setTotalPackages(long totalPackages) {
    this.totalPackages = totalPackages;
  }
}
