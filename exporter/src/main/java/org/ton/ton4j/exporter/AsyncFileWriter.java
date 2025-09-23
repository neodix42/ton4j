package org.ton.ton4j.exporter;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.extern.slf4j.Slf4j;

/**
 * Asynchronous file writer that buffers write operations to improve performance and reduce I/O
 * blocking for producer threads.
 */
@Slf4j
public class AsyncFileWriter implements Closeable {

  private static final String SHUTDOWN_SIGNAL = "##SHUTDOWN##";
  private static final int DEFAULT_QUEUE_CAPACITY = 10000;
  private static final int DEFAULT_BUFFER_SIZE = 128 * 1024; // 128KB buffer
  private static final int DEFAULT_FLUSH_INTERVAL =
      5000; // flush every 5000 blocks (reduce I/O frequency)

  private final BlockingQueue<String> writeQueue;
  private final BufferedWriter bufferedWriter;
  private final Thread writerThread;
  private final AtomicInteger blocksWritten = new AtomicInteger(0);
  private final AtomicInteger totalBlocksWritten = new AtomicInteger(0);
  private final AtomicBoolean isShutdown = new AtomicBoolean(false);
  private final AtomicBoolean writerThreadRunning = new AtomicBoolean(false);

  private volatile int flushInterval;
  private volatile boolean flushOnPackageComplete = true;

  /** Creates an AsyncFileWriter with default settings */
  public AsyncFileWriter(String filePath, boolean append) throws IOException {
    this(filePath, append, DEFAULT_QUEUE_CAPACITY, DEFAULT_BUFFER_SIZE, DEFAULT_FLUSH_INTERVAL);
  }

  /** Creates an AsyncFileWriter with custom settings */
  public AsyncFileWriter(
      String filePath, boolean append, int queueCapacity, int bufferSize, int flushInterval)
      throws IOException {
    this.writeQueue = new LinkedBlockingQueue<>(queueCapacity);
    this.flushInterval = flushInterval;

    // Create buffered writer with large buffer
    FileWriter fileWriter = new FileWriter(filePath, StandardCharsets.UTF_8, append);
    this.bufferedWriter = new BufferedWriter(fileWriter, bufferSize);

    // Start background writer thread
    this.writerThread =
        new Thread(this::writerLoop, "AsyncFileWriter-" + new File(filePath).getName());
    this.writerThread.setDaemon(false); // Ensure thread completes before JVM shutdown
    this.writerThread.start();

    log.debug(
        "AsyncFileWriter started: file={}, queueCapacity={}, bufferSize={}, flushInterval={}",
        filePath,
        queueCapacity,
        bufferSize,
        flushInterval);
  }

  /**
   * Writes a line asynchronously. This method is non-blocking for producer threads. If the queue is
   * full, it will block until space is available (backpressure).
   */
  public void writeLine(String line) {
    if (isShutdown.get()) {
      log.warn("Attempted to write to shutdown AsyncFileWriter");
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

  /** Forces a flush of the buffer to disk */
  public void flush() {
    try {
      writeQueue.put("##FLUSH##");
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      log.warn("Interrupted while requesting flush");
    }
  }

  /** Signals that a package has been completed, triggering a flush if configured */
  public void onPackageComplete() {
    if (flushOnPackageComplete) {
      flush();
    }
  }

  /** Sets the flush interval (number of blocks between flushes) */
  public void setFlushInterval(int blocks) {
    this.flushInterval = blocks;
    log.debug("Flush interval set to {} blocks", blocks);
  }

  /** Sets whether to flush on package completion */
  public void setFlushOnPackageComplete(boolean flush) {
    this.flushOnPackageComplete = flush;
    log.debug("Flush on package complete: {}", flush);
  }

  /** Gets the current queue size (for monitoring) */
  public int getQueueSize() {
    return writeQueue.size();
  }

  /** Gets the total number of blocks written */
  public int getTotalBlocksWritten() {
    return totalBlocksWritten.get();
  }

  /** Checks if the writer thread is still running */
  public boolean isWriterThreadRunning() {
    return writerThreadRunning.get();
  }

  /** Background writer thread that consumes from the queue and writes to disk */
  private void writerLoop() {
    writerThreadRunning.set(true);

    try {
      while (!isShutdown.get() || !writeQueue.isEmpty()) {
        try {
          // Poll with timeout to allow periodic checks for shutdown
          String line = writeQueue.poll(100, TimeUnit.MILLISECONDS);

          if (line == null) {
            continue; // Timeout, check shutdown status
          }

          if (SHUTDOWN_SIGNAL.equals(line)) {
            //            log.debug("Received shutdown signal, draining remaining queue");
            break;
          }

          if ("##FLUSH##".equals(line)) {
            // Force flush request
            bufferedWriter.flush();
            log.debug("Forced flush completed, {} blocks written so far", totalBlocksWritten.get());
            continue;
          }

          // Write the line
          bufferedWriter.write(line);
          bufferedWriter.newLine();

          int written = blocksWritten.incrementAndGet();
          totalBlocksWritten.incrementAndGet();

          // Check if we need to flush based on interval
          if (written >= flushInterval) {
            bufferedWriter.flush();
            blocksWritten.set(0); // Reset counter
            // log.debug("Auto-flush completed after {} blocks (total: {})", written,
            // totalBlocksWritten.get());
          }

        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
          log.debug("Writer thread interrupted");
          break;
        } catch (IOException e) {
          log.error("Error writing to file: {}", e.getMessage());
          // Continue processing to avoid losing data
        }
      }

      // Drain any remaining items in the queue
      String line;
      while ((line = writeQueue.poll()) != null) {
        if (!SHUTDOWN_SIGNAL.equals(line) && !"##FLUSH##".equals(line)) {
          try {
            bufferedWriter.write(line);
            bufferedWriter.newLine();
            totalBlocksWritten.incrementAndGet();
          } catch (IOException e) {
            log.error("Error writing remaining line during shutdown: {}", e.getMessage());
          }
        }
      }

      try {
        bufferedWriter.flush();
      } catch (IOException e) {
        log.error("Error during final flush: {}", e.getMessage());
      }

    } finally {
      writerThreadRunning.set(false);
    }
  }

  @Override
  public void close() throws IOException {
    if (isShutdown.getAndSet(true)) {
      return; // Already shutdown
    }

    // Signal shutdown to writer thread
    try {
      writeQueue.put(SHUTDOWN_SIGNAL);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      log.warn("Interrupted while sending shutdown signal");
    }

    // Wait for writer thread to finish
    try {
      writerThread.join(5000); // Wait up to 5 seconds
      if (writerThread.isAlive()) {
        log.warn("Writer thread did not finish within timeout, interrupting");
        writerThread.interrupt();
        writerThread.join(1000); // Give it another second
      }
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      log.warn("Interrupted while waiting for writer thread to finish");
    }

    // Close the buffered writer
    try {
      bufferedWriter.close();
    } catch (IOException e) {
      log.error("Error closing buffered writer: {}", e.getMessage());
      throw e;
    }
  }
}
