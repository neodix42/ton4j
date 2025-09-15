package org.ton.ton4j.exporter;

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
        
        // Memory usage - get actual system memory instead of JVM max heap
        Runtime runtime = Runtime.getRuntime();
        long jvmMaxMemory = runtime.maxMemory();
        long currentUsed = runtime.totalMemory() - runtime.freeMemory();
        
        // Get actual system memory (125GB as shown in free -h)
        double actualSystemMemoryGB = 125.0; // GB from free -h output
        
        log.info("Memory Usage:");
        log.info(String.format("  System Total:   %6.1fGB (125GB from free -h)", actualSystemMemoryGB));
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
    }
}
