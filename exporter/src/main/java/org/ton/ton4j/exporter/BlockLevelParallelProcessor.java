package org.ton.ton4j.exporter;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import lombok.extern.slf4j.Slf4j;
import org.ton.ton4j.exporter.reader.ArchiveDbReader;
import org.ton.ton4j.exporter.types.ArchiveInfo;

/**
 * Block-level parallel processor that implements Phase 3.1 optimization:
 * - Splits large packages into block-level work units
 * - Uses work-stealing queues for load balancing
 * - Enables parallel BOC parsing within packages
 */
@Slf4j
public class BlockLevelParallelProcessor {

    /** Work unit representing a single block processing task */
    public static class BlockWorkUnit {
        private final String blockKey;
        private final byte[] blockData;
        private final String archiveKey;

        public BlockWorkUnit(String blockKey, byte[] blockData, String archiveKey) {
            this.blockKey = blockKey;
            this.blockData = blockData;
            this.archiveKey = archiveKey;
        }

        public String getBlockKey() { return blockKey; }
        public byte[] getBlockData() { return blockData; }
        public String getArchiveKey() { return archiveKey; }
    }

    /** Interface for processing individual blocks */
    @FunctionalInterface
    public interface BlockProcessor {
        void process(BlockWorkUnit workUnit);
    }

    private final int parallelThreads;
    private final ForkJoinPool workStealingPool;
    private volatile boolean shutdownRequested = false;

    /**
     * Creates a new block-level parallel processor
     * 
     * @param parallelThreads Number of threads for parallel processing
     */
    public BlockLevelParallelProcessor(int parallelThreads) {
        this.parallelThreads = parallelThreads;
        // Use ForkJoinPool for work-stealing capabilities
        this.workStealingPool = new ForkJoinPool(
            parallelThreads,
            ForkJoinPool.defaultForkJoinWorkerThreadFactory,
            null,
            true // Enable async mode for better work-stealing
        );
        
        log.info("Initialized BlockLevelParallelProcessor with {} threads using work-stealing pool", 
                parallelThreads);
    }

    /**
     * Process a single archive package with block-level parallelization
     * 
     * @param archiveKey The archive key
     * @param archiveInfo Archive information
     * @param archiveDbReader The archive database reader
     * @param blockProcessor Processor for individual blocks
     * @return Number of blocks processed
     */
    public int processArchiveParallel(
            String archiveKey,
            ArchiveInfo archiveInfo,
            ArchiveDbReader archiveDbReader,
            BlockProcessor blockProcessor) {

        if (shutdownRequested) {
            log.debug("Shutdown requested, skipping archive: {}", archiveKey);
            return 0;
        }

        AtomicInteger processedBlocks = new AtomicInteger(0);
        
        // Create a blocking queue to collect work units from the archive
        BlockingQueue<BlockWorkUnit> workQueue = new LinkedBlockingQueue<>();
        
        try {
            // Phase 1: Extract all blocks from the package into work units
            // This runs in a separate thread to avoid blocking the work-stealing pool
            CompletableFuture<Void> extractionFuture = CompletableFuture.runAsync(() -> {
                try {
                    if (archiveInfo.getIndexPath() == null) {
                        // Files package
                        archiveDbReader.streamFromFilesPackage(archiveKey, archiveInfo, 
                            (blockKey, blockData) -> {
                                if (!shutdownRequested) {
                                    workQueue.offer(new BlockWorkUnit(blockKey, blockData, archiveKey));
                                }
                            });
                    } else {
                        // Traditional archive
                        archiveDbReader.streamFromTraditionalArchive(archiveKey, archiveInfo,
                            (blockKey, blockData) -> {
                                if (!shutdownRequested) {
                                    workQueue.offer(new BlockWorkUnit(blockKey, blockData, archiveKey));
                                }
                            });
                    }
                } catch (Exception e) {
                    log.warn("Error extracting blocks from archive {}: {}", archiveKey, e.getMessage());
                } finally {
                    // Signal end of extraction with a poison pill
                    workQueue.offer(new BlockWorkUnit(null, null, null));
                }
            });

            // Phase 2: Process work units in parallel using work-stealing
            List<CompletableFuture<Void>> processingFutures = new ArrayList<>();
            
            // Create multiple worker tasks that steal work from the queue
            for (int i = 0; i < parallelThreads; i++) {
                CompletableFuture<Void> workerFuture = CompletableFuture.runAsync(() -> {
                    while (!shutdownRequested) {
                        try {
                            // Poll for work with timeout to avoid indefinite blocking
                            BlockWorkUnit workUnit = workQueue.poll(100, TimeUnit.MILLISECONDS);
                            
                            if (workUnit == null) {
                                continue; // No work available, try again
                            }
                            
                            // Check for poison pill (end of work)
                            if (workUnit.getBlockKey() == null) {
                                // Put poison pill back for other workers
                                workQueue.offer(workUnit);
                                break;
                            }
                            
                            // Process the block
                            blockProcessor.process(workUnit);
                            processedBlocks.incrementAndGet();
                            
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            break;
                        } catch (Exception e) {
                            log.warn("Error processing block in archive {}: {}", archiveKey, e.getMessage());
                        }
                    }
                }, workStealingPool);
                
                processingFutures.add(workerFuture);
            }

            // Wait for extraction to complete
            extractionFuture.get(30, TimeUnit.SECONDS);
            
            // Wait for all processing to complete
            CompletableFuture<Void> allProcessing = CompletableFuture.allOf(
                processingFutures.toArray(new CompletableFuture[0]));
            allProcessing.get(60, TimeUnit.SECONDS);

            log.debug("Completed parallel processing of archive {}: {} blocks", 
                     archiveKey, processedBlocks.get());

        } catch (TimeoutException e) {
            log.warn("Timeout processing archive {}: {}", archiveKey, e.getMessage());
        } catch (Exception e) {
            log.error("Error in parallel processing of archive {}: {}", archiveKey, e.getMessage());
        }

        return processedBlocks.get();
    }

    /**
     * Process multiple archives with block-level parallelization across all archives
     * This method provides even better parallelization by mixing blocks from different archives
     * 
     * @param archiveEntries List of archive entries to process
     * @param archiveDbReader The archive database reader
     * @param blockProcessor Processor for individual blocks
     * @return Total number of blocks processed
     */
    public int processMultipleArchivesParallel(
            List<java.util.Map.Entry<String, ArchiveInfo>> archiveEntries,
            ArchiveDbReader archiveDbReader,
            BlockProcessor blockProcessor) {

        if (shutdownRequested) {
            log.debug("Shutdown requested, skipping multiple archives processing");
            return 0;
        }

        AtomicInteger totalProcessedBlocks = new AtomicInteger(0);
        
        // Create a large work-stealing queue that can hold blocks from all archives
        BlockingQueue<BlockWorkUnit> globalWorkQueue = new LinkedBlockingQueue<>();
        AtomicInteger activeExtractors = new AtomicInteger(archiveEntries.size());
        
        try {
            // Phase 1: Extract blocks from all archives concurrently
            List<CompletableFuture<Void>> extractionFutures = new ArrayList<>();
            
            for (java.util.Map.Entry<String, ArchiveInfo> entry : archiveEntries) {
                String archiveKey = entry.getKey();
                ArchiveInfo archiveInfo = entry.getValue();
                
                CompletableFuture<Void> extractionFuture = CompletableFuture.runAsync(() -> {
                    try {
                        if (archiveInfo.getIndexPath() == null) {
                            // Files package
                            archiveDbReader.streamFromFilesPackage(archiveKey, archiveInfo, 
                                (blockKey, blockData) -> {
                                    if (!shutdownRequested) {
                                        globalWorkQueue.offer(new BlockWorkUnit(blockKey, blockData, archiveKey));
                                    }
                                });
                        } else {
                            // Traditional archive
                            archiveDbReader.streamFromTraditionalArchive(archiveKey, archiveInfo,
                                (blockKey, blockData) -> {
                                    if (!shutdownRequested) {
                                        globalWorkQueue.offer(new BlockWorkUnit(blockKey, blockData, archiveKey));
                                    }
                                });
                        }
                    } catch (Exception e) {
                        log.warn("Error extracting blocks from archive {}: {}", archiveKey, e.getMessage());
                    } finally {
                        // Decrement active extractors count
                        if (activeExtractors.decrementAndGet() == 0) {
                            // Last extractor - signal end with poison pill
                            globalWorkQueue.offer(new BlockWorkUnit(null, null, null));
                        }
                    }
                });
                
                extractionFutures.add(extractionFuture);
            }

            // Phase 2: Process work units in parallel using work-stealing across all archives
            List<CompletableFuture<Void>> processingFutures = new ArrayList<>();
            
            // Create worker tasks that steal work from the global queue
            for (int i = 0; i < parallelThreads; i++) {
                CompletableFuture<Void> workerFuture = CompletableFuture.runAsync(() -> {
                    while (!shutdownRequested) {
                        try {
                            // Poll for work with timeout
                            BlockWorkUnit workUnit = globalWorkQueue.poll(200, TimeUnit.MILLISECONDS);
                            
                            if (workUnit == null) {
                                // Check if all extractors are done and queue is empty
                                if (activeExtractors.get() == 0 && globalWorkQueue.isEmpty()) {
                                    break;
                                }
                                continue; // No work available, try again
                            }
                            
                            // Check for poison pill (end of work)
                            if (workUnit.getBlockKey() == null) {
                                // Put poison pill back for other workers
                                globalWorkQueue.offer(workUnit);
                                break;
                            }
                            
                            // Process the block
                            blockProcessor.process(workUnit);
                            totalProcessedBlocks.incrementAndGet();
                            
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            break;
                        } catch (Exception e) {
                            log.warn("Error processing block: {}", e.getMessage());
                        }
                    }
                }, workStealingPool);
                
                processingFutures.add(workerFuture);
            }

            // Wait for all extraction to complete
            CompletableFuture<Void> allExtraction = CompletableFuture.allOf(
                extractionFutures.toArray(new CompletableFuture[0]));
            allExtraction.get(60, TimeUnit.SECONDS);
            
            // Wait for all processing to complete
            CompletableFuture<Void> allProcessing = CompletableFuture.allOf(
                processingFutures.toArray(new CompletableFuture[0]));
            allProcessing.get(120, TimeUnit.SECONDS);

            log.info("Completed parallel processing of {} archives: {} total blocks", 
                    archiveEntries.size(), totalProcessedBlocks.get());

        } catch (TimeoutException e) {
            log.warn("Timeout processing multiple archives: {}", e.getMessage());
        } catch (Exception e) {
            log.error("Error in parallel processing of multiple archives: {}", e.getMessage());
        }

        return totalProcessedBlocks.get();
    }

    /**
     * Request shutdown of the parallel processor
     */
    public void requestShutdown() {
        shutdownRequested = true;
        log.info("Shutdown requested for BlockLevelParallelProcessor");
    }

    /**
     * Shutdown the processor and clean up resources
     */
    public void shutdown() {
        requestShutdown();
        
        if (workStealingPool != null && !workStealingPool.isShutdown()) {
            workStealingPool.shutdown();
            try {
                if (!workStealingPool.awaitTermination(10, TimeUnit.SECONDS)) {
                    log.warn("Work-stealing pool did not terminate within 10 seconds, forcing shutdown");
                    workStealingPool.shutdownNow();
                    if (!workStealingPool.awaitTermination(5, TimeUnit.SECONDS)) {
                        log.error("Work-stealing pool did not respond to forced shutdown");
                    }
                }
            } catch (InterruptedException e) {
                log.warn("Interrupted while waiting for work-stealing pool to terminate");
                workStealingPool.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
        
        log.info("BlockLevelParallelProcessor shutdown completed");
    }

    /**
     * Get the number of parallel threads configured
     */
    public int getParallelThreads() {
        return parallelThreads;
    }

    /**
     * Check if shutdown has been requested
     */
    public boolean isShutdownRequested() {
        return shutdownRequested;
    }
}
