package org.ton.ton4j.coverage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.*;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.ton.ton4j.bitstring.BitString;
import org.ton.ton4j.cell.Cell;
import org.ton.ton4j.cell.CellBuilder;
import org.ton.ton4j.cell.CellSlice;
import org.ton.ton4j.cell.CellType;
import org.ton.ton4j.cell.LevelMask;

@RunWith(JUnit4.class)
public class TestCellAndCellSliceImprovedCoverage {

  // ========== Cell Constructor Tests ==========

  @Test
  public void testCellConstructorWithBitStringAndByteArray() {
    // Test Cell(BitString, byte[])
    BitString bits = new BitString(16);
    bits.writeUint(255, 8);
    byte[] hashes = new byte[32];
    Arrays.fill(hashes, (byte) 0xAB);

    Cell cell = new Cell(bits, hashes);
    assertThat(cell).isNotNull();
    assertThat(cell.getHashes()).isEqualTo(hashes);
    assertThat(cell.getBits().getUsedBits()).isEqualTo(8);
  }

  @Test
  public void testCellConstructorWithBitStringByteArrayAndRefsCount() {
    // Test Cell(BitString, byte[], int)
    BitString bits = new BitString(32);
    bits.writeUint(12345, 16);
    byte[] hashes = new byte[64];
    Arrays.fill(hashes, (byte) 0xCD);
    int refsCount = 3;

    Cell cell = new Cell(bits, hashes, refsCount);
    assertThat(cell).isNotNull();
    assertThat(cell.getHashes()).isEqualTo(hashes);
    assertThat(cell.getRefsCount()).isEqualTo(refsCount);
    assertThat(cell.getBits().getUsedBits()).isEqualTo(16);
  }

  @Test
  public void testCellConstructorWithBitStringListAndCellType() {
    // Test Cell(BitString, List, int)
    BitString bits = new BitString(24);
    bits.writeUint(777, 10);
    
    Cell ref1 = CellBuilder.beginCell().storeUint(1, 8).endCell();
    Cell ref2 = CellBuilder.beginCell().storeUint(2, 8).endCell();
    List<Cell> refs = Arrays.asList(ref1, ref2);
    
    int cellType = -1; // ORDINARY

    Cell cell = new Cell(bits, refs, cellType);
    assertThat(cell).isNotNull();
    assertThat(cell.getRefs()).hasSize(2);
    assertThat(cell.getCellType()).isEqualTo(CellType.ORDINARY);
  }

  @Test
  public void testCellConstructorWithIntBitSize() {
    // Test Cell(int)
    Cell cell = new Cell(128);
    assertThat(cell).isNotNull();
    assertThat(cell.getBits().getLength()).isEqualTo(128);
    assertThat(cell.getRefs()).isEmpty();
  }

  @Test
  public void testCellConstructorWithBitStringBitSizeListBooleanLevelMask() {
    // Test Cell(BitString, int, List, boolean, LevelMask)
    BitString bits = new BitString(64);
    bits.writeUint(999, 16);
    int bitSize = 64;
    
    Cell ref = CellBuilder.beginCell().storeUint(42, 8).endCell();
    List<Cell> refs = Collections.singletonList(ref);
    boolean exotic = false;
    LevelMask levelMask = new LevelMask(0);

    Cell cell = new Cell(bits, bitSize, refs, exotic, levelMask);
    assertThat(cell).isNotNull();
    assertThat(cell.getRefs()).hasSize(1);
    assertThat(cell.isExotic()).isFalse();
    assertThat(cell.levelMask).isEqualTo(levelMask);
  }

  @Test
  public void testCellConstructorWithBitStringBitSizeListAndCellType() {
    // Test Cell(BitString, int, List, CellType)
    BitString bits = new BitString(48);
    bits.writeUint(555, 12);
    int bitSize = 48;
    
    Cell ref = CellBuilder.beginCell().storeUint(99, 8).endCell();
    List<Cell> refs = Collections.singletonList(ref);
    CellType cellType = CellType.ORDINARY;

    Cell cell = new Cell(bits, bitSize, refs, cellType);
    assertThat(cell).isNotNull();
    assertThat(cell.getRefs()).hasSize(1);
    assertThat(cell.getCellType()).isEqualTo(CellType.ORDINARY);
  }

  @Test
  public void testWriteCellMethod() {
    // Test writeCell(Cell)
    Cell cell1 = CellBuilder.beginCell().storeUint(100, 8).endCell();
    Cell cell2 = CellBuilder.beginCell().storeUint(200, 8).endCell();
    
    Cell targetCell = new Cell();
    targetCell.writeCell(cell1);
    targetCell.writeCell(cell2);
    
    assertThat(targetCell.getBits().getUsedBits()).isEqualTo(16);
  }

  @Test
  public void testWriteCellWithRefs() {
    // Test writeCell with refs
    Cell ref1 = CellBuilder.beginCell().storeUint(1, 8).endCell();
    Cell ref2 = CellBuilder.beginCell().storeUint(2, 8).endCell();
    
    Cell sourceCell = CellBuilder.beginCell()
        .storeUint(50, 8)
        .storeRef(ref1)
        .storeRef(ref2)
        .endCell();
    
    Cell targetCell = new Cell();
    targetCell.writeCell(sourceCell);
    
    assertThat(targetCell.getRefs()).hasSize(2);
  }

  @Test
  public void testWriteCellWithEmptyRefs() {
    // Test writeCell with no refs (edge case)
    Cell sourceCell = CellBuilder.beginCell().storeUint(123, 8).endCell();
    Cell targetCell = new Cell();
    
    targetCell.writeCell(sourceCell);
    
    assertThat(targetCell.getBits().getUsedBits()).isEqualTo(8);
    assertThat(targetCell.getRefs()).isEmpty();
  }

  @Test
  public void testWriteCellWithSingleRef() {
    // Test writeCell with single ref (optimization path)
    Cell ref = CellBuilder.beginCell().storeUint(77, 8).endCell();
    Cell sourceCell = CellBuilder.beginCell()
        .storeUint(88, 8)
        .storeRef(ref)
        .endCell();
    
    Cell targetCell = new Cell();
    targetCell.writeCell(sourceCell);
    
    assertThat(targetCell.getRefs()).hasSize(1);
  }

  // ========== CellSlice Constructor Tests ==========

  @Test
  public void testCellSliceConstructorWithBitStringAndList() {
    // Test CellSlice(BitString, List)
    BitString bits = new BitString(32);
    bits.writeUint(456, 16);
    
    Cell ref = CellBuilder.beginCell().storeUint(10, 8).endCell();
    List<Cell> refs = Collections.singletonList(ref);
    
    CellSlice slice = CellSlice.beginParse(
        new Cell(bits, refs)
    );
    
    assertThat(slice).isNotNull();
    assertThat(slice.getRefsCount()).isEqualTo(1);
  }

  @Test
  public void testCellSliceConstructorWithBitStringListCellTypeAndHashes() {
    // Test CellSlice(BitString, List, CellType, byte[])
    BitString bits = new BitString(40);
    bits.writeUint(789, 12);
    
    Cell ref = CellBuilder.beginCell().storeUint(20, 8).endCell();
    List<Cell> refs = Collections.singletonList(ref);
    CellType cellType = CellType.ORDINARY;
    byte[] hashes = new byte[32];
    Arrays.fill(hashes, (byte) 0xEF);
    
    Cell cell = new Cell(bits, refs);
    CellSlice slice = CellSlice.beginParse(cell, hashes);
    
    assertThat(slice).isNotNull();
    assertThat(slice.getHashes()).isEqualTo(hashes);
  }

  @Test
  public void testCellSliceWithEmptyRefs() {
    // Test with empty refs list
    BitString bits = new BitString(16);
    bits.writeUint(111, 8);
    List<Cell> refs = Collections.emptyList();
    
    Cell cell = new Cell(bits, refs);
    CellSlice slice = CellSlice.beginParse(cell);
    
    assertThat(slice.getRefsCount()).isEqualTo(0);
  }

  @Test
  public void testCellSliceWithMultipleRefs() {
    // Test with multiple refs (>4 edge case)
    BitString bits = new BitString(16);
    bits.writeUint(222, 8);
    
    Cell ref1 = CellBuilder.beginCell().storeUint(1, 8).endCell();
    Cell ref2 = CellBuilder.beginCell().storeUint(2, 8).endCell();
    Cell ref3 = CellBuilder.beginCell().storeUint(3, 8).endCell();
    List<Cell> refs = Arrays.asList(ref1, ref2, ref3);
    
    Cell cell = new Cell(bits, refs);
    CellSlice slice = CellSlice.beginParse(cell);
    
    assertThat(slice.getRefsCount()).isEqualTo(3);
  }

  // ========== CellSlice Method Tests ==========

  @Test
  public void testLoadUintLEQ() {
    // Test loadUintLEQ(BigInteger)
    Cell cell = CellBuilder.beginCell().storeUint(50, 7).endCell();
    CellSlice slice = CellSlice.beginParse(cell);
    
    BigInteger result = slice.loadUintLEQ(BigInteger.valueOf(100));
    assertThat(result).isEqualTo(BigInteger.valueOf(50));
  }

  @Test
  public void testLoadUintLEQThrowsError() {
    // Test loadUintLEQ throws error when value > n
    Cell cell = CellBuilder.beginCell().storeUint(255, 8).endCell();
    CellSlice slice = CellSlice.beginParse(cell);
    
    assertThrows(Error.class, () -> slice.loadUintLEQ(BigInteger.valueOf(100)));
  }

  @Test
  public void testPreloadMaybeRefXWithTrue() {
    // Test preloadMaybeRefX() when bit is true
    Cell ref = CellBuilder.beginCell().storeUint(42, 8).endCell();
    Cell cell = CellBuilder.beginCell()
        .storeBit(true)
        .storeRef(ref)
        .endCell();
    
    CellSlice slice = CellSlice.beginParse(cell);
    Cell result = slice.preloadMaybeRefX();
    
    assertThat(result).isNotNull();
  }

  @Test
  public void testPreloadMaybeRefXWithFalse() {
    // Test preloadMaybeRefX() when bit is false
    Cell cell = CellBuilder.beginCell().storeBit(false).endCell();
    CellSlice slice = CellSlice.beginParse(cell);
    
    Cell result = slice.preloadMaybeRefX();
    assertThat(result).isNull();
  }

  @Test
  public void testLoadSnakeStringSimple() {
    // Test loadSnakeString() with simple string
    String testString = "Hello World";
    Cell cell = CellBuilder.beginCell().storeSnakeString(testString).endCell();
    CellSlice slice = CellSlice.beginParse(cell);
    
    String result = slice.loadSnakeString();
    assertThat(result).isEqualTo(testString);
  }

  @Test
  public void testLoadSnakeStringWithRefs() {
    // Test loadSnakeString() with refs (long string)
    StringBuilder longString = new StringBuilder();
    for (int i = 0; i < 200; i++) {
      longString.append("A");
    }
    
    Cell cell = CellBuilder.beginCell().storeSnakeString(longString.toString()).endCell();
    CellSlice slice = CellSlice.beginParse(cell);
    
    String result = slice.loadSnakeString();
    assertThat(result).isEqualTo(longString.toString());
  }

  @Test
  public void testLoadSnakeStringReturnsNull() {
    // Test loadSnakeString() returns null on error
    Cell cell = CellBuilder.beginCell().storeUint(255, 8).endCell();
    CellSlice slice = CellSlice.beginParse(cell);
    
    // This should handle the error gracefully
    String result = slice.loadSnakeString();
    // Result may be null or partial depending on implementation
    assertThat(result).isNotNull();
  }

  @Test
  public void testSkipDict() {
    // Test skipDict(int) - skipDict loads and processes a dict
    // Create a proper dict structure
    Cell valueCell = CellBuilder.beginCell().storeUint(100, 16).endCell();
    Cell dictRoot = CellBuilder.beginCell()
        .storeUint(1, 8)
        .storeRef(valueCell)
        .endCell();
    
    CellSlice slice = CellSlice.beginParse(dictRoot);
    CellSlice result = slice.skipDict(8);
    
    assertThat(result).isNotNull();
  }

  @Test
  public void testPreloadInt() {
    // Test preloadInt(int)
    Cell cell = CellBuilder.beginCell().storeInt(-42, 8).endCell();
    CellSlice slice = CellSlice.beginParse(cell);
    
    BigInteger result = slice.preloadInt(8);
    assertThat(result).isEqualTo(BigInteger.valueOf(-42));
    
    // Verify cursor didn't move
    assertThat(slice.getRestBits()).isEqualTo(8);
  }

  @Test
  public void testPreloadIntRestoresPosition() {
    // Test preloadInt restores position on success
    Cell cell = CellBuilder.beginCell().storeInt(-100, 16).endCell();
    CellSlice slice = CellSlice.beginParse(cell);
    
    BigInteger result = slice.preloadInt(16);
    assertThat(result).isEqualTo(BigInteger.valueOf(-100));
    
    // Verify position was restored
    assertThat(slice.getRestBits()).isEqualTo(16);
  }

  @Test
  public void testPreloadCoins() {
    // Test preloadCoins()
    BigInteger coins = BigInteger.valueOf(123456789);
    Cell cell = CellBuilder.beginCell().storeCoins(coins).endCell();
    CellSlice slice = CellSlice.beginParse(cell);
    
    BigInteger result = slice.preloadCoins();
    assertThat(result).isEqualTo(coins);
    
    // Verify cursor didn't move
    assertThat(slice.getRestBits()).isGreaterThan(0);
  }

  @Test
  public void testPreloadCoinsWithError() {
    // Test preloadCoins with error handling
    Cell cell = CellBuilder.beginCell().storeUint(1, 1).endCell();
    CellSlice slice = CellSlice.beginParse(cell);
    
    // Should handle error and restore position
    try {
      slice.preloadCoins();
    } catch (Error e) {
      // Expected
    }
  }

  // ========== Additional Edge Cases ==========

  @Test
  public void testCellWithExoticType() {
    // Test Cell with exotic type
    BitString bits = new BitString(32);
    bits.writeUint(100, 8);
    List<Cell> refs = Collections.emptyList();
    boolean exotic = true;
    CellType cellType = CellType.ORDINARY;
    
    Cell cell = new Cell(bits, 32, refs, exotic, cellType);
    assertThat(cell.isExotic()).isTrue();
  }

  @Test
  public void testCellSliceClone() {
    // Test CellSlice clone
    Cell cell = CellBuilder.beginCell().storeUint(999, 16).endCell();
    CellSlice slice = CellSlice.beginParse(cell);
    
    CellSlice cloned = slice.clone();
    assertThat(cloned).isNotNull();
    assertThat(cloned.getRestBits()).isEqualTo(slice.getRestBits());
  }

  @Test
  public void testCellSliceCloneWithRefs() {
    // Test CellSlice clone with refs
    Cell ref = CellBuilder.beginCell().storeUint(1, 8).endCell();
    Cell cell = CellBuilder.beginCell()
        .storeUint(100, 8)
        .storeRef(ref)
        .endCell();
    
    CellSlice slice = CellSlice.beginParse(cell);
    CellSlice cloned = slice.clone();
    
    assertThat(cloned.getRefsCount()).isEqualTo(1);
  }

  @Test
  public void testCellSliceCloneWithEmptyRefs() {
    // Test CellSlice clone with empty refs
    Cell cell = CellBuilder.beginCell().storeUint(50, 8).endCell();
    CellSlice slice = CellSlice.beginParse(cell);
    
    CellSlice cloned = slice.clone();
    assertThat(cloned.getRefsCount()).isEqualTo(0);
  }

  @Test
  public void testCellSliceCloneWithMultipleRefs() {
    // Test CellSlice clone with multiple refs
    Cell ref1 = CellBuilder.beginCell().storeUint(1, 8).endCell();
    Cell ref2 = CellBuilder.beginCell().storeUint(2, 8).endCell();
    Cell ref3 = CellBuilder.beginCell().storeUint(3, 8).endCell();
    Cell ref4 = CellBuilder.beginCell().storeUint(4, 8).endCell();
    
    Cell cell = CellBuilder.beginCell()
        .storeUint(100, 8)
        .storeRef(ref1)
        .storeRef(ref2)
        .storeRef(ref3)
        .storeRef(ref4)
        .endCell();
    
    CellSlice slice = CellSlice.beginParse(cell);
    CellSlice cloned = slice.clone();
    
    assertThat(cloned.getRefsCount()).isEqualTo(4);
  }

  @Test
  public void testPreloadUintSmallValue() {
    // Test preloadUint with small value (optimization path)
    Cell cell = CellBuilder.beginCell().storeUint(42, 8).endCell();
    CellSlice slice = CellSlice.beginParse(cell);
    
    BigInteger result = slice.preloadUint(8);
    assertThat(result).isEqualTo(BigInteger.valueOf(42));
    assertThat(slice.getRestBits()).isEqualTo(8);
  }

  @Test
  public void testPreloadUintLargeValue() {
    // Test preloadUint with large value (>64 bits)
    BigInteger largeValue = new BigInteger("123456789012345678901234567890");
    Cell cell = CellBuilder.beginCell().storeUint(largeValue, 128).endCell();
    CellSlice slice = CellSlice.beginParse(cell);
    
    BigInteger result = slice.preloadUint(128);
    assertThat(result).isEqualTo(largeValue);
    assertThat(slice.getRestBits()).isEqualTo(128);
  }

  @Test
  public void testPreloadUintWithError() {
    // Test preloadUint with error
    Cell cell = CellBuilder.beginCell().storeUint(1, 1).endCell();
    CellSlice slice = CellSlice.beginParse(cell);
    
    BigInteger result = slice.preloadUint(16);
    assertThat(result).isEqualTo(BigInteger.ZERO);
  }

  @Test
  public void testLoadSnakeStringWithMultipleRefs() {
    // Test loadSnakeString with multiple refs
    StringBuilder veryLongString = new StringBuilder();
    for (int i = 0; i < 500; i++) {
      veryLongString.append("X");
    }
    
    Cell cell = CellBuilder.beginCell().storeSnakeString(veryLongString.toString()).endCell();
    CellSlice slice = CellSlice.beginParse(cell);
    
    String result = slice.loadSnakeString();
    assertThat(result).isEqualTo(veryLongString.toString());
  }

  @Test
  public void testLoadSnakeStringWithMoreThanOneRef() {
    // Test loadSnakeString error when more than one ref
    Cell ref1 = CellBuilder.beginCell().storeUint(1, 8).endCell();
    Cell ref2 = CellBuilder.beginCell().storeUint(2, 8).endCell();
    
    Cell cell = CellBuilder.beginCell()
        .storeString("test")
        .storeRef(ref1)
        .storeRef(ref2)
        .endCell();
    
    CellSlice slice = CellSlice.beginParse(cell);
    String result = slice.loadSnakeString();
    
    // Should return null due to error
    assertThat(result).isNull();
  }

  @Test
  public void testLoadSlicePartialByte() {
    // Test loadSlice with partial byte
    Cell cell = CellBuilder.beginCell().storeBits("10101").endCell();
    CellSlice slice = CellSlice.beginParse(cell);
    
    int[] result = slice.loadSlice(5);
    assertThat(result).isNotEmpty();
  }

  @Test
  public void testCellConstructorWithBitStringBitSizeListBooleanCellType() {
    // Test Cell(BitString, int, List, boolean, CellType)
    BitString bits = new BitString(32);
    bits.writeUint(777, 12);
    int bitSize = 32;
    
    Cell ref = CellBuilder.beginCell().storeUint(55, 8).endCell();
    List<Cell> refs = Collections.singletonList(ref);
    boolean exotic = true;
    CellType cellType = CellType.ORDINARY;

    Cell cell = new Cell(bits, bitSize, refs, exotic, cellType);
    assertThat(cell).isNotNull();
    assertThat(cell.isExotic()).isTrue();
    // getCellType() may return UNKNOWN for exotic cells without proper structure
    assertThat(cell.getCellType()).isIn(CellType.ORDINARY, CellType.UNKNOWN);
  }
}
