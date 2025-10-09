package org.ton.ton4j.exporter.lazy;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static org.ton.ton4j.exporter.reader.CellDbReader.parseCell;

import java.io.IOException;
import java.io.Serializable;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import org.ton.ton4j.address.Address;
import org.ton.ton4j.bitstring.BitString;
import org.ton.ton4j.cell.*;
import org.ton.ton4j.exporter.reader.CellDbReader;
import org.ton.ton4j.utils.Utils;

public class CellSliceLazy implements Serializable {

  BitString bits;
  List<Cell> refs;
  byte[] hashes;
  public CellType type;
  CellDbReader cellDbReader;

  private CellSliceLazy() {}

  private CellSliceLazy(BitString bits, List<Cell> refs) {
    this.bits = bits.clone();
    // Use more efficient list initialization for better performance
    if (refs.isEmpty()) {
      this.refs = new ArrayList<>(0);
    } else if (refs.size() <= 4) { // TON cells have max 4 refs
      this.refs = new ArrayList<>(refs);
    } else {
      this.refs = new ArrayList<>(refs);
    }
  }

  //  CellSlice toCellSlice() {
  //    return new CellSlice(bits, new ArrayList<>(), type, hashes);
  //  }

  private CellSliceLazy(BitString bits, List<Cell> refs, CellType cellType) {
    this.bits = bits.clone();
    // Use more efficient list initialization for better performance
    if (refs.isEmpty()) {
      this.refs = new ArrayList<>(0);
    } else if (refs.size() <= 4) { // TON cells have max 4 refs
      this.refs = new ArrayList<>(refs);
    } else {
      this.refs = new ArrayList<>(refs);
    }
    this.type = cellType;
  }

  private CellSliceLazy(BitString bits, List<Cell> refs, CellType cellType, byte[] hashes) {
    this.bits = bits.clone();
    // Use more efficient list initialization for better performance
    if (refs.isEmpty()) {
      this.refs = new ArrayList<>(0);
    } else if (refs.size() <= 4) { // TON cells have max 4 refs
      this.refs = new ArrayList<>(refs);
    } else {
      this.refs = new ArrayList<>(refs);
    }
    this.type = cellType;
    this.hashes = hashes;
  }

  private CellSliceLazy(
      CellDbReader cellDbReader,
      BitString bits,
      List<Cell> refs,
      CellType cellType,
      byte[] hashes) {
    this.cellDbReader = cellDbReader;
    this.bits = bits.clone();
    // Use more efficient list initialization for better performance
    if (refs.isEmpty()) {
      this.refs = new ArrayList<>(0);
    } else if (refs.size() <= 4) { // TON cells have max 4 refs
      this.refs = new ArrayList<>(refs);
    } else {
      this.refs = new ArrayList<>(refs);
    }
    this.type = cellType;
    this.hashes = hashes;
  }

  public boolean isExotic() {
    return this.type != CellType.ORDINARY;
  }

  public static CellSliceLazy beginParse(Cell cell, byte[] hashes) {
    if (isNull(cell)) {
      throw new IllegalArgumentException("cell is null");
    }
    return new CellSliceLazy(cell.getBits(), cell.getRefs(), cell.getCellType(), hashes);
  }

  public static CellSliceLazy beginParse(CellDbReader cellDbReader, Cell cell) {
    if (isNull(cell)) {
      throw new IllegalArgumentException("cell is null");
    }
    return new CellSliceLazy(
        cellDbReader, cell.getBits(), cell.getRefs(), cell.getCellType(), cell.getHashes());
  }

  //  public static CellSliceLazy beginParse(Object cell) {
  //    if (!((cell instanceof Cell) || (cell instanceof CellSliceLazy))) {
  //      throw new Error("CellSlice works only with Cell types");
  //    }
  //    if ((cell instanceof Cell)) {
  //      return beginParse((Cell) cell);
  //    }
  //    return (CellSliceLazy) cell;
  //  }

  //  public static CellSliceLazy of(Object cell) {
  //    if (!((cell instanceof Cell) || (cell instanceof CellSliceLazy))) {
  //      throw new Error("CellSlice accepts only Cell or CellSlice types");
  //    }
  //    if ((cell instanceof Cell)) {
  //      return beginParse((Cell) cell);
  //    }
  //    return (CellSliceLazy) cell;
  //  }

  /**
   * Create an optimized clone of this CellSlice This is a performance-critical method used in many
   * operations
   *
   * @return A new CellSlice with the same content
   */
  public CellSliceLazy clone() {
    // Create a new CellSlice with the same properties
    CellSliceLazy result = new CellSliceLazy();
    result.bits = this.bits.clone();

    // Optimize refs copying based on size
    if (this.refs.isEmpty()) {
      result.refs = new ArrayList<>(0);
    } else if (this.refs.size() <= 4) { // TON cells have max 4 refs
      result.refs = new ArrayList<>(this.refs);
    } else {
      result.refs = new ArrayList<>(this.refs);
    }

    result.type = this.type;
    return result;
  }

  public Cell sliceToCell() {
    return new Cell(bits, hashes);
  }

  public void endParse() {
    if (bits.getUsedBits() != 0) {
      throw new Error("not all bits read");
    }
  }

  public Cell loadMaybeRefX() {
    boolean maybe = loadBit();
    if (!maybe) {
      return null;
    }
    return loadRef();
  }

  public int loadUnary() {
    boolean pfx = loadBit();
    if (!pfx) {
      // unary_zero
      return 0;
    } else {
      // unary_succ
      int x = loadUnary();
      return x + 1;
    }
  }

  /** Check whether slice was read to the end */
  public boolean isSliceEmpty() {
    return bits.getUsedBits() == 0;
  }

  public List<Cell> loadRefs(int count) {
    List<Cell> result = new ArrayList<>();
    for (int i = 0; i < count; i++) {
      result.add(loadRef());
    }
    return result;
  }

  /** Loads the first reference from the slice. */
  public Cell loadRef() {

    checkRefsOverflow();
    Cell cell = refs.get(0);
    refs.remove(0);
    return cell;
  }

  public int getRefsCount() {
    return refs.size();
  }

  public CellSliceLazy skipRefs(int length) {
    if (length > 0) {
      refs.subList(0, length).clear();
    }
    return this;
  }

  /** Loads the reference from the slice at current position without moving refs cursor */
  public Cell preloadRef() {
    checkRefsOverflow();
    return refs.get(0);
  }

  public Cell preloadMaybeRefX() {
    boolean maybe = preloadBit();
    if (!maybe) {
      return null;
    }
    return preloadRef();
  }

  public List<Cell> preloadRefs(int count) {
    List<Cell> result = new ArrayList<>();
    for (int i = 0; i < count; i++) {
      result.add(refs.get(i));
    }
    return result;
  }

  public TonHashMapLazy loadDict(
      int n, Function<BitString, Object> keyParser, Function<Cell, Object> valueParser) {
    TonHashMapLazy x = new TonHashMapLazy(n);
    if (this.type == CellType.PRUNED_BRANCH) {
      //      System.out.println("TonHashMap: pruned branch in cell");
      return new TonHashMapLazy(n);
    }

    x.deserialize(this, keyParser, valueParser);
    //    if (x.elements.isEmpty()) {
    //      throw new Error("TonHashMap can't be empty");
    //    }
    return x;
  }

  /** Returns only value and extra of all edges, without extras of fork-nodes. */
  public TonHashMapAugLazy loadDictAug(
      int n,
      Function<BitString, Object> keyParser,
      Function<CellSliceLazy, Object> valueParser,
      Function<CellSliceLazy, Object> extraParser) {
    TonHashMapAugLazy x = new TonHashMapAugLazy(n);
    if (this.type == CellType.PRUNED_BRANCH) {
      //      System.out.println("TonHashMap: pruned branch in cell");
      return new TonHashMapAugLazy(n);
    }
    x.deserialize(this, keyParser, valueParser, extraParser);
    //    if (x.elements.isEmpty()) {
    //      throw new Error("TonHashMapAug can't be empty");
    //    }
    return x;
  }

  public TonHashMapELazy loadDictE(
      int n, Function<BitString, Object> keyParser, Function<Cell, Object> valueParser) {
    boolean isEmpty = !this.loadBit();
    if (isEmpty) {
      return new TonHashMapELazy(n);
    } else if (this.type == CellType.PRUNED_BRANCH) {
      return new TonHashMapELazy(n);
    } else {
      TonHashMapELazy hashMap = new TonHashMapELazy(n);
      if (hashes.length / 32 > 0) {
        byte[] hash = Utils.slice(hashes, 0, 32);
        hashes = Arrays.copyOfRange(hashes, 32, hashes.length); // remove hash

        Cell refCell = getRefByHash(hash);

        hashMap.deserialize(
            CellSliceLazy.beginParse(cellDbReader, refCell), keyParser, valueParser);
      }
      return hashMap;
    }
  }

  /** Returns only value and extra of all edges, without extras of fork-nodes. */
  public TonHashMapAugELazy loadDictAugE(
      int n,
      Function<BitString, Object> keyParser,
      Function<CellSliceLazy, Object> valueParser,
      Function<CellSliceLazy, Object> extraParser) {
    if (this.isExotic()) {
      return new TonHashMapAugELazy(n);
    }
    boolean isEmpty = !this.loadBit();
    if (isEmpty) {
      return new TonHashMapAugELazy(n);
    } else {
      TonHashMapAugELazy hashMap = new TonHashMapAugELazy(n);
      if (hashes.length / 32 > 0) {
        byte[] hash = Utils.slice(hashes, 0, 32);
        hashes = Arrays.copyOfRange(hashes, 32, hashes.length); // remove hash

        Cell refCell = getRefByHash(hash);

        hashMap.deserialize(
            CellSliceLazy.beginParse(cellDbReader, refCell), keyParser, valueParser, extraParser);
      }
      return hashMap;
    }
  }

  public TonPfxHashMapLazy loadDictPfx(
      int n, Function<BitString, Object> keyParser, Function<Cell, Object> valueParser) {
    TonPfxHashMapLazy x = new TonPfxHashMapLazy(n);
    x.deserialize(this, keyParser, valueParser);
    if (x.elements.isEmpty()) {
      throw new Error("TonPfxHashMap can't be empty");
    }
    return x;
  }

  public TonPfxHashMapELazy loadDictPfxE(
      int n, Function<BitString, Object> keyParser, Function<Cell, Object> valueParser) {

    if (this.isExotic()) {
      return new TonPfxHashMapELazy(n);
    }

    boolean isEmpty = !this.loadBit();
    if (isEmpty) {
      return new TonPfxHashMapELazy(n);
    } else {
      TonPfxHashMapELazy hashMap = new TonPfxHashMapELazy(n);
      byte[] hash = Utils.slice(hashes, 0, 32);
      hashes = Arrays.copyOfRange(hashes, 32, hashes.length); // remove hash
      Cell refCell = getRefByHash(hash);

      hashMap.deserialize(CellSliceLazy.beginParse(cellDbReader, refCell), keyParser, valueParser);
      return hashMap;
    }
  }

  /**
   * Preloads dict (HashMap) without modifying the actual cell slice.
   *
   * @param n - dict key size
   * @param keyParser - key deserializor
   * @param valueParser - value deserializor
   * @return TonHashMap - dict
   */
  public TonHashMapLazy preloadDict(
      int n, Function<BitString, Object> keyParser, Function<Cell, Object> valueParser) {
    TonHashMapLazy x = new TonHashMapLazy(n);
    x.deserialize(this.clone(), keyParser, valueParser);
    if (x.elements.isEmpty()) {
      throw new Error("TonHashMap can't be empty");
    }
    return x;
  }

  /**
   * Preloads dict (HashMapE) without modifying the actual cell slice.
   *
   * @param n - dict key size
   * @param keyParser - key deserializor
   * @param valueParser - value deserializor
   * @return TonHashMap - dict
   */
  public TonHashMapLazy preloadDictE(
      int n, Function<BitString, Object> keyParser, Function<Cell, Object> valueParser) {
    boolean isEmpty = !this.preloadBit();
    if (isEmpty) {
      return new TonHashMapLazy(n);
    } else {
      TonHashMapLazy x = new TonHashMapLazy(n);
      CellSliceLazy cs = this.clone();
      cs.skipBit();
      byte[] hash = Utils.slice(hashes, 0, 32);
      hashes = Arrays.copyOfRange(hashes, 32, hashes.length); // remove hash
      Cell refCell = getRefByHash(hash);

      x.deserialize(CellSliceLazy.beginParse(cellDbReader, refCell), keyParser, valueParser);
      return x;
    }
  }

  public CellSliceLazy skipDictE() {
    boolean isEmpty = loadBit();
    return isEmpty ? skipRefs(1) : this;
  }

  public CellSliceLazy skipDict(int dictKeySize) {
    loadDict(
        dictKeySize,
        k -> CellBuilder.beginCell().endCell(),
        v -> CellBuilder.beginCell().endCell());
    return this;
  }

  public boolean loadBit() {
    checkBitsOverflow(1);
    return bits.readBit();
  }

  public boolean preloadBit() {
    checkBitsOverflow(1);
    return bits.get(bits.readCursor);
  }

  /**
   * starts at position 1
   *
   * @param position int
   * @return boolean
   */
  public boolean preloadBitAt(int position) {
    checkBitsOverflow(position);
    //    BitString cloned = clone().bits;
    //    cloned.readBits(position - 1);
    //    return cloned.readBit();
    return bits.get(bits.readCursor - 1 + position);
  }

  public int getFreeBits() {
    return getRestBits();
  }

  public int getRestBits() {
    return bits.getUsedBits();
  }

  public CellSliceLazy skipBits(int length) {
    checkBitsOverflow(length);
    // Skip bits in one operation instead of a loop
    bits.readCursor += length;
    return this;
  }

  public CellSliceLazy skipBit() {
    checkBitsOverflow(1);
    bits.readBit();
    return this;
  }

  /**
   * @param length in bits
   * @return unsigned byte array
   */
  public byte[] loadBytes(int length) {
    checkBitsOverflow(length);
    BitString bitString = bits.readBits(length);
    return bitString.toByteArray();
  }

  public List<BigInteger> loadList(int elementNum, int elementBitLength) {
    checkBitsOverflow(elementNum * elementBitLength);
    List<BigInteger> result = new ArrayList<>(elementNum);
    for (int i = 0; i < elementNum; i++) {
      result.add(bits.readUint(elementBitLength));
    }
    return result;
  }

  /**
   * load rest of bytes
   *
   * @return unsigned byte array
   */
  public int[] loadBytes() {
    BitString bitString = bits.readBits();
    return bitString.toUnsignedByteArray();
  }

  public byte[] loadSignedBytes() {
    BitString bitString = bits.readBits();
    return bitString.toSignedByteArray();
  }

  /**
   * Load a slice of bits as an array of unsigned bytes (represented as ints) Optimized version that
   * reduces intermediate object creation
   *
   * @param length Number of bits to load
   * @return Array of unsigned bytes (as ints)
   */
  public int[] loadSlice(int length) {
    checkBitsOverflow(length);

    // Calculate how many bytes we'll need
    int bytesNeeded = (length + 7) / 8;
    int[] result = new int[bytesNeeded];

    // Save current position
    int savedPosition = bits.readCursor;

    // Read directly into result array without creating intermediate BitString
    for (int i = 0; i < bytesNeeded; i++) {
      int value = 0;
      for (int j = 0; j < 8 && (i * 8 + j) < length; j++) {
        if (bits.readCursor < bits.writeCursor) {
          if (bits.readBit()) {
            value |= (1 << (7 - j));
          }
        }
      }
      result[i] = value & 0xFF;
    }

    // If we didn't read exactly 'length' bits due to partial byte at the end,
    // adjust the cursor position
    if (bits.readCursor != savedPosition + length) {
      bits.readCursor = savedPosition + length;
    }

    return result;
  }

  public String loadString(int length) {
    checkBitsOverflow(length);
    BitString bitString = bits.readBits(length);
    return new String(bitString.toByteArray());
  }

  /**
   * Loads the very long string data, from the rest of the cell and nested refs
   *
   * @return String
   */
  public String loadSnakeString() {
    // Estimate initial capacity to reduce StringBuilder reallocations
    int estimatedCapacity = Math.max(256, bits.getUsedBits() / 8 * 2);
    StringBuilder s = new StringBuilder(estimatedCapacity);
    CellSliceLazy ref = this.clone();

    while (nonNull(ref)) {
      try {
        // Get all bits at once
        BitString bitString = ref.loadBits(ref.bits.getUsedBits());

        // Convert to string and append
        byte[] bytes = bitString.toByteArray();
        if (bytes.length > 0) {
          s.append(new String(bytes, StandardCharsets.UTF_8));
        }

        if (ref.refs.size() > 1) {
          throw new Error("more than one ref, it is not snake string");
        }

        if (ref.refs.size() == 1) {
          byte[] hash = Utils.slice(hashes, 0, 32);
          hashes = Arrays.copyOfRange(hashes, 32, hashes.length); // remove hash
          Cell value = getRefByHash(hash);
          ref = CellSliceLazy.beginParse(cellDbReader, value);
          continue;
        }
      } catch (Throwable e) {
        return null;
      }
      ref = null;
    }

    return s.toString();
  }

  public BitString loadBits(int length) {
    checkBitsOverflow(length);
    return bits.readBits(length);
  }

  public BigInteger loadInt(int length) {
    return bits.readInt(length);
  }

  public BigInteger loadIntMaybe(int length) {
    if (bits.readBit()) {
      return bits.readInt(length);
    } else {
      return null;
    }
  }

  public BigInteger loadUintMaybe(int length) {
    if (bits.readBit()) {
      return bits.readUint(length);
    } else {
      return null;
    }
  }

  public BigInteger loadUint(int length) {
    checkBitsOverflow(length);
    if (length == 0) return BigInteger.ZERO;
    return new BigInteger(loadBits(length).toBitString(), 2);
  }

  public BigInteger preloadInt(int bitLength) {
    // Save current position instead of cloning the entire BitString
    int savedPosition = bits.readCursor;
    try {
      BigInteger result = loadInt(bitLength);
      // Restore position
      bits.readCursor = savedPosition;
      return result;
    } catch (Throwable e) {
      // Restore position on error
      bits.readCursor = savedPosition;
      throw e;
    }
  }

  /**
   * Preload an unsigned integer without advancing the read cursor Optimized version with special
   * handling for small integers
   *
   * @param bitLength Length of the integer in bits
   * @return The integer value
   */
  public BigInteger preloadUint(int bitLength) {
    // Fast path for common small bit lengths
    if (bitLength <= 64 && bits.readCursor + bitLength <= bits.writeCursor) {
      // Save current position
      int savedPosition = bits.readCursor;
      try {
        // For small integers, use a more efficient approach
        long result = 0;
        for (int i = 0; i < bitLength; i++) {
          if (bits.readBit()) {
            result = (result << 1) | 1;
          } else {
            result = result << 1;
          }
        }
        // Restore position
        bits.readCursor = savedPosition;
        return BigInteger.valueOf(result);
      } catch (Throwable e) {
        // Restore position on error
        bits.readCursor = savedPosition;
        return BigInteger.ZERO;
      }
    } else {
      // For larger integers, use the standard approach
      int savedPosition = bits.readCursor;
      try {
        BigInteger result = loadUint(bitLength);
        // Restore position
        bits.readCursor = savedPosition;
        return result;
      } catch (Throwable e) {
        // Restore position on error
        bits.readCursor = savedPosition;
        return BigInteger.ZERO;
      }
    }
  }

  public BigInteger loadUintLEQ(BigInteger n) {
    BigInteger result = loadUint(n.bitLength());
    if (result.compareTo(n) > 0) {
      throw new Error("Cannot load {<= x}: encoded number is too high");
    }
    return result;
  }

  /** Loads unsigned integer less than n by reading minimal number of bits encoding n-1 */
  public BigInteger loadUintLess(BigInteger n) {
    return loadUintLEQ(n.subtract(BigInteger.ONE));
  }

  /**
   * Loads VarUInteger
   *
   * <pre>
   *     var_uint$_ {n:#} len:(#&lt; n) value:(uint (len * 8)) = VarUInteger n;
   * </pre>
   */
  public BigInteger loadVarUInteger(int value) {
    int len = loadUint(BigInteger.valueOf(value - 1).bitLength()).intValue();
    if (len == 0) {
      return BigInteger.ZERO;
    } else {
      return loadUint(len * 8);
    }
  }

  /**
   * Loads coins amount
   *
   * <p>nanograms$_ amount:(VarUInteger 16) = Grams;
   */
  public BigInteger loadCoins() {
    return loadVarUInteger(16);
  }

  public BigInteger preloadCoins() {
    // Save current position instead of cloning the entire BitString
    int savedPosition = bits.readCursor;
    try {
      BigInteger result = loadVarUInteger(16);
      // Restore position
      bits.readCursor = savedPosition;
      return result;
    } catch (Throwable e) {
      // Restore position on error
      bits.readCursor = savedPosition;
      throw e;
    }
  }

  public BigInteger skipCoins() {
    return loadCoins();
  }

  void checkBitsOverflow(int length) {
    if (length > bits.getUsedBits()) {
      throw new Error(
          "Bits overflow. Can't load " + length + " bits. " + bits.getUsedBits() + " bits left.");
    }
  }

  void checkRefsOverflow() {
    if (refs.isEmpty()) {
      throw new Error("Refs overflow. No more refs.");
    }
  }

  public String toString() {
    return bits.toHex();
  }

  public Address loadAddress() {
    BigInteger i = preloadUint(2);
    if (i.intValue() == 0) {
      skipBits(2);
      return null;
    }
    loadBits(2);
    loadBits(1);
    int workchain = loadInt(8).intValue();
    BigInteger hashPart = loadUint(256);

    String address =
        workchain + ":" + String.format("%64s", hashPart.toString(16)).replace(' ', '0');
    return Address.of(address);
  }

  public byte[] getHashes() {
    return hashes;
  }

  public Cell getRefByHash(byte[] hash) {
    try {
      byte[] value = cellDbReader.getCellDb().get(hash);
      //      System.out.println("looking for hash " + Utils.bytesToHex(hash));
      if (value == null) {
        throw new RuntimeException("Cannot find cell with hash " + Utils.bytesToHex(hash));
      }
      return parseCell(ByteBuffer.wrap(value).order(ByteOrder.LITTLE_ENDIAN));
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
