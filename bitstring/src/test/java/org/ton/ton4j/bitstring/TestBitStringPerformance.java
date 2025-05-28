package org.ton.ton4j.bitstring;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.math.BigInteger;
import lombok.extern.slf4j.Slf4j;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@Slf4j
@RunWith(JUnit4.class)
@Ignore
public class TestBitStringPerformance {

  private static final int SIZE = 1_000_000;

  @Test
  public void testWriteBitStringPerformance() {
    BitString bitString = new BitString(SIZE);
    long startTime = System.nanoTime();

    for (int i = 0; i < SIZE; i++) {
      bitString.writeBit(i % 2 == 0);
    }

    long endTime = System.nanoTime();
    long duration = endTime - startTime;
    System.out.println("Write BitString performance: " + duration / 1_000_000 + " ms");

    assertTrue("Test should complete in less than 300 ms", duration < 300_000_000);
  }

  @Test
  public void testReadBitStringPerformance() {
    BitString bitString = new BitString(SIZE);
    for (int i = 0; i < SIZE; i++) {
      bitString.writeBit(i % 2 == 0);
    }

    long startTime = System.nanoTime();

    for (int i = 0; i < SIZE; i++) {
      bitString.readBit();
    }

    long endTime = System.nanoTime();
    long duration = endTime - startTime;
    System.out.println("Read BitString performance: " + duration / 1_000_000 + " ms");

    assertTrue("Test should complete in less than 300 ms", duration < 300_000_000);
  }

  @Test
  public void testWriteUintPerformance() {
    BitString bitString = new BitString(SIZE);
    long startTime = System.nanoTime();

    for (int i = 0; i < SIZE; i++) {
      bitString.writeUint(i, 32);
    }

    long endTime = System.nanoTime();
    long duration = endTime - startTime;
    System.out.println("Write Uint performance: " + duration / 1_000_000 + " ms");

    assertTrue("Test should complete in less than 400 ms", duration < 400_000_000);
  }

  @Test
  public void testReadUintPerformance() {
    BitString bitString = new BitString(SIZE);
    for (int i = 0; i < SIZE; i++) {
      bitString.writeUint(i, 64);
    }

    long startTime = System.nanoTime();

    for (int i = 0; i < SIZE; i++) {
      bitString.readUint(64);
    }

    long endTime = System.nanoTime();
    long duration = endTime - startTime;
    System.out.println("Read Uint performance: " + duration / 1_000_000 + " ms");

    assertTrue("Test should complete in less than 300 ms", duration < 300_000_000);
  }

  @Test
  public void testWriteBytesPerformance() {
    byte[] data = new byte[SIZE];
    new java.util.Random().nextBytes(data);
    BitString bitString = new BitString(SIZE);
    long startTime = System.nanoTime();

    bitString.writeBytes(data);

    long endTime = System.nanoTime();
    long duration = endTime - startTime;
    System.out.println("Write Bytes performance: " + duration / 1_000_000 + " ms");

    assertTrue("Test should complete in less than 300 ms", duration < 300_000_000);
  }

  @Test
  public void testReadBytesPerformance() {
    byte[] data = new byte[SIZE];
    new java.util.Random().nextBytes(data);
    BitString bitString = new BitString(SIZE);
    bitString.writeBytes(data);

    long startTime = System.nanoTime();

    bitString.readBytes(SIZE * 8);

    long endTime = System.nanoTime();
    long duration = endTime - startTime;
    System.out.println("Read Bytes performance: " + duration / 1_000_000 + " ms");

    assertTrue("Test should complete in less than 300 ms", duration < 300_000_000);
  }

  @Test
  public void testWriteUint() {
    BitString bitString = new BitString(SIZE);

    BigInteger number = new BigInteger("12345678901234567890");
    int bitLength = SIZE;

    long startTime = System.nanoTime();
    bitString.writeUint(number, bitLength);

    BigInteger result = bitString.readUint(bitLength);

    long endTime = System.nanoTime();
    long duration = endTime - startTime;
    System.out.println("Read Bytes performance: " + duration / 1_000_000 + " ms");
    assertTrue("Test should complete in less than 300 ms", duration < 300_000_000);

    assertEquals(number, result);
  }
}
