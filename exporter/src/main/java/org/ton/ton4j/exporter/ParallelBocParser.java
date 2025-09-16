package org.ton.ton4j.exporter;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import lombok.extern.slf4j.Slf4j;
import org.ton.ton4j.cell.Cell;
import org.ton.ton4j.cell.CellBuilder;
import org.ton.ton4j.cell.CellSlice;
import org.ton.ton4j.tlb.Block;

/**
 * Parallel BOC (Bag of Cells) parser that implements optimized parallel parsing
 * as part of Phase 3.1 block-level parallelization.
 * 
 * This parser handles the CPU-intensive BOC parsing and TLB deserialization
 * operations in parallel to maximize throughput.
 */
@Slf4j
public class ParallelBocParser {

    /** Result of BOC parsing operation */
    public static class BocParseResult {
        private final boolean success;
        private final Cell cell;
        private final Block block;
        private final String errorMessage;
        private final boolean isBlock;
        private final long magic;

        private BocParseResult(boolean success, Cell cell, Block block, String errorMessage, 
                              boolean isBlock, long magic) {
            this.success = success;
            this.cell = cell;
            this.block = block;
            this.errorMessage = errorMessage;
            this.isBlock = isBlock;
            this.magic = magic;
        }

        public static BocParseResult success(Cell cell, Block block, boolean isBlock, long magic) {
            return new BocParseResult(true, cell, block, null, isBlock, magic);
        }

        public static BocParseResult failure(String errorMessage) {
            return new BocParseResult(false, null, null, errorMessage, false, 0);
        }

        public boolean isSuccess() { return success; }
        public Cell getCell() { return cell; }
        public Block getBlock() { return block; }
        public String getErrorMessage() { return errorMessage; }
        public boolean isBlock() { return isBlock; }
        public long getMagic() { return magic; }
    }

    /** Configuration for BOC parsing */
    public static class BocParseConfig {
        private final boolean deserializeBlocks;
        private final boolean validateMagic;
        private final long expectedMagic;

        public BocParseConfig(boolean deserializeBlocks, boolean validateMagic, long expectedMagic) {
            this.deserializeBlocks = deserializeBlocks;
            this.validateMagic = validateMagic;
            this.expectedMagic = expectedMagic;
        }

        public static BocParseConfig forBlocks(boolean deserialize) {
            return new BocParseConfig(deserialize, true, 0x11ef55aaL);
        }

        public static BocParseConfig forAnyCell() {
            return new BocParseConfig(false, false, 0);
        }

        public boolean shouldDeserializeBlocks() { return deserializeBlocks; }
        public boolean shouldValidateMagic() { return validateMagic; }
        public long getExpectedMagic() { return expectedMagic; }
    }

    private final ExecutorService parsingExecutor;
    private final int parallelThreads;
    
    // Performance metrics
    private final AtomicLong totalBocParseTime = new AtomicLong(0);
    private final AtomicLong totalTlbDeserializeTime = new AtomicLong(0);
    private final AtomicLong totalParsedBlocks = new AtomicLong(0);
    private final AtomicLong totalParseErrors = new AtomicLong(0);

    /**
     * Creates a new parallel BOC parser
     * 
     * @param parallelThreads Number of threads for parallel parsing
     */
    public ParallelBocParser(int parallelThreads) {
        this.parallelThreads = parallelThreads;
        // Use a dedicated thread pool for CPU-intensive parsing operations
        this.parsingExecutor = Executors.newFixedThreadPool(
            parallelThreads,
            r -> {
                Thread t = new Thread(r, "BocParser-" + System.currentTimeMillis());
                t.setDaemon(false); // Keep JVM alive
                return t;
            }
        );
        
        log.info("Initialized ParallelBocParser with {} threads", parallelThreads);
    }

    /**
     * Parse a single BOC synchronously
     * 
     * @param bocData The BOC data to parse
     * @param config Parsing configuration
     * @return Parse result
     */
    public BocParseResult parseBoc(byte[] bocData, BocParseConfig config) {
        long startTime = System.nanoTime();
        
        try {
            // Phase 1: Parse BOC to Cell
            long bocStartTime = System.nanoTime();
            Cell cell = CellBuilder.beginCell().fromBoc(bocData).endCell();
            long bocEndTime = System.nanoTime();
            totalBocParseTime.addAndGet(bocEndTime - bocStartTime);

            // Phase 2: Check magic number if required
            long magic = cell.getBits().preReadUint(32).longValue();
            boolean isBlock = false;
            
            if (config.shouldValidateMagic()) {
                isBlock = (magic == config.getExpectedMagic());
                if (!isBlock && config.getExpectedMagic() != 0) {
                    // Not the expected type, but still successful parsing
                    return BocParseResult.success(cell, null, false, magic);
                }
            }

            // Phase 3: Deserialize Block if required and it's a block
            Block block = null;
            if (config.shouldDeserializeBlocks() && (isBlock || !config.shouldValidateMagic())) {
                try {
                    long tlbStartTime = System.nanoTime();
                    block = Block.deserialize(CellSlice.beginParse(cell));
                    long tlbEndTime = System.nanoTime();
                    totalTlbDeserializeTime.addAndGet(tlbEndTime - tlbStartTime);
                    isBlock = true; // Successfully deserialized as block
                } catch (Exception e) {
                    // Deserialization failed, but BOC parsing succeeded
                    log.debug("Failed to deserialize block: {}", e.getMessage());
                    isBlock = false;
                }
            }

            totalParsedBlocks.incrementAndGet();
            return BocParseResult.success(cell, block, isBlock, magic);

        } catch (Exception e) {
            totalParseErrors.incrementAndGet();
            return BocParseResult.failure("BOC parsing failed: " + e.getMessage());
        } finally {
            long endTime = System.nanoTime();
            // Total time includes all phases
        }
    }

    /**
     * Parse a single BOC asynchronously
     * 
     * @param bocData The BOC data to parse
     * @param config Parsing configuration
     * @return CompletableFuture with parse result
     */
    public CompletableFuture<BocParseResult> parseBocAsync(byte[] bocData, BocParseConfig config) {
        return CompletableFuture.supplyAsync(() -> parseBoc(bocData, config), parsingExecutor);
    }

    /**
     * Parse multiple BOCs in parallel
     * 
     * @param bocDataList List of BOC data to parse
     * @param config Parsing configuration
     * @return CompletableFuture with list of parse results
     */
    public CompletableFuture<java.util.List<BocParseResult>> parseBocsBatch(
            java.util.List<byte[]> bocDataList, BocParseConfig config) {
        
        java.util.List<CompletableFuture<BocParseResult>> futures = new java.util.ArrayList<>();
        
        for (byte[] bocData : bocDataList) {
            futures.add(parseBocAsync(bocData, config));
        }
        
        // Combine all futures into a single result
        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
            .thenApply(v -> {
                java.util.List<BocParseResult> results = new java.util.ArrayList<>();
                for (CompletableFuture<BocParseResult> future : futures) {
                    try {
                        results.add(future.get());
                    } catch (Exception e) {
                        results.add(BocParseResult.failure("Async parsing failed: " + e.getMessage()));
                    }
                }
                return results;
            });
    }

    /**
     * Parse BOCs using a streaming approach with callback processing
     * This is optimal for large datasets where you don't want to accumulate all results in memory
     * 
     * @param bocDataStream Stream of BOC data
     * @param config Parsing configuration
     * @param resultProcessor Callback to process each result as it becomes available
     * @return CompletableFuture that completes when all parsing is done
     */
    public CompletableFuture<Void> parseBocStream(
            java.util.stream.Stream<byte[]> bocDataStream,
            BocParseConfig config,
            java.util.function.Consumer<BocParseResult> resultProcessor) {
        
        // Convert stream to parallel stream and process with the parsing executor
        return CompletableFuture.runAsync(() -> {
            bocDataStream.parallel().forEach(bocData -> {
                try {
                    BocParseResult result = parseBoc(bocData, config);
                    resultProcessor.accept(result);
                } catch (Exception e) {
                    resultProcessor.accept(BocParseResult.failure("Stream parsing failed: " + e.getMessage()));
                }
            });
        }, parsingExecutor);
    }

    /**
     * Get performance statistics
     */
    public ParsingStatistics getStatistics() {
        return new ParsingStatistics(
            totalParsedBlocks.get(),
            totalParseErrors.get(),
            totalBocParseTime.get(),
            totalTlbDeserializeTime.get()
        );
    }

    /**
     * Reset performance statistics
     */
    public void resetStatistics() {
        totalBocParseTime.set(0);
        totalTlbDeserializeTime.set(0);
        totalParsedBlocks.set(0);
        totalParseErrors.set(0);
    }

    /**
     * Shutdown the parser and clean up resources
     */
    public void shutdown() {
        if (parsingExecutor != null && !parsingExecutor.isShutdown()) {
            parsingExecutor.shutdown();
            try {
                if (!parsingExecutor.awaitTermination(10, TimeUnit.SECONDS)) {
                    log.warn("BOC parsing executor did not terminate within 10 seconds, forcing shutdown");
                    parsingExecutor.shutdownNow();
                    if (!parsingExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                        log.error("BOC parsing executor did not respond to forced shutdown");
                    }
                }
            } catch (InterruptedException e) {
                log.warn("Interrupted while waiting for BOC parsing executor to terminate");
                parsingExecutor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
        
        log.info("ParallelBocParser shutdown completed");
    }

    /**
     * Performance statistics for BOC parsing
     */
    public static class ParsingStatistics {
        private final long totalParsedBlocks;
        private final long totalParseErrors;
        private final long totalBocParseTimeNanos;
        private final long totalTlbDeserializeTimeNanos;

        public ParsingStatistics(long totalParsedBlocks, long totalParseErrors, 
                               long totalBocParseTimeNanos, long totalTlbDeserializeTimeNanos) {
            this.totalParsedBlocks = totalParsedBlocks;
            this.totalParseErrors = totalParseErrors;
            this.totalBocParseTimeNanos = totalBocParseTimeNanos;
            this.totalTlbDeserializeTimeNanos = totalTlbDeserializeTimeNanos;
        }

        public long getTotalParsedBlocks() { return totalParsedBlocks; }
        public long getTotalParseErrors() { return totalParseErrors; }
        public long getTotalBocParseTimeNanos() { return totalBocParseTimeNanos; }
        public long getTotalTlbDeserializeTimeNanos() { return totalTlbDeserializeTimeNanos; }
        
        public double getAverageBocParseTimeMs() {
            return totalParsedBlocks > 0 ? 
                (totalBocParseTimeNanos / 1_000_000.0) / totalParsedBlocks : 0.0;
        }
        
        public double getAverageTlbDeserializeTimeMs() {
            return totalParsedBlocks > 0 ? 
                (totalTlbDeserializeTimeNanos / 1_000_000.0) / totalParsedBlocks : 0.0;
        }
        
        public double getSuccessRate() {
            long total = totalParsedBlocks + totalParseErrors;
            return total > 0 ? (double) totalParsedBlocks / total * 100.0 : 0.0;
        }

        @Override
        public String toString() {
            return String.format(
                "ParsingStatistics{blocks=%d, errors=%d, successRate=%.2f%%, " +
                "avgBocParseMs=%.3f, avgTlbDeserializeMs=%.3f}",
                totalParsedBlocks, totalParseErrors, getSuccessRate(),
                getAverageBocParseTimeMs(), getAverageTlbDeserializeTimeMs()
            );
        }
    }

    /**
     * Get the number of parallel threads configured
     */
    public int getParallelThreads() {
        return parallelThreads;
    }
}
