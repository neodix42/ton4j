package org.ton.ton4j.exporter;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicBoolean;
import lombok.extern.slf4j.Slf4j;

/**
 * Ultra-high capacity queue system for buffering processed blocks between
 * processing threads (1000+) and writer threads (32). Designed for aggressive
 * RAM utilization on 128GB+ systems.
 * 
 * This queue acts as a massive buffer that allows processing threads to work
 * at maximum speed without being blocked by disk I/O operations.
 */
@Slf4j
public class MassiveBlockQueue {
    
    // Default queue capacity for 128GB systems - can hold ~1M blocks
    private static final int DEFAULT_QUEUE_CAPACITY = 1_000_000;
    
    // Shutdown signal block
    private static final ProcessedBlock SHUTDOWN_SIGNAL = ProcessedBlock.builder()
        .blockKey("##SHUTDOWN##")
        .archiveKey("##SHUTDOWN##")
        .processedData("##SHUTDOWN##")
        .processingTimestamp(System.currentTimeMillis())
        .processingThreadId(-1)
        .dataSize(0)
        .wasDeserialized(false)
        .processingTimeNanos(0)
        .build();
    
    private final BlockingQueue<ProcessedBlock> queue;
    private final int capacity;
    
    // Statistics and monitoring
    private final AtomicLong totalBlocksQueued = new AtomicLong(0);
    private final AtomicLong totalBlocksDequeued = new AtomicLong(0);
    private final AtomicLong totalBytesQueued = new AtomicLong(0);
    private final AtomicLong totalBytesDequeued = new AtomicLong(0);
    private final AtomicLong maxQueueSize = new AtomicLong(0);
    private final AtomicBoolean isShutdown = new AtomicBoolean(false);
    
    // Performance monitoring
    private volatile long lastStatsTime = System.currentTimeMillis();
    private volatile long lastQueuedCount = 0;
    private volatile long lastDequeuedCount = 0;
    
    /**
     * Creates a massive block queue with default capacity (1M blocks)
     */
    public MassiveBlockQueue() {
        this(DEFAULT_QUEUE_CAPACITY);
    }
    
    /**
     * Creates a massive block queue with specified capacity
     * 
     * @param capacity Maximum number of blocks to buffer in memory
     */
    public MassiveBlockQueue(int capacity) {
        this.capacity = capacity;
        this.queue = new LinkedBlockingQueue<>(capacity);
        
        log.info("MassiveBlockQueue initialized with capacity: {} blocks (~{}GB estimated)", 
            capacity, estimateMemoryUsageGB(capacity));
    }
    
    /**
     * Adds a processed block to the queue. This method will block if the queue is full,
     * providing natural backpressure to processing threads.
     * 
     * @param block The processed block to queue
     * @throws InterruptedException If interrupted while waiting
     */
    public void offer(ProcessedBlock block) throws InterruptedException {
        if (isShutdown.get()) {
            log.warn("Attempted to offer block to shutdown queue: {}", block.toCompactString());
            return;
        }
        
        // This will block if queue is full (backpressure mechanism)
        queue.put(block);
        
        // Update statistics
        totalBlocksQueued.incrementAndGet();
        totalBytesQueued.addAndGet(block.getDataSize());
        
        // Track maximum queue size
        int currentSize = queue.size();
        maxQueueSize.updateAndGet(max -> Math.max(max, currentSize));
    }
    
    /**
     * Attempts to add a block with a timeout
     * 
     * @param block The block to add
     * @param timeout Timeout value
     * @param unit Time unit
     * @return true if block was added, false if timeout occurred
     * @throws InterruptedException If interrupted while waiting
     */
    public boolean offer(ProcessedBlock block, long timeout, TimeUnit unit) throws InterruptedException {
        if (isShutdown.get()) {
            return false;
        }
        
        boolean added = queue.offer(block, timeout, unit);
        if (added) {
            totalBlocksQueued.incrementAndGet();
            totalBytesQueued.addAndGet(block.getDataSize());
            
            int currentSize = queue.size();
            maxQueueSize.updateAndGet(max -> Math.max(max, currentSize));
        }
        
        return added;
    }
    
    /**
     * Takes a block from the queue. This method will block until a block is available
     * or the queue is shutdown.
     * 
     * @return The next processed block, or null if shutdown
     * @throws InterruptedException If interrupted while waiting
     */
    public ProcessedBlock take() throws InterruptedException {
        ProcessedBlock block = queue.take();
        
        // Check for shutdown signal
        if (SHUTDOWN_SIGNAL.equals(block) || "##SHUTDOWN##".equals(block.getBlockKey())) {
            // Put shutdown signal back for other threads
            queue.offer(SHUTDOWN_SIGNAL);
            return null;
        }
        
        // Update statistics
        totalBlocksDequeued.incrementAndGet();
        totalBytesDequeued.addAndGet(block.getDataSize());
        
        return block;
    }
    
    /**
     * Polls for a block with timeout
     * 
     * @param timeout Timeout value
     * @param unit Time unit
     * @return Block if available, null if timeout or shutdown
     * @throws InterruptedException If interrupted while waiting
     */
    public ProcessedBlock poll(long timeout, TimeUnit unit) throws InterruptedException {
        ProcessedBlock block = queue.poll(timeout, unit);
        
        if (block == null) {
            return null; // Timeout
        }
        
        // Check for shutdown signal
        if (SHUTDOWN_SIGNAL.equals(block) || "##SHUTDOWN##".equals(block.getBlockKey())) {
            queue.offer(SHUTDOWN_SIGNAL); // Put back for other threads
            return null;
        }
        
        // Update statistics
        totalBlocksDequeued.incrementAndGet();
        totalBytesDequeued.addAndGet(block.getDataSize());
        
        return block;
    }
    
    /**
     * Initiates shutdown of the queue by sending shutdown signals to all writer threads
     * 
     * @param writerThreadCount Number of writer threads to signal
     */
    public void shutdown(int writerThreadCount) {
        if (isShutdown.getAndSet(true)) {
            return; // Already shutdown
        }
        
        log.info("Shutting down MassiveBlockQueue, sending {} shutdown signals", writerThreadCount);
        
        // Send shutdown signal for each writer thread
        for (int i = 0; i < writerThreadCount; i++) {
            try {
                queue.offer(SHUTDOWN_SIGNAL, 1, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("Interrupted while sending shutdown signal");
                break;
            }
        }
    }
    
    /**
     * Gets current queue size
     */
    public int size() {
        return queue.size();
    }
    
    /**
     * Gets queue capacity
     */
    public int getCapacity() {
        return capacity;
    }
    
    /**
     * Gets current queue utilization as percentage
     */
    public double getUtilizationPercent() {
        return (double) queue.size() / capacity * 100.0;
    }
    
    /**
     * Checks if queue is nearly full (>90% capacity)
     */
    public boolean isNearlyFull() {
        return getUtilizationPercent() > 90.0;
    }
    
    /**
     * Gets total blocks queued since creation
     */
    public long getTotalBlocksQueued() {
        return totalBlocksQueued.get();
    }
    
    /**
     * Gets total blocks dequeued since creation
     */
    public long getTotalBlocksDequeued() {
        return totalBlocksDequeued.get();
    }
    
    /**
     * Gets total bytes queued since creation
     */
    public long getTotalBytesQueued() {
        return totalBytesQueued.get();
    }
    
    /**
     * Gets total bytes dequeued since creation
     */
    public long getTotalBytesDequeued() {
        return totalBytesDequeued.get();
    }
    
    /**
     * Gets maximum queue size reached
     */
    public long getMaxQueueSize() {
        return maxQueueSize.get();
    }
    
    /**
     * Gets current estimated memory usage in bytes
     */
    public long getEstimatedMemoryUsage() {
        // Rough estimate: queue overhead + block data
        long queueOverhead = queue.size() * 200; // Rough object overhead per block
        return queueOverhead + (totalBytesQueued.get() - totalBytesDequeued.get());
    }
    
    /**
     * Gets current estimated memory usage in GB
     */
    public double getEstimatedMemoryUsageGB() {
        return getEstimatedMemoryUsage() / (1024.0 * 1024.0 * 1024.0);
    }
    
    /**
     * Calculates current throughput statistics
     */
    public QueueStats getStats() {
        long currentTime = System.currentTimeMillis();
        long currentQueued = totalBlocksQueued.get();
        long currentDequeued = totalBlocksDequeued.get();
        
        long timeDelta = currentTime - lastStatsTime;
        long queuedDelta = currentQueued - lastQueuedCount;
        long dequeuedDelta = currentDequeued - lastDequeuedCount;
        
        double queueRate = timeDelta > 0 ? (queuedDelta * 1000.0 / timeDelta) : 0.0;
        double dequeueRate = timeDelta > 0 ? (dequeuedDelta * 1000.0 / timeDelta) : 0.0;
        
        // Update for next calculation
        lastStatsTime = currentTime;
        lastQueuedCount = currentQueued;
        lastDequeuedCount = currentDequeued;
        
        return new QueueStats(
            queue.size(),
            capacity,
            getUtilizationPercent(),
            queueRate,
            dequeueRate,
            getEstimatedMemoryUsageGB(),
            maxQueueSize.get()
        );
    }
    
    /**
     * Prints detailed queue statistics
     */
    public void printStats() {
        QueueStats stats = getStats();
        log.info("MassiveBlockQueue Stats: size={}/{} ({:.1f}%), rates: queue={:.1f}/s dequeue={:.1f}/s, " +
                "memory={:.2f}GB, max_size={}", 
            stats.currentSize, stats.capacity, stats.utilizationPercent,
            stats.queueRate, stats.dequeueRate, stats.memoryUsageGB, stats.maxSize);
    }
    
    /**
     * Estimates memory usage for a given capacity
     */
    private static double estimateMemoryUsageGB(int capacity) {
        // Rough estimate: 2KB per block average
        return capacity * 2048.0 / (1024.0 * 1024.0 * 1024.0);
    }
    
    /**
     * Queue statistics data class
     */
    public static class QueueStats {
        public final int currentSize;
        public final int capacity;
        public final double utilizationPercent;
        public final double queueRate;
        public final double dequeueRate;
        public final double memoryUsageGB;
        public final long maxSize;
        
        public QueueStats(int currentSize, int capacity, double utilizationPercent,
                         double queueRate, double dequeueRate, double memoryUsageGB, long maxSize) {
            this.currentSize = currentSize;
            this.capacity = capacity;
            this.utilizationPercent = utilizationPercent;
            this.queueRate = queueRate;
            this.dequeueRate = dequeueRate;
            this.memoryUsageGB = memoryUsageGB;
            this.maxSize = maxSize;
        }
    }
}
