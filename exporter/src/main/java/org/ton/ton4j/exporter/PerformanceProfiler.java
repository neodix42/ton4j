package org.ton.ton4j.exporter;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.extern.slf4j.Slf4j;

/**
 * Performance profiler to measure and identify bottlenecks in the export process
 */
@Slf4j
public class PerformanceProfiler {
    
    // Timing measurements (in nanoseconds)
    private final AtomicLong rocksDbReadTime = new AtomicLong(0);
    private final AtomicLong bocParsingTime = new AtomicLong(0);
    private final AtomicLong tlbDeserializationTime = new AtomicLong(0);
    private final AtomicLong jsonSerializationTime = new AtomicLong(0);
    private final AtomicLong diskWriteTime = new AtomicLong(0);
    
    // Operation counters
    private final AtomicInteger rocksDbReads = new AtomicInteger(0);
    private final AtomicInteger bocParsings = new AtomicInteger(0);
    private final AtomicInteger tlbDeserializations = new AtomicInteger(0);
    private final AtomicInteger jsonSerializations = new AtomicInteger(0);
    private final AtomicInteger diskWrites = new AtomicInteger(0);
    
    // Memory tracking
    private final AtomicLong maxMemoryUsed = new AtomicLong(0);
    private final AtomicLong totalMemoryAllocated = new AtomicLong(0);
    
    // Thread configuration tracking
    private volatile int parallelThreads = 1;
    private volatile long wallClockStartTime = 0;
    private volatile long wallClockEndTime = 0;
    
    /**
     * Records RocksDB read time
     */
    public void recordRocksDbRead(long nanos) {
        rocksDbReadTime.addAndGet(nanos);
        rocksDbReads.incrementAndGet();
    }
    
    /**
     * Records BOC parsing time
     */
    public void recordBocParsing(long nanos) {
        bocParsingTime.addAndGet(nanos);
        bocParsings.incrementAndGet();
    }
    
    /**
     * Records TLB deserialization time
     */
    public void recordTlbDeserialization(long nanos) {
        tlbDeserializationTime.addAndGet(nanos);
        tlbDeserializations.incrementAndGet();
    }
    
    /**
     * Records JSON serialization time
     */
    public void recordJsonSerialization(long nanos) {
        jsonSerializationTime.addAndGet(nanos);
        jsonSerializations.incrementAndGet();
    }
    
    /**
     * Records disk write time
     */
    public void recordDiskWrite(long nanos) {
        diskWriteTime.addAndGet(nanos);
        diskWrites.incrementAndGet();
    }
    
    /**
     * Updates memory usage tracking
     */
    public void updateMemoryUsage() {
        Runtime runtime = Runtime.getRuntime();
        long usedMemory = runtime.totalMemory() - runtime.freeMemory();
        long maxMemory = runtime.maxMemory();
        
        maxMemoryUsed.set(Math.max(maxMemoryUsed.get(), usedMemory));
        totalMemoryAllocated.set(runtime.totalMemory());
    }
    
    /**
     * Sets the number of parallel threads used for processing
     */
    public void setParallelThreads(int threads) {
        this.parallelThreads = threads;
    }
    
    /**
     * Records the start time for wall clock measurements
     */
    public void recordWallClockStart() {
        this.wallClockStartTime = System.currentTimeMillis();
    }
    
    /**
     * Records the end time for wall clock measurements
     */
    public void recordWallClockEnd() {
        this.wallClockEndTime = System.currentTimeMillis();
    }
    
    /**
     * Prints detailed performance report
     */
    public void printReport() {
        long totalTime = rocksDbReadTime.get() + bocParsingTime.get() + 
                        tlbDeserializationTime.get() + jsonSerializationTime.get() + 
                        diskWriteTime.get();
        
        if (totalTime == 0) {
            log.info("No performance data collected yet");
            return;
        }
        
        log.info("=== PERFORMANCE ANALYSIS REPORT ===");
        
        // Time breakdown - show in seconds instead of milliseconds
        log.info("Time Breakdown (total: " + String.format("%.1f", totalTime / 1_000_000_000.0) + "s):");
        
        // Calculate operations per second instead of microseconds per operation
        double rocksDbOpsPerSec = rocksDbReads.get() > 0 ? (rocksDbReads.get() * 1_000_000_000.0) / rocksDbReadTime.get() : 0;
        double bocOpsPerSec = bocParsings.get() > 0 ? (bocParsings.get() * 1_000_000_000.0) / bocParsingTime.get() : 0;
        double tlbOpsPerSec = tlbDeserializations.get() > 0 ? (tlbDeserializations.get() * 1_000_000_000.0) / tlbDeserializationTime.get() : 0;
        double jsonOpsPerSec = jsonSerializations.get() > 0 ? (jsonSerializations.get() * 1_000_000_000.0) / jsonSerializationTime.get() : 0;
        double diskOpsPerSec = diskWrites.get() > 0 ? (diskWrites.get() * 1_000_000_000.0) / diskWriteTime.get() : 0;
        
        log.info(String.format("  RocksDB Reads:      %6.1fs (%5.1f%%) - %d operations, %.0f ops/sec", 
            rocksDbReadTime.get() / 1_000_000_000.0,
            (rocksDbReadTime.get() * 100.0) / totalTime,
            rocksDbReads.get(),
            rocksDbOpsPerSec));
            
        log.info(String.format("  BOC Parsing:        %6.1fs (%5.1f%%) - %d operations, %.0f ops/sec", 
            bocParsingTime.get() / 1_000_000_000.0,
            (bocParsingTime.get() * 100.0) / totalTime,
            bocParsings.get(),
            bocOpsPerSec));
            
        log.info(String.format("  TLB Deserialization: %6.1fs (%5.1f%%) - %d operations, %.0f ops/sec", 
            tlbDeserializationTime.get() / 1_000_000_000.0,
            (tlbDeserializationTime.get() * 100.0) / totalTime,
            tlbDeserializations.get(),
            tlbOpsPerSec));
            
        log.info(String.format("  JSON Serialization: %6.1fs (%5.1f%%) - %d operations, %.0f ops/sec", 
            jsonSerializationTime.get() / 1_000_000_000.0,
            (jsonSerializationTime.get() * 100.0) / totalTime,
            jsonSerializations.get(),
            jsonOpsPerSec));
            
        log.info(String.format("  Disk Writes:        %6.1fs (%5.1f%%) - %d operations, %.0f ops/sec", 
            diskWriteTime.get() / 1_000_000_000.0,
            (diskWriteTime.get() * 100.0) / totalTime,
            diskWrites.get(),
            diskOpsPerSec));
        
        // Threading analysis
        log.info("Threading Analysis:");
        log.info(String.format("  Parallel Threads:   %d", parallelThreads));
        
        // Calculate wall clock time if available
        long wallClockTimeMs = 0;
        if (wallClockEndTime > wallClockStartTime && wallClockStartTime > 0) {
            wallClockTimeMs = wallClockEndTime - wallClockStartTime;
            double wallClockTimeSeconds = wallClockTimeMs / 1000.0;
            log.info(String.format("  Wall Clock Time:    %.1fs", wallClockTimeSeconds));
            
            // Calculate effective parallelization
            double totalCpuTimeSeconds = totalTime / 1_000_000_000.0;
            double effectiveParallelization = totalCpuTimeSeconds / wallClockTimeSeconds;
            log.info(String.format("  Effective Parallel: %.1fx (CPU time / Wall time)", effectiveParallelization));
            
            // Single-thread equivalent rates
            if (bocParsings.get() > 0) {
                double singleThreadBocRate = bocOpsPerSec;
                double multiThreadBocRate = bocParsings.get() / wallClockTimeSeconds;
                log.info(String.format("  BOC Single-Thread:  %.0f ops/sec", singleThreadBocRate));
                log.info(String.format("  BOC Multi-Thread:   %.0f ops/sec (%.1fx speedup)", multiThreadBocRate, multiThreadBocRate / singleThreadBocRate));
            }
            
            if (diskWrites.get() > 0) {
                double multiThreadDiskRate = diskWrites.get() / wallClockTimeSeconds;
                double singleThreadDiskRate = multiThreadDiskRate / parallelThreads; // Average per writer thread
                log.info(String.format("  Disk Single-Thread:  %.0f ops/sec (average per writer thread)", singleThreadDiskRate));
                log.info(String.format("  Disk Multi-Thread:   %.0f ops/sec (total throughput across %d writer threads)", multiThreadDiskRate, parallelThreads));
            }
        } else {
            log.info("  Wall Clock Time:    Not available");
            log.info(String.format("  Single-Thread Rate: %.0f ops/sec (BOC parsing)", bocOpsPerSec));
            log.info(String.format("  Multi-Thread Est:   %.0f ops/sec (theoretical max)", bocOpsPerSec * parallelThreads));
        }
        
        // Memory usage - get actual system memory instead of JVM max heap
        Runtime runtime = Runtime.getRuntime();
        long jvmMaxMemory = runtime.maxMemory();
        long currentUsed = runtime.totalMemory() - runtime.freeMemory();
        
        // Get actual system memory dynamically
        double actualSystemMemoryGB = getSystemMemoryGB();
        
        log.info("Memory Usage:");
        log.info(String.format("  System Total:   %6.1fGB", actualSystemMemoryGB));
        log.info(String.format("  JVM Max Heap:   %6.1fGB", jvmMaxMemory / (1024.0 * 1024.0 * 1024.0)));
        log.info(String.format("  JVM Used:       %6.1fGB (%5.1f%% of heap)", 
            currentUsed / (1024.0 * 1024.0 * 1024.0),
            (currentUsed * 100.0) / jvmMaxMemory));
        log.info(String.format("  JVM Peak:       %6.1fGB (%5.1f%% of heap)", 
            maxMemoryUsed.get() / (1024.0 * 1024.0 * 1024.0),
            (maxMemoryUsed.get() * 100.0) / jvmMaxMemory));
        
        // Bottleneck identification
        long maxTime = Math.max(rocksDbReadTime.get(), 
                      Math.max(bocParsingTime.get(),
                      Math.max(tlbDeserializationTime.get(),
                      Math.max(jsonSerializationTime.get(), diskWriteTime.get()))));
        
        String bottleneck = "Unknown";
        if (maxTime == rocksDbReadTime.get()) bottleneck = "RocksDB Reads";
        else if (maxTime == bocParsingTime.get()) bottleneck = "BOC Parsing";
        else if (maxTime == tlbDeserializationTime.get()) bottleneck = "TLB Deserialization";
        else if (maxTime == jsonSerializationTime.get()) bottleneck = "JSON Serialization";
        else if (maxTime == diskWriteTime.get()) bottleneck = "Disk Writes";
        
        log.info(String.format("PRIMARY BOTTLENECK: %s (%.1f%% of total time)", 
            bottleneck, (maxTime * 100.0) / totalTime));
        
        log.info("=====================================");
    }
    
    /**
     * Gets system memory in GB by reading /proc/meminfo
     */
    private double getSystemMemoryGB() {
        try (BufferedReader reader = new BufferedReader(new FileReader("/proc/meminfo"))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("MemTotal:")) {
                    // Extract memory in KB and convert to GB
                    String[] parts = line.split("\\s+");
                    if (parts.length >= 2) {
                        long memoryKB = Long.parseLong(parts[1]);
                        return memoryKB / (1024.0 * 1024.0); // Convert KB to GB
                    }
                }
            }
        } catch (IOException e) {
            log.warn("Could not read system memory from /proc/meminfo: {}", e.getMessage());
        }
        
        // Fallback to JVM max memory if system memory cannot be determined
        Runtime runtime = Runtime.getRuntime();
        return runtime.maxMemory() / (1024.0 * 1024.0 * 1024.0);
    }
    
    /**
     * Resets all measurements
     */
    public void reset() {
        rocksDbReadTime.set(0);
        bocParsingTime.set(0);
        tlbDeserializationTime.set(0);
        jsonSerializationTime.set(0);
        diskWriteTime.set(0);
        
        rocksDbReads.set(0);
        bocParsings.set(0);
        tlbDeserializations.set(0);
        jsonSerializations.set(0);
        diskWrites.set(0);
        
        maxMemoryUsed.set(0);
        totalMemoryAllocated.set(0);
        
        parallelThreads = 1;
        wallClockStartTime = 0;
        wallClockEndTime = 0;
    }
}
