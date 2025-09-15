package org.ton.ton4j.exporter;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import lombok.extern.slf4j.Slf4j;

/**
 * High-performance file writer that uses multiple parallel writers and large RAM buffers
 * to maximize disk I/O utilization and throughput.
 */
@Slf4j
public class HighPerformanceFileWriter implements Closeable {
    
    private static final String SHUTDOWN_SIGNAL = "##SHUTDOWN##";
    private static final int DEFAULT_WRITER_THREADS = 8; // More parallel writers
    private static final int DEFAULT_BUFFER_SIZE_MB = 256; // Much larger buffers
    private static final int DEFAULT_QUEUE_CAPACITY = 200000; // Massive queue for RAM utilization
    private static final int DEFAULT_BATCH_SIZE = 5000; // Much larger batches
    
    private final BlockingQueue<String> writeQueue;
    private final ExecutorService writerExecutor;
    private final AtomicLong totalLinesWritten = new AtomicLong(0);
    private final AtomicBoolean isShutdown = new AtomicBoolean(false);
    private final AtomicInteger activeWriters = new AtomicInteger(0);
    
    private final String baseFilePath;
    private final int writerThreads;
    private final int bufferSizeMB;
    private final int batchSize;
    
    // File splitting for parallel writing
    private final AtomicInteger fileCounter = new AtomicInteger(0);
    private final List<String> createdFiles = new ArrayList<>();
    
    /**
     * Creates a high-performance file writer with default settings
     */
    public HighPerformanceFileWriter(String filePath, boolean append) throws IOException {
        this(filePath, append, DEFAULT_WRITER_THREADS, DEFAULT_BUFFER_SIZE_MB, 
             DEFAULT_QUEUE_CAPACITY, DEFAULT_BATCH_SIZE);
    }
    
    /**
     * Creates a high-performance file writer with custom settings
     */
    public HighPerformanceFileWriter(String filePath, boolean append, int writerThreads, 
                                   int bufferSizeMB, int queueCapacity, int batchSize) throws IOException {
        this.baseFilePath = filePath;
        this.writerThreads = writerThreads;
        this.bufferSizeMB = bufferSizeMB;
        this.batchSize = batchSize;
        
        this.writeQueue = new LinkedBlockingQueue<>(queueCapacity);
        this.writerExecutor = Executors.newFixedThreadPool(writerThreads);
        
        // If not appending, ensure base file doesn't exist
        if (!append) {
            Files.deleteIfExists(Paths.get(filePath));
        }
        
        // Start writer threads
        for (int i = 0; i < writerThreads; i++) {
            final int writerId = i;
            writerExecutor.submit(() -> writerLoop(writerId));
        }
        
        log.info("HighPerformanceFileWriter started: file={}, writers={}, bufferMB={}, queueCapacity={}, batchSize={}", 
            filePath, writerThreads, bufferSizeMB, queueCapacity, batchSize);
    }
    
    /**
     * Writes a line asynchronously with high throughput
     */
    public void writeLine(String line) {
        if (isShutdown.get()) {
            log.warn("Attempted to write to shutdown HighPerformanceFileWriter");
            return;
        }
        
        try {
            // This will block if queue is full (backpressure mechanism)
            writeQueue.put(line);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Interrupted while writing line to queue");
        }
    }
    
    /**
     * Gets the current queue size (for monitoring)
     */
    public int getQueueSize() {
        return writeQueue.size();
    }
    
    /**
     * Gets the total number of lines written
     */
    public long getTotalLinesWritten() {
        return totalLinesWritten.get();
    }
    
    /**
     * Gets the number of active writer threads
     */
    public int getActiveWriters() {
        return activeWriters.get();
    }
    
    /**
     * Writer thread loop that processes batches of lines
     */
    private void writerLoop(int writerId) {
        activeWriters.incrementAndGet();
        String writerFileName = getWriterFileName(writerId);
        
        log.debug("Writer thread {} started, writing to: {}", writerId, writerFileName);
        
        try (BufferedWriter writer = Files.newBufferedWriter(
                Paths.get(writerFileName), 
                StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, 
                StandardOpenOption.APPEND)) {
            
            // Set large buffer size for high throughput
            BufferedWriter bufferedWriter = new BufferedWriter(writer, bufferSizeMB * 1024 * 1024);
            
            List<String> batch = new ArrayList<>(batchSize);
            long lastFlushTime = System.currentTimeMillis();
            int linesWrittenSinceFlush = 0;
            
            while (!isShutdown.get() || !writeQueue.isEmpty()) {
                try {
                    // Collect a batch of lines
                    batch.clear();
                    
                    // Get first line (blocking)
                    String line = writeQueue.poll(100, TimeUnit.MILLISECONDS);
                    if (line == null) {
                        // Timeout - check if we need to flush due to time (every 5 seconds)
                        long currentTime = System.currentTimeMillis();
                        if (linesWrittenSinceFlush > 0 && (currentTime - lastFlushTime) > 5000) {
                            bufferedWriter.flush();
                            lastFlushTime = currentTime;
                            linesWrittenSinceFlush = 0;
                            log.debug("Writer {} flushed due to timeout ({} lines)", writerId, linesWrittenSinceFlush);
                        }
                        continue; // Timeout, check shutdown status
                    }
                    
                    if (SHUTDOWN_SIGNAL.equals(line)) {
                        log.debug("Writer {} received shutdown signal", writerId);
                        break;
                    }
                    
                    batch.add(line);
                    
                    // Collect additional lines for batch (non-blocking)
                    while (batch.size() < batchSize) {
                        String additionalLine = writeQueue.poll();
                        if (additionalLine == null) {
                            break; // No more lines available
                        }
                        
                        if (SHUTDOWN_SIGNAL.equals(additionalLine)) {
                            // Put shutdown signal back for other threads
                            writeQueue.offer(SHUTDOWN_SIGNAL);
                            break;
                        }
                        
                        batch.add(additionalLine);
                    }
                    
                    // Write the entire batch
                    for (String batchLine : batch) {
                        bufferedWriter.write(batchLine);
                        bufferedWriter.newLine();
                    }
                    
                    linesWrittenSinceFlush += batch.size();
                    totalLinesWritten.addAndGet(batch.size());
                    
                    // Flush based on reasonable criteria:
                    // 1. Every 5000 lines (reasonable for 1000 ops/sec = flush every 5 seconds)
                    // 2. Every 10 seconds (backup time-based flush)
                    // 3. When batch is large enough (original logic)
                    long currentTime = System.currentTimeMillis();
                    boolean shouldFlush = linesWrittenSinceFlush >= 5000 || // Every 5000 lines
                                         (currentTime - lastFlushTime) > 10000 || // Every 10 seconds
                                         batch.size() >= batchSize / 2; // Large batch
                    
                    if (shouldFlush) {
                        bufferedWriter.flush();
                        lastFlushTime = currentTime;
                        linesWrittenSinceFlush = 0;
                        log.debug("Writer {} flushed {} lines (total: {})", writerId, batch.size(), totalLinesWritten.get());
                    }
                    
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    log.debug("Writer thread {} interrupted", writerId);
                    break;
                } catch (IOException e) {
                    log.error("Error writing batch in writer {}: {}", writerId, e.getMessage());
                    // Continue processing to avoid losing data
                }
            }
            
            // Final flush
            try {
                bufferedWriter.flush();
                log.debug("Writer {} completed, final flush done", writerId);
            } catch (IOException e) {
                log.error("Error during final flush in writer {}: {}", writerId, e.getMessage());
            }
            
        } catch (IOException e) {
            log.error("Error creating writer {}: {}", writerId, e.getMessage());
        } finally {
            activeWriters.decrementAndGet();
            log.debug("Writer thread {} finished", writerId);
        }
    }
    
    /**
     * Gets the file name for a specific writer thread
     */
    private String getWriterFileName(int writerId) {
        if (writerThreads == 1) {
            return baseFilePath;
        } else {
            // Create separate files for parallel writing
            Path basePath = Paths.get(baseFilePath);
            String fileName = basePath.getFileName().toString();
            String directory = basePath.getParent() != null ? basePath.getParent().toString() : "";
            
            int dotIndex = fileName.lastIndexOf('.');
            String nameWithoutExt = dotIndex > 0 ? fileName.substring(0, dotIndex) : fileName;
            String extension = dotIndex > 0 ? fileName.substring(dotIndex) : "";
            
            String writerFileName = nameWithoutExt + "_part" + writerId + extension;
            String fullPath = directory.isEmpty() ? writerFileName : Paths.get(directory, writerFileName).toString();
            
            synchronized (createdFiles) {
                if (!createdFiles.contains(fullPath)) {
                    createdFiles.add(fullPath);
                }
            }
            
            return fullPath;
        }
    }
    
    /**
     * Merges all part files into the final output file (if multiple writers were used)
     */
    private void mergePartFiles() throws IOException {
        if (writerThreads <= 1 || createdFiles.isEmpty()) {
            return; // No merging needed
        }
        
        log.info("Merging {} part files into final output: {}", createdFiles.size(), baseFilePath);
        
        try (BufferedWriter finalWriter = Files.newBufferedWriter(
                Paths.get(baseFilePath), 
                StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, 
                StandardOpenOption.TRUNCATE_EXISTING)) {
            
            for (String partFile : createdFiles) {
                Path partPath = Paths.get(partFile);
                if (Files.exists(partPath)) {
                    try (BufferedReader reader = Files.newBufferedReader(partPath, StandardCharsets.UTF_8)) {
                        String line;
                        while ((line = reader.readLine()) != null) {
                            finalWriter.write(line);
                            finalWriter.newLine();
                        }
                    }
                    
                    // Delete part file after merging
                    Files.deleteIfExists(partPath);
                    log.debug("Merged and deleted part file: {}", partFile);
                }
            }
        }
        
        log.info("Successfully merged all part files into: {}", baseFilePath);
    }
    
    @Override
    public void close() throws IOException {
        if (isShutdown.getAndSet(true)) {
            return; // Already shutdown
        }
        
        log.info("Shutting down HighPerformanceFileWriter, queue size: {}, total written: {}", 
            writeQueue.size(), totalLinesWritten.get());
        
        // Signal shutdown to all writer threads
        for (int i = 0; i < writerThreads; i++) {
            try {
                writeQueue.put(SHUTDOWN_SIGNAL);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("Interrupted while sending shutdown signal");
                break;
            }
        }
        
        // Shutdown executor and wait for completion
        writerExecutor.shutdown();
        try {
            if (!writerExecutor.awaitTermination(30, TimeUnit.SECONDS)) {
                log.warn("Writer threads did not finish within timeout, forcing shutdown");
                writerExecutor.shutdownNow();
                if (!writerExecutor.awaitTermination(10, TimeUnit.SECONDS)) {
                    log.error("Writer threads did not respond to forced shutdown");
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Interrupted while waiting for writer threads to finish");
            writerExecutor.shutdownNow();
        }
        
        // Merge part files if multiple writers were used
        try {
            mergePartFiles();
        } catch (IOException e) {
            log.error("Error merging part files: {}", e.getMessage());
            throw e;
        }
        
        log.info("HighPerformanceFileWriter closed successfully, total lines written: {}", 
            totalLinesWritten.get());
    }
}
