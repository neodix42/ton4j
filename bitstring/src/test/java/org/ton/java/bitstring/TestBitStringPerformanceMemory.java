package org.ton.java.bitstring;

import static org.junit.Assert.assertTrue;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.openjdk.jol.info.GraphLayout;
import org.ton.java.address.Address;

/**
 * Tests that compare memory consumption between BitString and RealBitString implementations. These
 * tests demonstrate the difference in memory footprint between the two approaches: - BitString:
 * Uses Deque<Boolean> (one Boolean object per bit) - RealBitString: Uses byte[] (one bit per bit)
 */
@Slf4j
@RunWith(JUnit4.class)
public class TestBitStringPerformanceMemory {

  /** Tests the memory footprint of empty instances of both BitString and RealBitString. */
  @Test
  public void testEmptyInstanceMemoryFootprint() {
    BitString1 bitString = new BitString1(0);
    BitString realBitString = new BitString(0);

    long bitStringMemory = GraphLayout.parseInstance(bitString).totalSize();
    long realBitStringMemory = GraphLayout.parseInstance(realBitString).totalSize();

    log.info("Empty BitString memory: {} bytes", bitStringMemory);
    log.info("Empty RealBitString memory: {} bytes", realBitStringMemory);
    log.info("Memory difference: {} bytes", Math.abs(bitStringMemory - realBitStringMemory));
    log.info("Ratio: BitString/RealBitString = {}", (double) bitStringMemory / realBitStringMemory);

    // No assertion here as we're just measuring and reporting
  }

  /** Tests how memory usage scales with the size of the BitString/RealBitString. */
  @Test
  public void testMemoryScalingWithSize() {
    int[] sizes = {8, 16, 32, 64, 128, 256, 512, 1024, 2048, 4096, 8192};

    log.info("Memory scaling with size:");
    log.info(
        "Size (bits) | BitString (bytes) | RealBitString (bytes) | Ratio (BitString/RealBitString)");

    for (int size : sizes) {
      BitString1 bitString = new BitString1(size);
      BitString realBitString = new BitString(size);

      // Fill with random bits to ensure memory is allocated
      for (int i = 0; i < size; i++) {
        boolean bit = Math.random() > 0.5;
        bitString.writeBit(bit);
        realBitString.writeBit(bit);
      }

      long bitStringMemory = GraphLayout.parseInstance(bitString).totalSize();
      long realBitStringMemory = GraphLayout.parseInstance(realBitString).totalSize();
      double ratio = (double) bitStringMemory / realBitStringMemory;

      log.info("{} | {} | {} | {}", size, bitStringMemory, realBitStringMemory, ratio);

      // As size increases, we expect the memory difference to become more significant
      if (size > 1000) {
        // This assertion might need adjustment based on actual measurements
        assertTrue(
            "BitString should use significantly more memory for large sizes",
            bitStringMemory > realBitStringMemory);
      }
    }
  }

  /** Tests memory consumption when performing typical operations on both implementations. */
  @Test
  public void testMemoryConsumptionWithTypicalOperations() {
    BitString1 bitString = new BitString1(1024);
    BitString realBitString = new BitString(1024);

    log.info("Memory consumption with typical operations:");
    log.info("Operation | BitString (bytes) | RealBitString (bytes) | Ratio");

    // Initial state
    measureAndLog("Initial", bitString, realBitString);

    // After writing 32-bit integer
    bitString.writeUint(12345, 32);
    realBitString.writeUint(12345, 32);
    measureAndLog("Write uint(32)", bitString, realBitString);

    // After writing 64-bit integer
    bitString.writeUint(BigInteger.valueOf(Long.MAX_VALUE), 64);
    realBitString.writeUint(BigInteger.valueOf(Long.MAX_VALUE), 64);
    measureAndLog("Write uint(64)", bitString, realBitString);

    // After writing string
    bitString.writeString("Hello, TON!");
    realBitString.writeString("Hello, TON!");
    measureAndLog("Write string", bitString, realBitString);

    // After writing address
    Address address = Address.of("0QAs9VlT6S776tq3unJcP5Ogsj-ELLunLXuOb1EKcOQi4-QO");
    bitString.writeAddress(address);
    realBitString.writeAddress(address);
    measureAndLog("Write address", bitString, realBitString);
  }

  /** Tests memory usage when creating multiple instances of both classes. */
  @Test
  public void testMemoryUsageWithMultipleInstances() {
    int instanceCount = 1000;
    int instanceSize = 128;

    List<BitString1> bitStrings = new ArrayList<>(instanceCount);
    List<BitString> realBitStrings = new ArrayList<>(instanceCount);

    // Create BitString instances
    for (int i = 0; i < instanceCount; i++) {
      BitString1 bs = new BitString1(instanceSize);
      for (int j = 0; j < instanceSize; j++) {
        bs.writeBit(j % 2 == 0);
      }
      bitStrings.add(bs);
    }

    // Create RealBitString instances
    for (int i = 0; i < instanceCount; i++) {
      BitString rbs = new BitString(instanceSize);
      for (int j = 0; j < instanceSize; j++) {
        rbs.writeBit(j % 2 == 0);
      }
      realBitStrings.add(rbs);
    }

    // Measure total memory
    long bitStringTotalMemory = GraphLayout.parseInstance(bitStrings).totalSize();
    long realBitStringTotalMemory = GraphLayout.parseInstance(realBitStrings).totalSize();

    log.info("Memory usage with {} instances of size {}:", instanceCount, instanceSize);
    log.info("BitString total: {} bytes", bitStringTotalMemory);
    log.info("RealBitString total: {} bytes", realBitStringTotalMemory);
    log.info(
        "Memory difference: {} bytes", Math.abs(bitStringTotalMemory - realBitStringTotalMemory));
    log.info(
        "Ratio: BitString/RealBitString = {}",
        (double) bitStringTotalMemory / realBitStringTotalMemory);

    // Calculate average memory per instance
    double bitStringAvg = (double) bitStringTotalMemory / instanceCount;
    double realBitStringAvg = (double) realBitStringTotalMemory / instanceCount;

    log.info("Average memory per BitString instance: {} bytes", bitStringAvg);
    log.info("Average memory per RealBitString instance: {} bytes", realBitStringAvg);
  }

  /** Tests memory consumption with very large bit strings. */
  @Test
  public void testLargeBitStringMemoryConsumption() {
    int size = 100_000; // 100K bits

    BitString1 bitString = new BitString1(size);
    BitString realBitString = new BitString(size);

    // Fill with alternating bits
    for (int i = 0; i < size; i++) {
      boolean bit = i % 2 == 0;
      bitString.writeBit(bit);
      realBitString.writeBit(bit);
    }

    long bitStringMemory = GraphLayout.parseInstance(bitString).totalSize();
    long realBitStringMemory = GraphLayout.parseInstance(realBitString).totalSize();

    log.info("Memory consumption for {} bits:", size);
    log.info("BitString: {} bytes", bitStringMemory);
    log.info("RealBitString: {} bytes", realBitStringMemory);
    log.info("Memory difference: {} bytes", Math.abs(bitStringMemory - realBitStringMemory));
    log.info("Ratio: BitString/RealBitString = {}", (double) bitStringMemory / realBitStringMemory);

    // Calculate bytes per bit
    double bitStringBytesPerBit = (double) bitStringMemory / size;
    double realBitStringBytesPerBit = (double) realBitStringMemory / size;

    log.info("BitString uses {} bytes per bit", bitStringBytesPerBit);
    log.info("RealBitString uses {} bytes per bit", realBitStringBytesPerBit);

    // For large bit strings, we expect BitString to use significantly more memory
    assertTrue(
        "BitString should use more memory for large bit strings",
        bitStringMemory > realBitStringMemory);
  }

  /** Helper method to measure and log memory usage for both implementations. */
  private void measureAndLog(String operation, BitString1 bitString, BitString realBitString) {
    long bitStringMemory = GraphLayout.parseInstance(bitString).totalSize();
    long realBitStringMemory = GraphLayout.parseInstance(realBitString).totalSize();
    double ratio = (double) bitStringMemory / realBitStringMemory;

    log.info("{} | {} | {} | {}", operation, bitStringMemory, realBitStringMemory, ratio);
  }
}
