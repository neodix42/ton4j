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
        
        // Time breakdown
        log.info("Time Breakdown (total: " + (totalTime / 1_000_000) + "ms):");
        log.info(String.format("  RocksDB Reads:      %6.1fms (%5.1f%%) - %d operations, avg: %.2fμs/op", 
            rocksDbReadTime.get() / 1_000_000.0,
            (rocksDbReadTime.get() * 100.0) / totalTime,
            rocksDbReads.get(),
            rocksDbReads.get() > 0 ? (rocksDbReadTime.get() / 1000.0) / rocksDbReads.get() : 0));
            
        log.info(String.format("  BOC Parsing:        %6.1fms (%5.1f%%) - %d operations, avg: %.2fμs/op", 
            bocParsingTime.get() / 1_000_000.0,
            (bocParsingTime.get() * 100.0) / totalTime,
            bocParsings.get(),
            bocParsings.get() > 0 ? (bocParsingTime.get() / 1000.0) / bocParsings.get() : 0));
            
        log.info(String.format("  TLB Deserialization: %6.1fms (%5.1f%%) - %d operations, avg: %.2fμs/op", 
            tlbDeserializationTime.get() / 1_000_000.0,
            (tlbDeserializationTime.get() * 100.0) / totalTime,
            tlbDeserializations.get(),
            tlbDeserializations.get() > 0 ? (tlbDeserializationTime.get() / 1000.0) / tlbDeserializations.get() : 0));
            
        log.info(String.format("  JSON Serialization: %6.1fms (%5.1f%%) - %d operations, avg: %.2fμs/op", 
            jsonSerializationTime.get() / 1_000_000.0,
            (jsonSerializationTime.get() * 100.0) / totalTime,
            jsonSerializations.get(),
            jsonSerializations.get() > 0 ? (jsonSerializationTime.get() / 1000.0) / jsonSerializations.get() : 0));
            
        log.info(String.format("  Disk Writes:        %6.1fms (%5.1f%%) - %d operations, avg: %.2fμs/op", 
            diskWriteTime.get() / 1_000_000.0,
            (diskWriteTime.get() * 100.0) / totalTime,
            diskWrites.get(),
            diskWrites.get() > 0 ? (diskWriteTime.get() / 1000.0) / diskWrites.get() : 0));
        
        // Memory usage
        Runtime runtime = Runtime.getRuntime();
        long maxMemory = runtime.maxMemory();
        long currentUsed = runtime.totalMemory() - runtime.freeMemory();
        
        log.info("Memory Usage:");
        log.info(String.format("  Max Available:  %6.1fMB", maxMemory / (1024.0 * 1024.0)));
        log.info(String.format("  Current Used:   %6.1fMB (%5.1f%%)", 
            currentUsed / (1024.0 * 1024.0),
            (currentUsed * 100.0) / maxMemory));
        log.info(String.format("  Peak Used:      %6.1fMB (%5.1f%%)", 
            maxMemoryUsed.get() / (1024.0 * 1024.0),
            (maxMemoryUsed.get() * 100.0) / maxMemory));
        
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
