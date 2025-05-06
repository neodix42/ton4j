package org.ton.java.bitstring;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.math.BigInteger;
import java.util.Random;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.ton.java.address.Address;

/**
 * Tests that compare CPU performance between BitString and RealBitString implementations.
 * These tests measure execution time for various operations to determine which implementation
 * is more CPU-efficient.
 */
@Slf4j
@RunWith(JUnit4.class)
public class TestBitStringPerformanceCpu {

    private static final int SMALL_SIZE = 100;
    private static final int MEDIUM_SIZE = 1_000;
    private static final int LARGE_SIZE = 10_000;
    private static final int WARMUP_ITERATIONS = 5;
    private static final int TEST_ITERATIONS = 10;
    private static final Random RANDOM = new Random(42); // Fixed seed for reproducibility

    /**
     * Tests the performance of writing individual bits.
     */
    @Test
    public void testWriteBitPerformance() {
        log.info("=== Write Bit Performance Test ===");
        System.out.println("Starting Write Bit Performance Test");
        
        // Generate random bits to write
        boolean[] bits = new boolean[LARGE_SIZE];
        for (int i = 0; i < LARGE_SIZE; i++) {
            bits[i] = RANDOM.nextBoolean();
        }
        
        // Warm-up
        for (int i = 0; i < WARMUP_ITERATIONS; i++) {
            BitString bs = new BitString(LARGE_SIZE);
            RealBitString rbs = new RealBitString(LARGE_SIZE);
            
            for (int j = 0; j < SMALL_SIZE; j++) {
                bs.writeBit(bits[j]);
                rbs.writeBit(bits[j]);
            }
        }
        
        // Test BitString
        long[] bitStringTimes = new long[TEST_ITERATIONS];
        for (int i = 0; i < TEST_ITERATIONS; i++) {
            BitString bs = new BitString(LARGE_SIZE);
            
            long startTime = System.nanoTime();
            for (int j = 0; j < LARGE_SIZE; j++) {
                bs.writeBit(bits[j]);
            }
            long endTime = System.nanoTime();
            
            bitStringTimes[i] = endTime - startTime;
        }
        
        // Test RealBitString
        long[] realBitStringTimes = new long[TEST_ITERATIONS];
        for (int i = 0; i < TEST_ITERATIONS; i++) {
            RealBitString rbs = new RealBitString(LARGE_SIZE);
            
            long startTime = System.nanoTime();
            for (int j = 0; j < LARGE_SIZE; j++) {
                rbs.writeBit(bits[j]);
            }
            long endTime = System.nanoTime();
            
            realBitStringTimes[i] = endTime - startTime;
        }
        
        // Calculate average times
        double bitStringAvg = calculateAverage(bitStringTimes);
        double realBitStringAvg = calculateAverage(realBitStringTimes);
        
        log.info("BitString write bit average time: {} ns ({} ms)", 
                String.format("%.2f", bitStringAvg), 
                String.format("%.2f", bitStringAvg / 1_000_000));
        log.info("RealBitString write bit average time: {} ns ({} ms)", 
                String.format("%.2f", realBitStringAvg), 
                String.format("%.2f", realBitStringAvg / 1_000_000));
        log.info("Ratio (BitString/RealBitString): {}", 
                String.format("%.2f", bitStringAvg / realBitStringAvg));
    }

    /**
     * Tests the performance of reading individual bits.
     */
    @Test
    public void testReadBitPerformance() {
        log.info("=== Read Bit Performance Test ===");
        
        // Generate random bits to write
        boolean[] bits = new boolean[LARGE_SIZE];
        for (int i = 0; i < LARGE_SIZE; i++) {
            bits[i] = RANDOM.nextBoolean();
        }
        
        // Create and fill BitString and RealBitString
        BitString bs = new BitString(LARGE_SIZE);
        RealBitString rbs = new RealBitString(LARGE_SIZE);
        
        for (int i = 0; i < LARGE_SIZE; i++) {
            bs.writeBit(bits[i]);
            rbs.writeBit(bits[i]);
        }
        
        // Warm-up
        for (int i = 0; i < WARMUP_ITERATIONS; i++) {
            BitString bsClone = bs.clone();
            RealBitString rbsClone = rbs.clone();
            
            for (int j = 0; j < SMALL_SIZE; j++) {
                bsClone.readBit();
                rbsClone.readBit();
            }
        }
        
        // Test BitString
        long[] bitStringTimes = new long[TEST_ITERATIONS];
        for (int i = 0; i < TEST_ITERATIONS; i++) {
            BitString bsClone = bs.clone();
            
            long startTime = System.nanoTime();
            for (int j = 0; j < LARGE_SIZE; j++) {
                bsClone.readBit();
            }
            long endTime = System.nanoTime();
            
            bitStringTimes[i] = endTime - startTime;
        }
        
        // Test RealBitString
        long[] realBitStringTimes = new long[TEST_ITERATIONS];
        for (int i = 0; i < TEST_ITERATIONS; i++) {
            RealBitString rbsClone = rbs.clone();
            
            long startTime = System.nanoTime();
            for (int j = 0; j < LARGE_SIZE; j++) {
                rbsClone.readBit();
            }
            long endTime = System.nanoTime();
            
            realBitStringTimes[i] = endTime - startTime;
        }
        
        // Calculate average times
        double bitStringAvg = calculateAverage(bitStringTimes);
        double realBitStringAvg = calculateAverage(realBitStringTimes);
        
        log.info("BitString read bit average time: {} ns ({} ms)", 
                String.format("%.2f", bitStringAvg), 
                String.format("%.2f", bitStringAvg / 1_000_000));
        log.info("RealBitString read bit average time: {} ns ({} ms)", 
                String.format("%.2f", realBitStringAvg), 
                String.format("%.2f", realBitStringAvg / 1_000_000));
        log.info("Ratio (BitString/RealBitString): {}", 
                String.format("%.2f", bitStringAvg / realBitStringAvg));
    }

    /**
     * Tests the performance of writing unsigned integers.
     */
    @Test
    public void testWriteUintPerformance() {
        log.info("=== Write Uint Performance Test ===");
        
        // Generate random integers to write
        BigInteger[] values = new BigInteger[MEDIUM_SIZE];
        for (int i = 0; i < MEDIUM_SIZE; i++) {
            values[i] = new BigInteger(32, RANDOM);
        }
        
        // Warm-up
        for (int i = 0; i < WARMUP_ITERATIONS; i++) {
            BitString bs = new BitString(MEDIUM_SIZE * 32);
            RealBitString rbs = new RealBitString(MEDIUM_SIZE * 32);
            
            for (int j = 0; j < SMALL_SIZE / 10; j++) {
                bs.writeUint(values[j], 32);
                rbs.writeUint(values[j], 32);
            }
        }
        
        // Test BitString
        long[] bitStringTimes = new long[TEST_ITERATIONS];
        for (int i = 0; i < TEST_ITERATIONS; i++) {
            BitString bs = new BitString(MEDIUM_SIZE * 32);
            
            long startTime = System.nanoTime();
            for (int j = 0; j < MEDIUM_SIZE; j++) {
                bs.writeUint(values[j], 32);
            }
            long endTime = System.nanoTime();
            
            bitStringTimes[i] = endTime - startTime;
        }
        
        // Test RealBitString
        long[] realBitStringTimes = new long[TEST_ITERATIONS];
        for (int i = 0; i < TEST_ITERATIONS; i++) {
            RealBitString rbs = new RealBitString(MEDIUM_SIZE * 32);
            
            long startTime = System.nanoTime();
            for (int j = 0; j < MEDIUM_SIZE; j++) {
                rbs.writeUint(values[j], 32);
            }
            long endTime = System.nanoTime();
            
            realBitStringTimes[i] = endTime - startTime;
        }
        
        // Calculate average times
        double bitStringAvg = calculateAverage(bitStringTimes);
        double realBitStringAvg = calculateAverage(realBitStringTimes);
        
        log.info("BitString write uint average time: {} ns ({} ms)", 
                String.format("%.2f", bitStringAvg), 
                String.format("%.2f", bitStringAvg / 1_000_000));
        log.info("RealBitString write uint average time: {} ns ({} ms)", 
                String.format("%.2f", realBitStringAvg), 
                String.format("%.2f", realBitStringAvg / 1_000_000));
        log.info("Ratio (BitString/RealBitString): {}", 
                String.format("%.2f", bitStringAvg / realBitStringAvg));
    }

    /**
     * Tests the performance of reading unsigned integers.
     */
    @Test
    public void testReadUintPerformance() {
        log.info("=== Read Uint Performance Test ===");
        
        // Generate random integers to write
        BigInteger[] values = new BigInteger[MEDIUM_SIZE];
        for (int i = 0; i < MEDIUM_SIZE; i++) {
            values[i] = new BigInteger(32, RANDOM);
        }
        
        // Create and fill BitString and RealBitString
        BitString bs = new BitString(MEDIUM_SIZE * 32);
        RealBitString rbs = new RealBitString(MEDIUM_SIZE * 32);
        
        for (int i = 0; i < MEDIUM_SIZE; i++) {
            bs.writeUint(values[i], 32);
            rbs.writeUint(values[i], 32);
        }
        
        // Warm-up
        for (int i = 0; i < WARMUP_ITERATIONS; i++) {
            BitString bsClone = bs.clone();
            RealBitString rbsClone = rbs.clone();
            
            for (int j = 0; j < SMALL_SIZE / 10; j++) {
                bsClone.readUint(32);
                rbsClone.readUint(32);
            }
        }
        
        // Test BitString
        long[] bitStringTimes = new long[TEST_ITERATIONS];
        for (int i = 0; i < TEST_ITERATIONS; i++) {
            BitString bsClone = bs.clone();
            
            long startTime = System.nanoTime();
            for (int j = 0; j < MEDIUM_SIZE; j++) {
                bsClone.readUint(32);
            }
            long endTime = System.nanoTime();
            
            bitStringTimes[i] = endTime - startTime;
        }
        
        // Test RealBitString
        long[] realBitStringTimes = new long[TEST_ITERATIONS];
        for (int i = 0; i < TEST_ITERATIONS; i++) {
            RealBitString rbsClone = rbs.clone();
            
            long startTime = System.nanoTime();
            for (int j = 0; j < MEDIUM_SIZE; j++) {
                rbsClone.readUint(32);
            }
            long endTime = System.nanoTime();
            
            realBitStringTimes[i] = endTime - startTime;
        }
        
        // Calculate average times
        double bitStringAvg = calculateAverage(bitStringTimes);
        double realBitStringAvg = calculateAverage(realBitStringTimes);
        
        log.info("BitString read uint average time: {} ns ({} ms)", 
                String.format("%.2f", bitStringAvg), 
                String.format("%.2f", bitStringAvg / 1_000_000));
        log.info("RealBitString read uint average time: {} ns ({} ms)", 
                String.format("%.2f", realBitStringAvg), 
                String.format("%.2f", realBitStringAvg / 1_000_000));
        log.info("Ratio (BitString/RealBitString): {}", 
                String.format("%.2f", bitStringAvg / realBitStringAvg));
    }

    /**
     * Tests the performance of writing strings.
     */
    @Test
    public void testWriteStringPerformance() {
        log.info("=== Write String Performance Test ===");
        
        // Generate random strings to write
        String[] strings = new String[SMALL_SIZE];
        for (int i = 0; i < SMALL_SIZE; i++) {
            strings[i] = generateRandomString(20);
        }
        
        // Warm-up
        for (int i = 0; i < WARMUP_ITERATIONS; i++) {
            BitString bs = new BitString(SMALL_SIZE * 160); // 20 chars * 8 bits
            RealBitString rbs = new RealBitString(SMALL_SIZE * 160);
            
            for (int j = 0; j < SMALL_SIZE / 10; j++) {
                bs.writeString(strings[j]);
                rbs.writeString(strings[j]);
            }
        }
        
        // Test BitString
        long[] bitStringTimes = new long[TEST_ITERATIONS];
        for (int i = 0; i < TEST_ITERATIONS; i++) {
            BitString bs = new BitString(SMALL_SIZE * 160);
            
            long startTime = System.nanoTime();
            for (int j = 0; j < SMALL_SIZE; j++) {
                bs.writeString(strings[j]);
            }
            long endTime = System.nanoTime();
            
            bitStringTimes[i] = endTime - startTime;
        }
        
        // Test RealBitString
        long[] realBitStringTimes = new long[TEST_ITERATIONS];
        for (int i = 0; i < TEST_ITERATIONS; i++) {
            RealBitString rbs = new RealBitString(SMALL_SIZE * 160);
            
            long startTime = System.nanoTime();
            for (int j = 0; j < SMALL_SIZE; j++) {
                rbs.writeString(strings[j]);
            }
            long endTime = System.nanoTime();
            
            realBitStringTimes[i] = endTime - startTime;
        }
        
        // Calculate average times
        double bitStringAvg = calculateAverage(bitStringTimes);
        double realBitStringAvg = calculateAverage(realBitStringTimes);
        
        log.info("BitString write string average time: {} ns ({} ms)", 
                String.format("%.2f", bitStringAvg), 
                String.format("%.2f", bitStringAvg / 1_000_000));
        log.info("RealBitString write string average time: {} ns ({} ms)", 
                String.format("%.2f", realBitStringAvg), 
                String.format("%.2f", realBitStringAvg / 1_000_000));
        log.info("Ratio (BitString/RealBitString): {}", 
                String.format("%.2f", bitStringAvg / realBitStringAvg));
    }

    /**
     * Tests the performance of writing and reading addresses.
     */
    @Test
    public void testAddressPerformance() {
        log.info("=== Address Performance Test ===");
        
        // Create a test address
        Address address = Address.of("0QAs9VlT6S776tq3unJcP5Ogsj-ELLunLXuOb1EKcOQi4-QO");
        
        // Warm-up
        for (int i = 0; i < WARMUP_ITERATIONS; i++) {
            BitString bs = new BitString(34 * 8);
            RealBitString rbs = new RealBitString(34 * 8);
            
            bs.writeAddress(address);
            rbs.writeAddress(address);
            
            BitString bsClone = bs.clone();
            RealBitString rbsClone = rbs.clone();
            
            bsClone.readAddress();
            rbsClone.readAddress();
        }
        
        // Test BitString write
        long[] bitStringWriteTimes = new long[TEST_ITERATIONS];
        for (int i = 0; i < TEST_ITERATIONS; i++) {
            BitString bs = new BitString(34 * 8);
            
            long startTime = System.nanoTime();
            for (int j = 0; j < SMALL_SIZE; j++) {
                bs = new BitString(34 * 8);
                bs.writeAddress(address);
            }
            long endTime = System.nanoTime();
            
            bitStringWriteTimes[i] = endTime - startTime;
        }
        
        // Test RealBitString write
        long[] realBitStringWriteTimes = new long[TEST_ITERATIONS];
        for (int i = 0; i < TEST_ITERATIONS; i++) {
            RealBitString rbs = new RealBitString(34 * 8);
            
            long startTime = System.nanoTime();
            for (int j = 0; j < SMALL_SIZE; j++) {
                rbs = new RealBitString(34 * 8);
                rbs.writeAddress(address);
            }
            long endTime = System.nanoTime();
            
            realBitStringWriteTimes[i] = endTime - startTime;
        }
        
        // Test BitString read
        BitString bs = new BitString(34 * 8);
        bs.writeAddress(address);
        
        long[] bitStringReadTimes = new long[TEST_ITERATIONS];
        for (int i = 0; i < TEST_ITERATIONS; i++) {
            BitString bsClone = bs.clone();
            
            long startTime = System.nanoTime();
            for (int j = 0; j < SMALL_SIZE; j++) {
                bsClone = bs.clone();
                bsClone.readAddress();
            }
            long endTime = System.nanoTime();
            
            bitStringReadTimes[i] = endTime - startTime;
        }
        
        // Test RealBitString read
        RealBitString rbs = new RealBitString(34 * 8);
        rbs.writeAddress(address);
        
        long[] realBitStringReadTimes = new long[TEST_ITERATIONS];
        for (int i = 0; i < TEST_ITERATIONS; i++) {
            RealBitString rbsClone = rbs.clone();
            
            long startTime = System.nanoTime();
            for (int j = 0; j < SMALL_SIZE; j++) {
                rbsClone = rbs.clone();
                rbsClone.readAddress();
            }
            long endTime = System.nanoTime();
            
            realBitStringReadTimes[i] = endTime - startTime;
        }
        
        // Calculate average times
        double bitStringWriteAvg = calculateAverage(bitStringWriteTimes);
        double realBitStringWriteAvg = calculateAverage(realBitStringWriteTimes);
        double bitStringReadAvg = calculateAverage(bitStringReadTimes);
        double realBitStringReadAvg = calculateAverage(realBitStringReadTimes);
        
        log.info("BitString write address average time: {} ns ({} ms)", 
                String.format("%.2f", bitStringWriteAvg), 
                String.format("%.2f", bitStringWriteAvg / 1_000_000));
        log.info("RealBitString write address average time: {} ns ({} ms)", 
                String.format("%.2f", realBitStringWriteAvg), 
                String.format("%.2f", realBitStringWriteAvg / 1_000_000));
        log.info("Write Ratio (BitString/RealBitString): {}", 
                String.format("%.2f", bitStringWriteAvg / realBitStringWriteAvg));
        
        log.info("BitString read address average time: {} ns ({} ms)", 
                String.format("%.2f", bitStringReadAvg), 
                String.format("%.2f", bitStringReadAvg / 1_000_000));
        log.info("RealBitString read address average time: {} ns ({} ms)", 
                String.format("%.2f", realBitStringReadAvg), 
                String.format("%.2f", realBitStringReadAvg / 1_000_000));
        log.info("Read Ratio (BitString/RealBitString): {}", 
                String.format("%.2f", bitStringReadAvg / realBitStringReadAvg));
    }

    /**
     * Tests the performance of complex operations that combine multiple operations.
     */
    @Test
    public void testComplexOperationsPerformance() {
        log.info("=== Complex Operations Performance Test ===");
        
        // Warm-up
        for (int i = 0; i < WARMUP_ITERATIONS; i++) {
            performComplexOperations(new BitString(MEDIUM_SIZE * 8), 100);
            performComplexOperations(new RealBitString(MEDIUM_SIZE * 8), 100);
        }
        
        // Test BitString
        long[] bitStringTimes = new long[TEST_ITERATIONS];
        for (int i = 0; i < TEST_ITERATIONS; i++) {
            BitString bs = new BitString(MEDIUM_SIZE * 8);
            
            long startTime = System.nanoTime();
            performComplexOperations(bs, MEDIUM_SIZE);
            long endTime = System.nanoTime();
            
            bitStringTimes[i] = endTime - startTime;
        }
        
        // Test RealBitString
        long[] realBitStringTimes = new long[TEST_ITERATIONS];
        for (int i = 0; i < TEST_ITERATIONS; i++) {
            RealBitString rbs = new RealBitString(MEDIUM_SIZE * 8);
            
            long startTime = System.nanoTime();
            performComplexOperations(rbs, MEDIUM_SIZE);
            long endTime = System.nanoTime();
            
            realBitStringTimes[i] = endTime - startTime;
        }
        
        // Calculate average times
        double bitStringAvg = calculateAverage(bitStringTimes);
        double realBitStringAvg = calculateAverage(realBitStringTimes);
        
        log.info("BitString complex operations average time: {} ns ({} ms)", 
                String.format("%.2f", bitStringAvg), 
                String.format("%.2f", bitStringAvg / 1_000_000));
        log.info("RealBitString complex operations average time: {} ns ({} ms)", 
                String.format("%.2f", realBitStringAvg), 
                String.format("%.2f", realBitStringAvg / 1_000_000));
        log.info("Ratio (BitString/RealBitString): {}", 
                String.format("%.2f", bitStringAvg / realBitStringAvg));
    }

    /**
     * Helper method to perform complex operations on a BitString.
     */
    private void performComplexOperations(BitString bs, int count) {
        for (int i = 0; i < count; i++) {
            bs.writeUint(i, 32);
            bs.writeString("Test" + i);
            bs.writeUint(BigInteger.valueOf(i * 1000), 64);
        }
        
        BitString clone = bs.clone();
        for (int i = 0; i < count; i++) {
            clone.readUint(32);
            clone.readString(("Test" + i).length() * 8);
            clone.readUint(64);
        }
    }

    /**
     * Helper method to perform complex operations on a RealBitString.
     */
    private void performComplexOperations(RealBitString rbs, int count) {
        for (int i = 0; i < count; i++) {
            rbs.writeUint(i, 32);
            rbs.writeString("Test" + i);
            rbs.writeUint(BigInteger.valueOf(i * 1000), 64);
        }
        
        RealBitString clone = rbs.clone();
        for (int i = 0; i < count; i++) {
            clone.readUint(32);
            clone.readString(("Test" + i).length() * 8);
            clone.readUint(64);
        }
    }

    /**
     * Helper method to generate a random string of specified length.
     */
    private String generateRandomString(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            char c = (char) ('a' + RANDOM.nextInt(26));
            sb.append(c);
        }
        return sb.toString();
    }

    /**
     * Helper method to calculate the average of an array of longs.
     */
    private double calculateAverage(long[] values) {
        long sum = 0;
        for (long value : values) {
            sum += value;
        }
        return (double) sum / values.length;
    }
}
