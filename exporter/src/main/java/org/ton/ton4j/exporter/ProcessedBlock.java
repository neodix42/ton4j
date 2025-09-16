package org.ton.ton4j.exporter;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

/**
 * Data structure representing a processed block ready for writing to disk.
 * This class serves as the bridge between the processing threads (that load pack files
 * into memory and parse blocks) and the writer threads (that handle disk I/O).
 * 
 * Optimized for high-throughput systems with massive RAM (128GB+) and thousands of
 * processing threads feeding a smaller number of optimized writer threads.
 */
@Builder
@Getter
@ToString
public class ProcessedBlock {
    
    /**
     * The original block hash/key used for identification
     */
    private final String blockKey;
    
    /**
     * The archive/package key this block came from (for debugging/tracking)
     */
    private final String archiveKey;
    
    /**
     * The processed block data ready for writing.
     * This could be:
     * - Raw BoC hex string (if deserialized=false)
     * - JSON serialized Block object (if deserialized=true)
     * - CSV format with workchain,shard,seqno,json (if deserialized=true)
     */
    private final String processedData;
    
    /**
     * Timestamp when this block was processed (for performance monitoring)
     */
    private final long processingTimestamp;
    
    /**
     * Thread ID that processed this block (for debugging/monitoring)
     */
    private final long processingThreadId;
    
    /**
     * Size of the processed data in bytes (for memory monitoring)
     */
    private final int dataSize;
    
    /**
     * Whether this block was successfully deserialized (for statistics)
     */
    private final boolean wasDeserialized;
    
    /**
     * Processing time in nanoseconds (for performance analysis)
     */
    private final long processingTimeNanos;
    
    /**
     * Creates a ProcessedBlock for raw BoC hex output
     */
    public static ProcessedBlock createRawBlock(String blockKey, String archiveKey, 
                                              String bocHex, long processingThreadId, 
                                              long processingTimeNanos) {
        return ProcessedBlock.builder()
            .blockKey(blockKey)
            .archiveKey(archiveKey)
            .processedData(bocHex)
            .processingTimestamp(System.currentTimeMillis())
            .processingThreadId(processingThreadId)
            .dataSize(bocHex.length())
            .wasDeserialized(false)
            .processingTimeNanos(processingTimeNanos)
            .build();
    }
    
    /**
     * Creates a ProcessedBlock for deserialized JSON output
     */
    public static ProcessedBlock createDeserializedBlock(String blockKey, String archiveKey,
                                                       String jsonData, long processingThreadId,
                                                       long processingTimeNanos) {
        return ProcessedBlock.builder()
            .blockKey(blockKey)
            .archiveKey(archiveKey)
            .processedData(jsonData)
            .processingTimestamp(System.currentTimeMillis())
            .processingThreadId(processingThreadId)
            .dataSize(jsonData.length())
            .wasDeserialized(true)
            .processingTimeNanos(processingTimeNanos)
            .build();
    }
    
    /**
     * Creates a ProcessedBlock for error cases
     */
    public static ProcessedBlock createErrorBlock(String blockKey, String archiveKey,
                                                String errorData, long processingThreadId) {
        return ProcessedBlock.builder()
            .blockKey(blockKey)
            .archiveKey(archiveKey)
            .processedData(errorData)
            .processingTimestamp(System.currentTimeMillis())
            .processingThreadId(processingThreadId)
            .dataSize(errorData.length())
            .wasDeserialized(false)
            .processingTimeNanos(0)
            .build();
    }
    
    /**
     * Gets the data ready for writing to file (just the processed data)
     */
    public String getWriteData() {
        return processedData;
    }
    
    /**
     * Estimates memory usage of this ProcessedBlock instance
     */
    public long getEstimatedMemoryUsage() {
        // Rough estimate: object overhead + string data + metadata
        return 200 + // Object overhead
               (blockKey != null ? blockKey.length() * 2 : 0) +
               (archiveKey != null ? archiveKey.length() * 2 : 0) +
               (processedData != null ? processedData.length() * 2 : 0); // UTF-16 chars
    }
    
    /**
     * Creates a compact string representation for logging
     */
    public String toCompactString() {
        return String.format("ProcessedBlock{key=%s, archive=%s, size=%d, deserialized=%s, thread=%d}", 
            blockKey != null ? blockKey.substring(0, Math.min(8, blockKey.length())) + "..." : "null",
            archiveKey,
            dataSize,
            wasDeserialized,
            processingThreadId);
    }
}
