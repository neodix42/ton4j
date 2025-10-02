package org.ton.ton4j.cell;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static org.ton.ton4j.cell.CellType.ORDINARY;
import static org.ton.ton4j.cell.CellType.UNKNOWN;

import java.io.Serializable;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.ton.ton4j.bitstring.BitString;
import org.ton.ton4j.utils.Utils;

@Slf4j
public class Cell implements Serializable {

  @Getter BitString bits;
  // Use more efficient list implementation for refs (most cells have 0-4 refs)
  List<Cell> refs;
  private CellType type;
  public int index;
  @Getter public boolean exotic;
  public LevelMask levelMask;
  // Use lazy initialization for hashes and depthLevels
  @Getter private byte[] hashes;
  @Getter private int[] depthLevels;

  public List<Cell> getRefs() {
    return new ArrayList<>(refs);
  }

  @Override
  public int hashCode() {
    return new BigInteger(this.getHash()).intValue();
  }

  @Override
  public boolean equals(Object o) {
    if (o instanceof Cell) {
      return Arrays.equals(this.getHash(), ((Cell) o).getHash());
    } else {
      return false;
    }
  }

  public Cell() {
    this.bits = new BitString();
    // Initialize with a modifiable list to allow adding refs
    this.refs = new ArrayList<>(4); // TON cells have max 4 refs
    this.exotic = false;
    this.type = ORDINARY;
    this.levelMask = new LevelMask(0);
    // Lazy initialization for hashes and depthLevels
    this.hashes = new byte[0];
    this.depthLevels = new int[0];
  }

  public Cell(int bitSize) {
    this.bits = new BitString(bitSize);
    // Initialize with a modifiable list to allow adding refs
    this.refs = new ArrayList<>(4); // TON cells have max 4 refs
    this.exotic = false;
    this.type = ORDINARY;
    this.levelMask = resolveMask();
    // Lazy initialization for hashes and depthLevels
    this.hashes = new byte[0];
    this.depthLevels = new int[0];
  }

  public Cell(BitString bits, List<Cell> refs) {
    this.bits = new BitString(bits.getUsedBits());
    this.bits.writeBitString(bits.clone());

    // Optimize refs initialization based on size
    if (refs == null || refs.isEmpty()) {
      this.refs = Collections.emptyList();
    } else if (refs.size() <= 4) { // TON cells have max 4 refs
      this.refs = new ArrayList<>(refs);
    } else {
      this.refs = new ArrayList<>(refs);
    }

    this.exotic = false;
    this.type = ORDINARY;
    this.levelMask = new LevelMask(0);

    // Initialize hashes and depthLevels as empty arrays
    this.hashes = new byte[0];
    this.depthLevels = new int[0];
  }

  public Cell(BitString bits, List<Cell> refs, int cellType) {
    this.bits = new BitString(bits.getUsedBits());
    this.bits.writeBitString(bits.clone());
    this.refs = new ArrayList<>(refs);
    this.exotic = false;
    this.type = toCellType(cellType);
    this.levelMask = new LevelMask(0);
  }

  public Cell(BitString bits, int bitSize, List<Cell> refs, boolean exotic, LevelMask levelMask) {
    this.bits = new BitString(bitSize);
    this.bits.writeBitString(bits);
    this.refs = new ArrayList<>(refs);
    this.exotic = exotic;
    this.type = ORDINARY;
    this.levelMask = levelMask;
  }

  public Cell(BitString bits, int bitSize, List<Cell> refs, boolean exotic, CellType cellType) {
    this.bits = new BitString(bitSize);
    this.bits.writeBitString(bits);
    this.refs = new ArrayList<>(refs);
    this.exotic = exotic;
    this.type = cellType;
    this.levelMask = resolveMask();
  }

  public Cell(BitString bits, int bitSize, List<Cell> refs, CellType cellType) {
    this.bits = new BitString(bitSize);
    this.bits.writeBitString(bits);
    this.refs = new ArrayList<>(refs);
    this.type = cellType;
    this.levelMask = resolveMask();
  }

  public static CellType toCellType(int cellType) {
    switch (cellType) {
      case -1:
        return CellType.ORDINARY;
      case 1:
        return CellType.PRUNED_BRANCH;
      case 2:
        return CellType.LIBRARY;
      case 3:
        return CellType.MERKLE_PROOF;
      case 4:
        return CellType.MERKLE_UPDATE;
      default:
        return CellType.UNKNOWN;
    }
  }

  public LevelMask resolveMask() {
    // taken from pytoniq-core
    if (this.type == ORDINARY) {
      // Ordinary Cell level = max(Cell refs)
      int mask = 0;
      for (Cell r : refs) {
        mask |= r.getMaxLevel();
      }
      return new LevelMask(mask);
    } else if (this.type == CellType.PRUNED_BRANCH) {
      if (!refs.isEmpty()) {
        throw new Error("Pruned branch must not have refs");
      }
      BitString bs = bits.clone();
      bs.readUint8();

      return new LevelMask(bs.readUint8().intValue());
    } else if (this.type == CellType.MERKLE_PROOF) {
      // merkle proof cell has exactly one ref
      return new LevelMask(refs.get(0).levelMask.getMask() >> 1);
    } else if (this.type == CellType.MERKLE_UPDATE) {
      // merkle update cell has exactly 2 refs
      return new LevelMask(refs.get(0).levelMask.getMask() | refs.get(1).levelMask.getMask() >> 1);
    } else if (this.type == CellType.LIBRARY) {
      return new LevelMask(0);
    } else {
      throw new Error("Unknown cell type " + this.type);
    }
  }

  /**
   * Calculate cell hashes - highly optimized version This is a performance-critical method used in
   * many operations
   */
  public void calculateHashes() {

    int totalHashCount = levelMask.getHashIndex() + 1;
    hashes = new byte[32 * totalHashCount];
    depthLevels = new int[totalHashCount];

    int hashCount = totalHashCount;
    if (type == CellType.PRUNED_BRANCH) {
      hashCount = 1;
    }

    int hashIndexOffset = totalHashCount - hashCount;
    int hashIndex = 0;
    int level = levelMask.getLevel();

    int off;

    for (int li = 0; li <= level; li++) {
      if (!levelMask.isSignificant(li)) {
        continue;
      }
      if (hashIndex < hashIndexOffset) {
        hashIndex++;
        continue;
      }

      byte[] dsc = getDescriptors(levelMask.apply(li).getLevel());

      byte[] hash = new byte[0];
      hash = Utils.concatBytes(hash, dsc);

      if (hashIndex == hashIndexOffset) {
        if ((li != 0) && (type != CellType.PRUNED_BRANCH)) {
          throw new Error("invalid cell");
        }

        byte[] data = getDataBytes();
        hash = Utils.concatBytes(hash, data);
      } else {
        if ((li == 0) && (type == CellType.PRUNED_BRANCH)) {
          throw new Error("neither pruned nor 0");
        }
        off = hashIndex - hashIndexOffset - 1;
        byte[] partHash = new byte[32];
        System.arraycopy(hashes, off * 32, partHash, 0, (off + 1) * 32);
        hash = Utils.concatBytes(hash, partHash);
      }

      int depth = 0;

      for (Cell r : refs) {
        int childDepth;
        if ((type == CellType.MERKLE_PROOF) || (type == CellType.MERKLE_UPDATE)) {
          childDepth = r.getDepth(li + 1);
        } else {
          childDepth = r.getDepth(li);
        }

        hash = Utils.concatBytes(hash, Utils.intToByteArray(childDepth));
        if (childDepth > depth) {
          depth = childDepth;
        }
      }
      if (!refs.isEmpty()) {
        depth++;
        if (depth >= 1024) {
          throw new Error("depth is more than max depth (1023)");
        }
      }

      for (Cell r : refs) {
        if ((type == CellType.MERKLE_PROOF) || (type == CellType.MERKLE_UPDATE)) {
          hash = Utils.concatBytes(hash, r.getHash(li + 1));
        } else {
          hash = Utils.concatBytes(hash, r.getHash(li));
        }
      }

      off = hashIndex - hashIndexOffset;
      depthLevels[off] = depth;
      System.arraycopy(Utils.sha256AsArray(hash), 0, hashes, off * 32, 32);
      hashIndex++;
    }
  }

  void setCellType(CellType pCellType) {
    type = pCellType;
  }

  void setExotic(boolean pExotic) {
    exotic = pExotic;
  }

  void setLevelMask(LevelMask pLevelMask) {
    levelMask = pLevelMask;
  }

  /**
   * Converts BoC in hex string to Cell
   *
   * @param data hex string containing valid BoC
   * @return Cell
   */
  public static Cell fromBoc(String data) {
    return fromBocMultiRoot(Utils.hexToSignedBytes(data)).get(0);
  }

  /**
   * Converts BoC in base64 string to Cell
   *
   * @param data base64 string containing valid BoC
   * @return Cell
   */
  public static Cell fromBocBase64(String data) {
    return fromBocMultiRoot(Utils.base64ToSignedBytes(data)).get(0);
  }

  public static Cell fromBoc(byte[] data) {
    return fromBocMultiRoot(data).get(0);
  }

  public static List<Cell> fromBocMultiRoots(String data) {
    return fromBocMultiRoot(Utils.hexToSignedBytes(data));
  }

  public static List<Cell> fromBocMultiRoots(byte[] data) {
    return fromBocMultiRoot(data);
  }

  public String toString() {
    //    return bits.toHex();
    return toHex(false);
  }

  public int getBitLength() {
    return bits.toBitString().length();
  }

  public Cell clone() {
    Cell c = new Cell();
    c.bits = this.bits.clone();

    // Always use a modifiable list
    if (this.refs.isEmpty()) {
      // No refs to copy - use empty modifiable list
      c.refs = new ArrayList<>(4);
    } else {
      // Copy refs to a new modifiable list
      c.refs = new ArrayList<>(this.refs);
    }

    c.exotic = this.exotic;
    c.type = this.type;
    c.levelMask = this.levelMask.clone();

    // Only copy hash data if it exists - use fast System.arraycopy for better performance
    if (this.hashes.length > 0) {
      c.hashes = new byte[this.hashes.length];
      System.arraycopy(this.hashes, 0, c.hashes, 0, this.hashes.length);
    }
    if (this.depthLevels.length > 0) {
      c.depthLevels = new int[this.depthLevels.length];
      System.arraycopy(this.depthLevels, 0, c.depthLevels, 0, this.depthLevels.length);
    }
    return c;
  }

  public void writeCell(Cell anotherCell) {
    // Avoid unnecessary cloning and optimize for common cases
    bits.writeBitString(anotherCell.bits);

    // Optimize for the common case of few refs
    int refsSize = anotherCell.refs.size();
    if (refsSize == 0) {
      // No refs to add
      return;
    } else if (refsSize == 1) {
      // Single ref - avoid list iteration
      refs.add(anotherCell.refs.get(0));
    } else {
      // Multiple refs
      refs.addAll(anotherCell.refs);
    }
  }

  public int getMaxRefs() {
    return 4;
  }

  public int getFreeRefs() {
    return getMaxRefs() - refs.size();
  }

  public int getUsedRefs() {
    return refs.size();
  }

  /**
   * Loads bitString to Cell. Refs are not taken into account.
   *
   * @param hexBitString - bitString in hex
   * @return Cell
   */
  public static Cell fromHex(String hexBitString) {
    try {
      boolean incomplete = hexBitString.endsWith("_");

      hexBitString = hexBitString.replaceAll("_", "");
      int[] b = Utils.hexToInts(hexBitString);

      BitString bs = new BitString(b);

      Boolean[] ba = bs.toBooleanArray();
      int i = ba.length - 1;
      // drop last elements up to first `1`, if incomplete
      while (incomplete && !ba[i]) {
        ba = Arrays.copyOf(ba, ba.length - 1);
        i--;
      }
      // if incomplete, drop the 1 as well
      if (incomplete) {
        ba = Arrays.copyOf(ba, ba.length - 1);
      }
      BitString bss = new BitString(ba.length);
      bss.writeBitArray(ba);

      if (bss.length < 1024) {
        return CellBuilder.beginCell().storeBitString(bss).endCell();
      } else {
        return CellBuilder.beginCell().storeSnakeString(new String(bss.toByteArray())).endCell();
      }
    } catch (Exception e) {
      throw new Error("Cannot convert hex BitString to Cell. Error " + e.getMessage());
    }
  }

  /**
   * loads cell data without refs
   *
   * @param data
   * @return
   */
  public static Cell fromBytes(byte[] data) {
    return CellBuilder.beginCell().storeBytes(data).endCell();
  }

  /**
   * loads cell data without refs bypassing 1023 bits limit
   *
   * @param data
   * @return
   */
  public static Cell fromBytesUnlimited(byte[] data) {
    return CellBuilder.beginCell().storeBytesUnlimited(data).endCell();
  }

  static List<Cell> fromBocMultiRoot(byte[] data) {
    if (data.length < 10) {
      throw new Error("Invalid boc");
    }
    byte[] reachBocMagicPrefix = Utils.hexToSignedBytes("B5EE9C72");

    UnsignedByteReader r = new UnsignedByteReader(data);
    if (!Utils.compareBytes(reachBocMagicPrefix, r.readBytes(4))) {
      throw new Error("Invalid boc magic header");
    }

    BocFlags bocFlags = parseBocFlags(r.readSignedByte());
    int dataSizeBytes = r.readByte(); // off_bytes:(## 8) { off_bytes <= 8 }

    long cellsNum =
        Utils.dynInt(r.readSignedBytes(bocFlags.cellNumSizeBytes)); // cells:(##(size * 8))
    long rootsNum =
        Utils.dynInt(
            r.readSignedBytes(bocFlags.cellNumSizeBytes)); // roots:(##(size * 8)) { roots >= 1 }

    r.readBytes(bocFlags.cellNumSizeBytes);
    long dataLen = Utils.dynInt(r.readSignedBytes(dataSizeBytes));

    if (bocFlags.hasCrc32c) {
      byte[] bocWithoutCrc = Arrays.copyOfRange(data, 0, data.length - 4);
      byte[] crcInBoc = Arrays.copyOfRange(data, data.length - 4, data.length);
      byte[] crc32 = Utils.getCRC32ChecksumAsBytesReversed(bocWithoutCrc);
      if (!Utils.compareBytes(crc32, crcInBoc)) {
        throw new Error("Crc32c hash mismatch");
      }
    }

    int[] rootsIndex = new int[(int) rootsNum];
    for (int i = 0; i < rootsNum; i++) {
      rootsIndex[i] = Utils.dynInt(r.readSignedBytes(bocFlags.cellNumSizeBytes));
    }

    if (bocFlags.hasCacheBits && !bocFlags.hasIndex) {
      throw new Error("cache flag cant be set without index flag");
    }

    int[] index = new int[0];
    int j = 0;
    if (bocFlags.hasIndex) {
      index = new int[(int) cellsNum];
      byte[] idxData = r.readSignedBytes(cellsNum * dataSizeBytes);

      for (int i = 0; i < cellsNum; i++) {
        int off = i * dataSizeBytes;
        int val = Utils.dynInt(Arrays.copyOfRange(idxData, off, off + dataSizeBytes));
        if (bocFlags.hasCacheBits) {
          val = val / 2;
        }
        index[j++] = val;
      }
    }

    if (cellsNum > dataLen / 2) {
      throw new Error(
          "cells num looks malicious: data len " + Arrays.toString(data) + ", cells " + cellsNum);
    }

    byte[] payload = r.readBytes(dataLen);

    return parseCells(rootsIndex, rootsNum, cellsNum, bocFlags.cellNumSizeBytes, payload, index);
  }

  private static List<Cell> parseCells(
      int[] rootsIndex, long rootsNum, long cellsNum, int refSzBytes, byte[] data, int[] index) {
    Cell[] cells = new Cell[(int) cellsNum];
    for (int i = 0; i < cellsNum; i++) {
      cells[i] = new Cell();
    }

    int offset = 0;
    for (int i = 0; i < cellsNum; i++) {
      if ((data.length - offset) < 2) {
        throw new Error("failed to parse cell header, corrupted data");
      }

      if (nonNull(index) && (index.length != 0)) {
        // if we have index, then set offset from it, it stores end of each cell
        offset = 0;
        if (i > 0) {
          offset = index[i - 1];
        }
      }

      int flags = data[offset];
      int refsNum = flags & 0b111;
      boolean special = (flags & 0b1000) != 0;
      boolean withHashes = (flags & 0b10000) != 0;
      LevelMask levelMask = new LevelMask(flags >> 5);

      if (refsNum > 4) {
        throw new Error("too many refs in cell");
      }

      int ln = data[offset + 1] & 0xFF;
      int oneMore = ln % 2;
      int sz = (ln / 2 + oneMore);

      offset += 2;
      if ((data.length - offset) < sz) {
        throw new Error("failed to parse cell payload, corrupted data");
      }

      if (withHashes) {
        int maskBits = (int) Math.ceil(Math.log(levelMask.mask + 1) / Math.log(2));
        int hashesNum = maskBits + 1;
        offset += hashesNum * 32 + hashesNum * 2;
      }
      byte[] payload = Arrays.copyOfRange(data, offset, offset + sz);

      offset += sz;
      if ((data.length - offset) < (refsNum * refSzBytes)) {
        throw new Error("failed to parse cell refs, corrupted data");
      }

      int[] refsIndex = new int[refsNum];
      int x = 0;
      for (int j = 0; j < refsNum; j++) {
        byte[] t = Arrays.copyOfRange(data, offset, offset + refSzBytes);
        refsIndex[x++] = Utils.dynInt(t);

        offset += refSzBytes;
      }

      Cell[] refs = new Cell[refsIndex.length];

      for (int y = 0; y < refsIndex.length; y++) {
        if ((refsIndex[y] < i) && (isNull(index))) {
          throw new Error("reference to index which is behind parent cell");
        }

        if (refsIndex[y] >= cells.length) {
          throw new Error("invalid index, out of scope");
        }

        refs[y] = cells[refsIndex[y]];
      }

      int bitSz = ln * 4;

      // if not full byte
      if ((ln % 2) != 0) {
        // find last bit of byte which indicates the end and cut it and next
        for (int y = 0; y < 8; y++) {
          if (((payload[payload.length - 1] >> y) & 1) == 1) {
            bitSz += 3 - y;
            break;
          }
        }
      }

      cells[i].bits = new BitString(payload, bitSz);
      cells[i].refs = Arrays.asList(refs);
      cells[i].exotic = special; // i = 264 / ddnew 0101
      cells[i].levelMask = levelMask;

      cells[i].type = cells[i].getCellType();
    }

    Cell[] roots = new Cell[rootsIndex.length];

    for (int i = cells.length - 1; i >= 0; i--) {
      cells[i].calculateHashes();
    }

    for (int i = 0; i < rootsIndex.length; i++) {
      roots[i] = cells[rootsIndex[i]];
    }

    return Arrays.asList(roots);
  }

  /**
   * has_idx:(## 1) has_crc32c:(## 1) has_cache_bits:(## 1) flags:(## 2) { flags = 0 } size:(## 3) {
   * size <= 4 }
   *
   * @param data raw data
   * @return BocFlags
   */
  static BocFlags parseBocFlags(byte data) {
    BocFlags bocFlags = new BocFlags();
    bocFlags.hasIndex = (data & (1 << 7)) > 0;
    bocFlags.hasCrc32c = (data & (1 << 6)) > 0;
    bocFlags.hasCacheBits = (data & (1 << 5)) > 0;
    bocFlags.cellNumSizeBytes = (data & 0b00000111);
    return bocFlags;
  }

  /**
   * Recursively prints cell's content like Fift
   *
   * @return String
   */
  public String print(String indent) {
    String t = "x";
    if (this.type == CellType.MERKLE_PROOF) {
      t = "p";
    } else if (this.type == CellType.MERKLE_UPDATE) {
      t = "u";
    } else if (this.type == CellType.PRUNED_BRANCH) {
      t = "P";
    }
    StringBuilder s = new StringBuilder(indent + t + "{" + bits.toHex() + "}\n");
    if (nonNull(refs) && !refs.isEmpty()) {
      for (Cell i : refs) {
        if (nonNull(i)) {
          s.append(i.print(indent + " "));
        }
      }
    }
    return s.toString();
  }

  public String print() {
    return print("");
  }

  /** Saves BoC to file */
  public void toFile(String filename, boolean withCrc) {
    byte[] boc = toBoc(withCrc);
    try {
      Files.write(Paths.get(filename), boc);
    } catch (Exception e) {
      log.error("Cannot write to file. Error: {} ", e.getMessage());
    }
  }

  public void toFile(String filename) {
    toFile(filename, true);
  }

  public String toHex(boolean withCrc) {
    return Utils.bytesToHex(toBoc(withCrc));
  }

  public String toHex(boolean withCrc, boolean withIndex) {
    return Utils.bytesToHex(toBoc(withCrc, withIndex));
  }

  public String toHex(boolean withCrc, boolean withIndex, boolean withCacheBits) {
    return Utils.bytesToHex(toBoc(withCrc, withIndex, withCacheBits));
  }

  public String toHex(
      boolean withCrc, boolean withIndex, boolean withCacheBits, boolean withTopHash) {
    return Utils.bytesToHex(toBoc(withCrc, withIndex, withCacheBits, withTopHash));
  }

  public String toHex(
      boolean withCrc,
      boolean withIndex,
      boolean withCacheBits,
      boolean withTopHash,
      boolean withIntHashes) {
    return Utils.bytesToHex(toBoc(withCrc, withIndex, withCacheBits, withTopHash, withIntHashes));
  }

  /**
   * BoC to hex
   *
   * @return HexString
   */
  public String toHex() {
    return Utils.bytesToHex(toBoc(true));
  }

  public String bitStringToHex() {
    return bits.toHex();
  }

  public String toBitString() {
    return bits.toBitString();
  }

  public String toBase64() {
    return Utils.bytesToBase64(toBoc(true));
  }

  public String toBase64UrlSafe() {
    return Utils.bytesToBase64SafeUrl(toBoc(true));
  }

  public String toBase64(boolean withCrc) {
    return Utils.bytesToBase64(toBoc(withCrc));
  }

  public byte[] hash() {
    return getHash();
  }

  public byte[] getHash() {
    return getHash(levelMask.getLevel());
  }

  public String getShortHash() {
    String hashHex = Utils.bytesToHex(getHash(levelMask.getLevel()));
    return hashHex.substring(0, 4)
        + ".."
        + hashHex.substring(hashHex.length() - 5, hashHex.length() - 1);
  }

  public byte[] getHash(int lvl) {
    int hashIndex = levelMask.apply(lvl).getHashIndex();
    if (type == CellType.PRUNED_BRANCH) {
      int prunedHashIndex = levelMask.getHashIndex();
      if (hashIndex != prunedHashIndex) {
        return Arrays.copyOfRange(getDataBytes(), 2 + (hashIndex * 32), 2 + ((hashIndex + 1) * 32));
      }
      hashIndex = 0;
    }

    if (hashes.length != 0) {
      return Utils.slice(hashes, hashIndex * 32, 32);
    } else {
      byte[] bytes = new byte[32];
      return bytes;
    }
  }

  public byte[] getRefsDescriptor(int lvl) {
    byte[] d1 = new byte[1];
    d1[0] = (byte) (isNull(refs) ? 0 : refs.size() + ((exotic ? 1 : 0) * 8) + lvl * 32);
    return d1;
  }

  public byte[] getBitsDescriptor() {
    int bitsLength = bits.getUsedBits();
    byte d3 = (byte) ((bitsLength / 8) * 2);
    if ((bitsLength % 8) != 0) {
      d3++;
    }
    return new byte[] {d3};
  }

  public int getMaxLevel() {
    // TODO level calculation differ for exotic cells
    int maxLevel = 0;
    for (Cell i : refs) {
      if (i.getMaxLevel() > maxLevel) {
        maxLevel = i.getMaxLevel();
      }
    }
    return maxLevel;
  }

  public byte[] toBoc() {
    return toBoc(true, false, false, false, false);
  }

  public byte[] toBoc(boolean withCRC) {
    return toBoc(withCRC, false, false, false, false);
  }

  public byte[] toBoc(boolean withCRC, boolean withIdx) {
    return toBoc(withCRC, withIdx, false, false, false);
  }

  public byte[] toBoc(boolean withCRC, boolean withIdx, boolean withCacheBits) {
    return toBoc(withCRC, withIdx, withCacheBits, false, false);
  }

  public byte[] toBoc(
      boolean withCRC, boolean withIdx, boolean withCacheBits, boolean withTopHash) {
    return toBoc(withCRC, withIdx, withCacheBits, withTopHash, false);
  }

  private byte[] internalToBoc(
      List<Cell> roots,
      boolean hasCrc32c,
      boolean hasIdx,
      boolean hasCacheBits,
      boolean hasTopHash,
      boolean hasIntHashes) {
    Pair<List<IdxItem>, Map<String, IdxItem>> sortedCellsAndIndex = flattenIndex(roots, hasTopHash);

    List<IdxItem> sortedCells = sortedCellsAndIndex.getLeft();
    Map<String, IdxItem> index = sortedCellsAndIndex.getRight();

    int cellSizeBits = Utils.log2(sortedCells.size() + 1);
    int cellSizeBytes = (int) Math.ceil((double) cellSizeBits / 8);

    byte[] payload = new byte[0];
    for (IdxItem sortedCell : sortedCells) {
      payload =
          Utils.concatBytes(
              payload,
              sortedCell.getCell().serialize(cellSizeBytes, index, sortedCell.isWithHash()));
      sortedCell.dataIndex = payload.length;
    }

    // bytes needed to store len of payload
    int sizeBits = Utils.log2Ceil(payload.length + 1);
    byte sizeBytes = (byte) ((sizeBits + 7) / 8);

    // has_idx 1bit, hash_crc32 1bit,  has_cache_bits 1bit, flags 2bit, size_bytes 3 bit
    byte flagsByte = 0;
    if (hasIdx) {
      flagsByte |= (byte) 0b1_0_0_00_000;
    }
    if (hasCrc32c) {
      flagsByte |= 0b0_1_0_00_000;
    }
    if (hasCacheBits) {
      flagsByte |= 0b0_0_1_00_000;
    }

    flagsByte |= (byte) cellSizeBytes;

    byte[] data = new byte[0];

    byte[] bocMagic = new byte[] {(byte) 0xB5, (byte) 0xEE, (byte) 0x9C, 0x72};

    data = Utils.concatBytes(data, bocMagic);

    data = Utils.concatBytes(data, Utils.byteToBytes(flagsByte));

    // bytes needed to store size
    data = Utils.concatBytes(data, Utils.byteToBytes(sizeBytes));

    // cells num
    data =
        Utils.concatBytes(
            data, Utils.dynamicIntBytes(BigInteger.valueOf(sortedCells.size()), cellSizeBytes));

    // roots num
    data = Utils.concatBytes(data, Utils.dynamicIntBytes(BigInteger.ONE, cellSizeBytes));

    // complete BOCs = 0
    data = Utils.concatBytes(data, Utils.dynamicIntBytes(BigInteger.ZERO, cellSizeBytes));

    // len of data
    data =
        Utils.concatBytes(
            data, Utils.dynamicIntBytes(BigInteger.valueOf(payload.length), sizeBytes));

    for (Cell c : roots) {
      data =
          Utils.concatBytes(
              data,
              Utils.dynamicIntBytes(index.get(Utils.bytesToHex(c.getHash())).index, cellSizeBytes));
    }

    if (hasIdx) {
      for (IdxItem sc : sortedCells) {
        long idx = sc.dataIndex;
        if (hasCacheBits) {
          idx *= 2;
          if (sc.repeats > 0) {
            // cache cells which has refs
            idx++;
          }
        }
        data = Utils.concatBytes(data, Utils.dynamicIntBytes(BigInteger.valueOf(idx), sizeBytes));
      }
    }

    data = Utils.appendByteArray(data, payload);

    if (hasCrc32c) {
      data = Utils.appendByteArray(data, Utils.getCRC32ChecksumAsBytesReversed(data));
    }

    return data;
  }

  public byte[] toBoc(
      boolean hasCrc32c,
      boolean hasIdx,
      boolean hasCacheBits,
      boolean hasTopHash,
      boolean hasIntHashes) {

    return internalToBoc(
        Collections.singletonList(this), hasCrc32c, hasIdx, hasCacheBits, hasTopHash, hasIntHashes);
  }

  public byte[] toBocMultiRoot(
      List<Cell> roots,
      boolean hasCrc32c,
      boolean hasIdx,
      boolean hasCacheBits,
      boolean hasTopHash,
      boolean hasIntHashes) {
    return internalToBoc(roots, hasCrc32c, hasIdx, hasCacheBits, hasTopHash, hasIntHashes);
  }

  /** reworked in order to coincide with tonutils-go */
  private Pair<List<IdxItem>, Map<String, IdxItem>> flattenIndex(
      List<Cell> roots, boolean hasTopHash) {
    Map<String, IdxItem> index = new HashMap<>();

    BigInteger idx = BigInteger.ZERO;

    while (!roots.isEmpty()) {
      List<Cell> next = new ArrayList<>(roots.size() * 4);
      for (Cell p : roots) {
        String hash = Utils.bytesToHex(p.getHash());

        IdxItem v = index.get(hash);
        if (nonNull(v)) {
          v.repeats++;
          continue;
        }

        index.put(hash, IdxItem.builder().cell(p).index(idx).withHash(hasTopHash).build());
        idx = idx.add(BigInteger.ONE);

        next.addAll(p.getRefs());
      }
      roots = next;
    }

    List<IdxItem> idxSlice = new ArrayList<>(index.size());
    for (Map.Entry<String, IdxItem> entry : index.entrySet()) {
      String key = entry.getKey();
      IdxItem value = entry.getValue();
      idxSlice.add(value);
    }

    idxSlice.sort(Comparator.comparing(lhs -> lhs.index));

    boolean verifyOrder = true;
    while (verifyOrder) {
      verifyOrder = false;
      for (IdxItem id : idxSlice) {
        for (Cell ref : id.getCell().getRefs()) {
          IdxItem idRef = index.get(Utils.bytesToHex(ref.getHash()));
          if (idRef.index.compareTo(id.index) < 0) {
            idRef.index = idx;
            idx = idx.add(BigInteger.ONE); // idx++
            verifyOrder = true;
          }
        }
      }
    }
    idxSlice.sort(Comparator.comparing(lhs -> lhs.index));

    for (int i = 0; i < idxSlice.size(); i++) {
      idxSlice.get(i).index = BigInteger.valueOf(i);
    }

    return Pair.of(idxSlice, index);
  }

  private byte[] serialize(int refIndexSzBytes, Map<String, IdxItem> index, boolean hasHash) {
    byte[] body = this.getBits().toByteArray();
    int unusedBits = 8 - (bits.getUsedBits() % 8);

    if (unusedBits != 8) {
      body[body.length - 1] += (byte) (1 << (unusedBits - 1));
    }

    int refsLn = this.getRefs().size() * refIndexSzBytes;
    int bufLn = 2 + body.length + refsLn;

    byte[] data;

    byte[] descriptors = getDescriptors(levelMask.getMask());

    data = Utils.concatBytes(descriptors, body);

    long refsOffset = bufLn - refsLn;
    for (Cell ref : refs) {
      String refIndex = Utils.bytesToHex(ref.getHash());
      byte[] src = Utils.dynamicIntBytes(index.get(refIndex).index, refIndexSzBytes);
      data = Utils.concatBytes(data, src);
    }

    return data;
  }

  private byte[] getDescriptors(int lvl) {
    return Utils.concatBytes(getRefsDescriptor(lvl), getBitsDescriptor());
  }

  private int getDepth(int lvlMask) {
    int hashIndex = levelMask.apply(lvlMask).getHashIndex();
    if (type == CellType.PRUNED_BRANCH) {
      int prunedHashIndex = levelMask.getHashIndex();
      if (hashIndex != prunedHashIndex) {
        int off = 2 + 32 * prunedHashIndex + hashIndex * 2;
        return Utils.bytesToIntX(Utils.slice(getDataBytes(), off, 2));
      }
    }
    return depthLevels[hashIndex];
  }

  private byte[] getDataBytes() {
    if ((bits.getUsedBits() % 8) > 0) {
      byte[] a = bits.toBitString().getBytes(StandardCharsets.UTF_8);
      int sz = a.length;
      byte[] b = new byte[sz + 1];

      System.arraycopy(a, 0, b, 0, sz);
      b[sz] = (byte) '1';

      int mod = b.length % 8;
      if (mod > 0) {
        b = Utils.rightPadBytes(b, b.length + (8 - mod), '0');
      }

      return Utils.bitStringToByteArray(new String(b));
    } else {
      return bits.toByteArray();
    }
  }

  public static CellType getCellType(Cell c) {
    return c.getCellType();
  }

  public CellType getCellType() {
    if (!exotic) {
      return ORDINARY;
    }

    if (bits.getUsedBits() < 8) {
      return UNKNOWN;
    }

    BitString clonedBits = bits.clone();
    CellType cellType = toCellType(clonedBits.readUint(8).intValue());
    switch (cellType) {
      case PRUNED_BRANCH:
        {
          if (bits.getUsedBits() >= 288) {
            LevelMask msk = new LevelMask(clonedBits.readUint(8).intValue());
            int lvl = msk.getLevel();
            if ((lvl > 0)
                && (lvl <= 3)
                && (bits.getUsedBits()
                    >= 16 + (256 + 16) * msk.apply(lvl - 1).getHashIndex() + 1)) {
              return CellType.PRUNED_BRANCH;
            }
          }
        }
      case MERKLE_PROOF:
        {
          if ((refs.size() == 1) && (bits.getUsedBits() == 280)) {
            return CellType.MERKLE_PROOF;
          }
        }
      case MERKLE_UPDATE:
        {
          if ((refs.size() == 2) && (bits.getUsedBits() == 552)) {
            return CellType.MERKLE_UPDATE;
          }
        }
      case LIBRARY:
        {
          if (bits.getUsedBits() == (8 + 256)) {
            return CellType.LIBRARY;
          }
        }
    }
    return UNKNOWN;
  }
}
