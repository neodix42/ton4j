package org.ton.ton4j.exporter;

import com.google.gson.annotations.SerializedName;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

/**
 * Represents the export status for tracking processed packages
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExportStatus {
    
    @SerializedName("export_id")
    private String exportId;
    
    @SerializedName("start_time")
    private String startTime;
    
    @SerializedName("last_update")
    private String lastUpdate;
    
    @SerializedName("total_packages")
    private long totalPackages;
    
    @SerializedName("processed_packages")
    private Set<String> processedPackages = new HashSet<>();
    
    @SerializedName("processed_count")
    private int processedCount;
    
    @SerializedName("parsed_blocks_count")
    private int parsedBlocksCount;
    
    @SerializedName("non_blocks_count")
    private int nonBlocksCount;
    
    @SerializedName("export_type")
    private String exportType; // "file" or "stdout"
    
    @SerializedName("output_file")
    private String outputFile; // only for file exports
    
    @SerializedName("deserialized")
    private boolean deserialized;
    
    @SerializedName("parallel_threads")
    private int parallelThreads;
    
    @SerializedName("completed")
    private boolean completed;
    
    public ExportStatus(String exportId, long totalPackages, String exportType, 
                       String outputFile, boolean deserialized, int parallelThreads) {
        this.exportId = exportId;
        this.startTime = Instant.now().toString();
        this.lastUpdate = this.startTime;
        this.totalPackages = totalPackages;
        this.processedPackages = new HashSet<>();
        this.processedCount = 0;
        this.parsedBlocksCount = 0;
        this.nonBlocksCount = 0;
        this.exportType = exportType;
        this.outputFile = outputFile;
        this.deserialized = deserialized;
        this.parallelThreads = parallelThreads;
        this.completed = false;
    }
    
    public void markPackageProcessed(String packageKey, int blocksFound, int nonBlocksFound) {
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
    
    public void markCompleted() {
        this.completed = true;
        this.lastUpdate = Instant.now().toString();
    }
    
    public double getProgressPercentage() {
        if (totalPackages == 0) return 0.0;
        return (double) processedCount / totalPackages * 100.0;
    }
}
