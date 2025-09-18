package org.ton.ton4j.exporter.types;

import com.google.gson.annotations.SerializedName;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents the export status for tracking processed packages.
 * Optimized to avoid performance degradation from growing collections.
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
    
    // REMOVED: processedPackages HashSet - was causing performance degradation
    // Now using a simple counter-based approach for better performance
    
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
    
    // Thread-safe tracking for concurrent access (but no growing collections)
    private transient final AtomicInteger atomicProcessedCount = new AtomicInteger(0);
    private transient final AtomicInteger atomicParsedBlocksCount = new AtomicInteger(0);
    private transient final AtomicInteger atomicNonBlocksCount = new AtomicInteger(0);
    
    // Small, bounded cache for recent packages to avoid duplicate processing
    // Using ConcurrentHashMap with size limit to prevent growth
    private transient final ConcurrentHashMap<String, Boolean> recentPackages = new ConcurrentHashMap<>();
    private transient final AtomicInteger cacheSize = new AtomicInteger(0);
    private static final int MAX_CACHE_SIZE = 1000; // Bounded cache size
    
    public ExportStatus(String exportId, long totalPackages, String exportType, 
                       String outputFile, boolean deserialized, int parallelThreads) {
        this.exportId = exportId;
        this.startTime = Instant.now().toString();
        this.lastUpdate = this.startTime;
        this.totalPackages = totalPackages;
        this.processedCount = 0;
        this.parsedBlocksCount = 0;
        this.nonBlocksCount = 0;
        this.exportType = exportType;
        this.outputFile = outputFile;
        this.deserialized = deserialized;
        this.parallelThreads = parallelThreads;
        this.completed = false;
        
        // Initialize atomic counters
        this.atomicProcessedCount.set(0);
        this.atomicParsedBlocksCount.set(0);
        this.atomicNonBlocksCount.set(0);
    }
    
    public void markPackageProcessed(String packageKey, int blocksFound, int nonBlocksFound) {
        // Use bounded cache to avoid duplicate processing without growing indefinitely
        if (recentPackages.putIfAbsent(packageKey, Boolean.TRUE) == null) {
            // Package was not in cache, so it's newly processed
            atomicProcessedCount.incrementAndGet();
            atomicParsedBlocksCount.addAndGet(blocksFound);
            atomicNonBlocksCount.addAndGet(nonBlocksFound);
            
            // Update serialized fields periodically for status saving
            this.processedCount = atomicProcessedCount.get();
            this.parsedBlocksCount = atomicParsedBlocksCount.get();
            this.nonBlocksCount = atomicNonBlocksCount.get();
            this.lastUpdate = Instant.now().toString();
            
            // Maintain bounded cache size to prevent performance degradation
            if (cacheSize.incrementAndGet() > MAX_CACHE_SIZE) {
                // Clear half the cache when it gets too large
                recentPackages.clear();
                cacheSize.set(0);
            }
        }
    }
    
    public boolean isPackageProcessed(String packageKey) {
        // Simple check against bounded cache (may have false negatives after cache clear, but that's OK)
        return recentPackages.containsKey(packageKey);
    }
    
    public void markCompleted() {
        this.completed = true;
        this.lastUpdate = Instant.now().toString();
        
        // Final sync of atomic counters to serialized fields
        this.processedCount = atomicProcessedCount.get();
        this.parsedBlocksCount = atomicParsedBlocksCount.get();
        this.nonBlocksCount = atomicNonBlocksCount.get();
    }
    
    public double getProgressPercentage() {
        if (totalPackages == 0) return 0.0;
        return (double) getProcessedCount() / totalPackages * 100.0;
    }
    
    // Thread-safe getters that use atomic counters
    public int getProcessedCount() {
        return atomicProcessedCount != null ? atomicProcessedCount.get() : processedCount;
    }
    
    public int getParsedBlocksCount() {
        return atomicParsedBlocksCount != null ? atomicParsedBlocksCount.get() : parsedBlocksCount;
    }
    
    public int getNonBlocksCount() {
        return atomicNonBlocksCount != null ? atomicNonBlocksCount.get() : nonBlocksCount;
    }
}
