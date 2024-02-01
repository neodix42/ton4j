package org.ton.java.cell;

import org.apache.commons.lang3.tuple.Pair;
import org.ton.java.bitstring.BitString;
import org.ton.java.tlb.types.Boc;
import org.ton.java.utils.Utils;

import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

public class Cell {

    public static final int ORDINARY_CELL_TYPE = 0x00;
    public static final int PRUNED_CELL_TYPE = 0x01;
    public static final int LIBRARY_CELL_TYPE = 0x02;
    public static final int MERKLE_PROOF_CELL_TYPE = 0x03;
    public static final int MERKLE_UPDATE_CELL_TYPE = 0x04;
    public static final int UNKNOWN_CELL_TYPE = 0xFF;

    private static final int[] SERIALIZED_BOC_PREFIX = Utils.hexToUnsignedBytes("B5EE9C72");
    private static final int[] SERIALIZED_BOC_IDX_PREFIX = Utils.hexToUnsignedBytes("68ff65f3");
    private static final int[] SERIALIZED_BOC_IDX_CRC32C = Utils.hexToUnsignedBytes("acc3a728");
    private static final int HASH_SIZE = 32;
    private static final int DEPTH_SIZE = 2;

    public BitString bits;
    public List<Cell> refs;

    public CellType type;
    private int cellType;
    private int[] refsIndexes;


    public int index;

    public boolean special;
    public LevelMask levelMask;

    public Cell() {
        this.bits = new BitString();
        this.refs = new ArrayList<>();
        this.special = false;
        this.type = CellType.ORDINARY;
        this.levelMask = new LevelMask(0);
    }

    public Cell(int bitSize) {
        this.bits = new BitString(bitSize);
        this.refs = new ArrayList<>();
        this.special = false;
        this.type = CellType.ORDINARY;
        this.levelMask = new LevelMask(0);
    }

    public Cell(BitString bits, List<Cell> refs) {
        this.bits = new BitString(bits.getLength());
        this.bits.writeBitString(bits.clone());
        this.refs = new ArrayList<>(refs);
        this.special = false;
        this.type = CellType.ORDINARY;
        this.levelMask = new LevelMask(0);
    }

    public Cell(BitString bits, List<Cell> refs, int cellType) {
        this.bits = new BitString(bits.getLength());
        this.bits.writeBitString(bits.clone());
        this.refs = new ArrayList<>(refs);
        this.special = false;
        this.type = toCellType(cellType);
        this.levelMask = new LevelMask(0);
    }

    public Cell(BitString bits, int bitSize, List<Cell> refs, boolean special, LevelMask levelMask) {
        this.bits = new BitString(bitSize);
        this.bits.writeBitString(bits);
        this.refs = new ArrayList<>(refs);
        this.special = special;
        this.type = toCellType(cellType);
        this.levelMask = levelMask;
    }

    public Cell(BitString bits, int bitSize, List<Cell> refs, boolean special, CellType cellType) {
        this.bits = new BitString(bitSize);
        this.bits.writeBitString(bits);
        this.refs = new ArrayList<>(refs);
        this.special = special;
        this.type = cellType;
        this.levelMask = resolveMask();
    }

    public Cell(BitString bits, int[] refsIndexes, int cellType) {
        this.bits = new BitString(bits);
        this.refsIndexes = refsIndexes;
        this.type = toCellType(cellType);
        this.cellType = cellType;
    }

    public Cell(BitString bits, int bitSize, List<Cell> refs, int cellType) {
        this.bits = new BitString(bitSize);
        this.bits.writeBitString(bits);
        this.refs = new ArrayList<>(refs);
        this.type = toCellType(cellType);
        this.cellType = cellType;
        this.levelMask = resolveMask();
    }

    public CellType toCellType(int cellType) {
        return switch (cellType) {
            case -1, 0 -> CellType.ORDINARY;
            case 1 -> CellType.PRUNED_BRANCH;
            case 2 -> CellType.LIBRARY;
            case 3 -> CellType.MERKLE_PROOF;
            case 4 -> CellType.MERKLE_UPDATE;
            default -> CellType.UNKNOWN;
        };
    }

    public LevelMask resolveMask() {
        // taken from pytoniq-core
        if (this.type == CellType.ORDINARY) {
            // Ordinary Cell level = max(Cell refs)
            int mask = 0;
            for (Cell r : refs) {
                mask |= r.getMaxLevel();
            }
            return new LevelMask(mask);
        } else if (this.type == CellType.PRUNED_BRANCH) {
            // prunned branch doesn't have refs
            if (!refs.isEmpty()) {
                throw new Error("Pruned branch must not has refs");
            }
            BitString bs = bits.clone();
            bs.readUint8();

            return new LevelMask(bs.readUint8().intValue()); // todo test
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

    public static Cell fromBoc(String data) {
        return fromBocMultiRoot(Utils.hexToUnsignedBytes(data)).get(0);
    }

    public static Cell fromBoc(int[] data) {
        return fromBocMultiRoot(data).get(0);
    }

    public static List<Cell> fromBocMultiRoots(String data) {
        return fromBocMultiRoot(Utils.hexToUnsignedBytes(data));
    }

    public static List<Cell> fromBocMultiRoots(int[] data) {
        return fromBocMultiRoot(data);
    }

    public String toString() {
        return bits.toHex();
    }

    public Cell clone() {
        Cell c = new Cell();
        c.bits = this.bits.clone();
        for (Cell refCell : this.refs) {
            c.refs.add(refCell.clone());
        }
        c.special = this.special;
        c.type = this.type;
        c.levelMask = this.levelMask.clone();
        return c;
    }

    public void writeCell(Cell anotherCell) {
        Cell cloned = anotherCell.clone();
        bits.writeBitString(cloned.bits);
        refs.addAll(cloned.refs);
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

            BitString bs = new BitString(hexBitString.length() * 8);
            bs.writeBytes(b);

            boolean[] ba = bs.toBooleanArray();
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

            BitString f = new BitString(bss.getBitString().length());
            f.writeBitString(bss);

            Cell c = new Cell();
            c.bits = f;
            return c;
        } catch (Exception e) {
            throw new Error("Cannot convert hex BitString to Cell. Error " + e.getMessage());
        }
    }

    /**
     * taken from pytoniq-core
     */
    static Pair<Cell, Integer> deserializeCell(int[] data, int refIndexSize) {
        int dataLen = data.length;
        int refsDescriptor = data[0];
        int level = refsDescriptor >> 5;
        int totalRefs = refsDescriptor & 7;
        boolean hasHashes = (refsDescriptor & 16) != 0;
        boolean isExotic = (refsDescriptor & 8) != 0;
        boolean isAbsent = (totalRefs == 7) && hasHashes;
        if (isAbsent) {
            throw new Error("Cannot deserialize absent cell");
        }
        int bitsDescriptor = data[1];
        boolean isAugmented = (bitsDescriptor & 1) != 0;
        int dataSize = (bitsDescriptor & 1) + (bitsDescriptor >> 1); // todo review !
        int hashesSize = (level + 1) * (hasHashes ? 32 : 0);
        int depthSize = (level + 1) * (hasHashes ? 2 : 0);
        int i = 2;

        if ((dataLen - i) < (hashesSize + depthSize + dataSize + refIndexSize * totalRefs)) {
            throw new Error("Not enough bytes to encode cell data");
        }

        if (hasHashes) {
            i += hashesSize + depthSize;
        }

        int[] ret = Arrays.copyOfRange(data, i, i + dataSize);
        i += dataSize;

        int end = 0;
        if (isAugmented && ret.length != 0) {
            // find last bit of byte which indicates the end and cut it and next        
            for (int y = 0; y < 8; y++) {
                if (((ret[ret.length - 1] >> y) & 1) == 1) {
                    end = y + 1;
                    break;
                }
            }
        }

        BitString bits = new BitString(ret, ret.length * 8 - end);

        int cellType = -1;
        if (isExotic) {
            if (bits.getLength() < 8) {
                throw new Error("not enough bytes for an exotic cell type");
            }
            cellType = bits.readUint(8).intValue();
        }
        int[] cellRefsIndex = new int[totalRefs];

        for (int j = 0; j < totalRefs; j++) {
            cellRefsIndex[j] = Utils.bytesToIntX(Arrays.copyOfRange(data, i, i + refIndexSize));
            i += refIndexSize;
        }

        Cell c = new Cell(bits, cellRefsIndex, cellType);
        return Pair.of(c, i);

    }

    static List<Cell> fromBocMultiRoot(int[] data) {
        if (data.length < 10) {
            throw new Error("Invalid boc");
        }
        Boc boc = deserializeBocHeader(data);
        Cell[] cells = new Cell[boc.getCells()];
        int i = 0;
        for (int x = 0; x < boc.getCells(); x++) {
            int[] ret = Arrays.copyOfRange(boc.getCellData(), i, boc.getCellData().length);
            Pair<Cell, Integer> ci = deserializeCell(ret, boc.getSize());
            i += ci.getRight();
            cells[x] = ci.getLeft();
        }

        for (int ci = boc.getCells() - 1; ci >= 0; ci--) {
            Cell c = cells[ci];
            List<Cell> refs = new ArrayList<>();

            for (int ri = 0; ri < c.refsIndexes.length; ri++) {
                int r = c.refsIndexes[ri];
                if (r < ci) {
                    throw new Error("Topological order is broken");
                }
                refs.add(cells[r]);
            }

            cells[ci] = new Cell(cells[ci].bits, refs, cells[ci].cellType);
        }

        List<Cell> rootCells = new ArrayList<>();
        for (int ri = 0; ri < boc.getRootList().size(); ri++) {
            rootCells.add(cells[ri]);
        }
        return rootCells;
    }

    private static Boc deserializeBocHeader(int data[]) {
        Cell rawCell = CellBuilder.beginCell(data.length * 8).storeBytes(data);
        CellSlice cs = CellSlice.beginParse(rawCell);
        Boc boc = Boc.builder()
                .magic(cs.loadUint(32).longValue())
                .build();
        boc.setHasIdx(cs.loadBit());
        boc.setHasCrc32c(cs.loadBit());
        boc.setHasCacheBits(cs.loadBit());
        boc.setFlags(cs.loadUint(2).intValue());
        boc.setSize(cs.loadUint(3).intValue());
        boc.setOffBytes(cs.loadUint(8).intValue());
        boc.setCells(cs.loadUint(boc.getSize() * 8).intValue());
        boc.setRoots(cs.loadUint(boc.getSize() * 8).intValue());
        boc.setAbsent(cs.loadUint(boc.getSize() * 8).intValue());
        boc.setTotalCellsSize(cs.loadUint(boc.getOffBytes() * 8).intValue());
        boc.setRootList(cs.loadList(boc.getRoots(), boc.getSize() * 8));
        boc.setIndex(boc.isHasIdx() ? cs.loadList(boc.getCells(), boc.getOffBytes() * 8) : null);
        boc.setCellData(cs.loadBytes(boc.getTotalCellsSize() * 8));
        boc.setCrc32c(boc.isHasCrc32c() ? cs.loadUint(32).longValue() : 0);

        return boc;
    }


    private static List<Cell> parseCells(long rootsNum, long cellsNum, int refSzBytes, int[] data, int[] index) {
        Cell[] cells = new Cell[(int) cellsNum];
        for (int i = 0; i < cellsNum; i++) {
            cells[i] = new Cell();
        }
        boolean[] referred = new boolean[(int) cellsNum];

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

            int ln = data[offset + 1];
            int oneMore = ln % 2;
            int sz = (ln / 2 + oneMore);

            offset += 2;
            if ((data.length - offset) < sz) {
                throw new Error("failed to parse cell payload, corrupted data");
            }

            if (withHashes) {
                int maskBits = (int) Math.ceil(Math.log(levelMask.mask + 1) / Math.log(2));
                int hashesNum = maskBits + 1;
                offset += hashesNum * HASH_SIZE + hashesNum * DEPTH_SIZE;
            }

            int[] payload = Arrays.copyOfRange(data, offset, offset + sz);

            offset += sz;
            if ((data.length - offset) < (refsNum * refSzBytes)) {
                throw new Error("failed to parse cell refs, corrupted data");
            }

            int[] refsIndex = new int[refsNum];
            int x = 0;
            for (int j = 0; j < refsNum; j++) {
                int[] t = Arrays.copyOfRange(data, offset, offset + refSzBytes);
                refsIndex[x++] = Utils.dynInt(t);

                offset += refSzBytes;
            }

            List<Cell> refs = new ArrayList<>();

            for (int id : refsIndex) {
                if (i == id) {
                    throw new Error("recursive reference of cells");
                }

                if ((id < i) && (isNull(index))) {
                    throw new Error("reference to index which is behind parent cell");
                }

                if (id >= cells.length) {
                    throw new Error("invalid index, out of scope");
                }

                refs.add(cells[id]);

                referred[id] = true;
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
            cells[i].refs = refs;
            cells[i].special = special;
            cells[i].levelMask = levelMask;
        }

        List<Cell> roots = new ArrayList<>((int) rootsNum);

        // get cells which are not referenced by another, these are root cells
        for (int i = 0; i < referred.length; i++) {
            if (!referred[i]) {
                roots.add(cells[i]);
            }
        }

        if (roots.size() != rootsNum) {
            throw new Error("roots num not match actual num");
        }

        return roots;
    }

    /**
     * Recursively prints cell's content like Fift
     *
     * @return String
     */
    public String print(String indent) {
        StringBuilder s = new StringBuilder(indent + "x{" + bits.toHex() + "}\n");
        if (nonNull(refs) && refs.size() > 0) {
            for (Cell i : refs) {
                if (nonNull(i)) {
                    s.append(i.print(indent + " "));
                }
            }
        }
        return s.toString();
    }

    public String print() {
        String indent = "";
        StringBuilder s = new StringBuilder(indent + "x{" + bits.toHex() + "}\n");
        if (nonNull(refs) && refs.size() > 0) {
            for (Cell i : refs) {
                if (nonNull(i)) {
                    s.append(i.print(indent + " "));
                }
            }
        }
        return s.toString();
    }

    /**
     * Saves BoC to file
     */
    public void toFile(String filename, boolean withCrc) {

        int[] boc = toBoc(withCrc);
        try {
            Files.write(Paths.get(filename), Utils.unsignedBytesToSigned(boc));
        } catch (Exception e) {
            System.err.println("Cannot write to file. " + e.getMessage());
        }
    }

    public void toFile(String filename) {
        toFile(filename, true);
    }

    public String toHex(boolean withCrc) {
        return Utils.bytesToHex(toBoc(withCrc));
    }

    public String toHex() {
        return Utils.bytesToHex(toBoc(true));
    }

    public String toBase64() {
        return Utils.bytesToBase64(toBoc(true));
    }

    public String toBase64(boolean withCrc) {
        return Utils.bytesToBase64(toBoc(withCrc));
    }

    public int[] hash() {
        int[] repr = getRepr();
        return Utils.hexToInts(Utils.sha256(repr));
    }

    int[] getRepr() {
        int[] reprArray = new int[0];

        reprArray = Utils.concatBytes(reprArray, getDataWithDescriptors());

        for (Cell cell : refs) {
            reprArray = Utils.concatBytes(reprArray, cell.getMaxDepthAsArray());
        }

        for (Cell cell : refs) {
            reprArray = Utils.concatBytes(reprArray, cell.hash());
        }

        int[] x = new int[0];
        x = Utils.concatBytes(x, reprArray);
        return x;
    }

    int[] getRefsDescriptor() {
        int[] d1 = new int[1];
        d1[0] = (refs.size() + ((special ? 1 : 0) * 8) + levelMask.getMask() * 32);
        return d1;
    }

    int[] getBitsDescriptor() {
        int[] d2 = new int[1];
        d2[0] = (int) (Math.ceil(bits.getUsedBits() / (double) 8) + Math.floor(bits.getUsedBits() / (double) 8));
        return d2;
    }

    int[] getDataWithDescriptors() {
        int[] d1 = getRefsDescriptor();
        int[] d2 = getBitsDescriptor();
        int[] tuBits = bits.getTopUppedArray();
        return Utils.concatBytes(Utils.concatBytes(d1, d2), tuBits);
    }

    int getMaxLevel() {
        //TODO level calculation differ for exotic cells
        int maxLevel = 0;
        for (Cell i : refs) {
            if (i.getMaxLevel() > maxLevel) {
                maxLevel = i.getMaxLevel();
            }
        }
        return maxLevel;
    }

    int[] getMaxDepthAsArray() {
        int maxDepth = getMaxDepth();
        int[] d = new int[2];
        d[1] = (int) (maxDepth % 256);
        d[0] = (int) Math.floor(maxDepth / (double) 256);
        return d;
    }

    int getMaxDepth() {
        int maxDepth = 0;
        if (!refs.isEmpty()) {
            for (Cell i : refs) {
                if (i.getMaxDepth() > maxDepth) {
                    maxDepth = i.getMaxDepth();
                }
            }
            maxDepth = maxDepth + 1;
        }
        return maxDepth;
    }

    /*
        serialized_boc#b5ee9c72 has_idx:(## 1) has_crc32c:(## 1)
          has_cache_bits:(## 1) flags:(## 2) { flags = 0 }
          size:(## 3) { size <= 4 }
          off_bytes:(## 8) { off_bytes <= 8 }
          cells:(##(size * 8))
          roots:(##(size * 8)) { roots >= 1 }
          absent:(##(size * 8)) { roots + absent <= cells }
          tot_cells_size:(##(off_bytes * 8))
          root_list:(roots * ##(size * 8))
          index:has_idx?(cells * ##(off_bytes * 8))
          cell_data:(tot_cells_size * [ uint8 ])
          crc32c:has_crc32c?uint32
         = BagOfCells;
    */
    public int[] toBoc() {
        return toBoc(true, false, false);
    }

    public int[] toBoc(boolean withCRC) {
        return toBoc(withCRC, false, false);
    }

    public int[] toBoc(boolean hasCrc32c, boolean hasIdx, boolean hasCacheBits) {
        // recursively go through cells, build hash index and store unique in slice
        List<Cell> orderCells = flattenIndex(List.of(this));

        int cellSizeBits = Utils.log2(orderCells.size() + 1);
        int cellSizeBytes = (int) Math.ceil((double) cellSizeBits / 8);

        List<Integer> payload = new ArrayList<>();
        for (Cell orderCell : orderCells) {
            payload.addAll(orderCell.serialize(cellSizeBytes));
        }
        // bytes needed to store len of payload
        int sizeBits = Utils.log2(payload.size() + 1);
        int sizeBytes = (int) Math.ceil((double) sizeBits / 8);

        List<BigInteger> rootList = new ArrayList<>();

        Boc boc = Boc.builder()
                .hasIdx(hasIdx)
                .hasCrc32c(hasCrc32c)
                .hasCacheBits(hasCacheBits)
                .flags(0)
                .size(cellSizeBytes)
                .offBytes(sizeBytes)
                .cells(orderCells.size())
                .roots(1)
                .absent(0)
                .totalCellsSize(payload.size())
                .rootList(List.of(BigInteger.ZERO))
                .index(List.of(BigInteger.ZERO)) // len in bytes of all cells, todo
                .cellData(payload.stream().mapToInt(Integer::intValue).toArray())
                .build();

        return boc.toCell().bits.toUnsignedByteArray();
    }

    private List<Cell> flattenIndex(List<Cell> src) {
        try {
            List<Cell> pending = src;
            Map<String, Cell> allCells = new HashMap<>();
            Map<String, Cell> notPermCells = new HashMap<>();

            Deque<String> sorted = new ArrayDeque<>();

            while (pending.size() > 0) {
                List<Cell> cells = new ArrayList<>(pending);
                pending = new ArrayList<>();

                for (Cell cell : cells) {
                    String hash = Utils.bytesToHex(cell.hash());
                    if (allCells.containsKey((String) hash)) {
                        continue;
                    }
                    notPermCells.put(hash, null);
                    allCells.put(hash, cell);

                    pending.addAll(cell.refs);
                }
            }

            Map<String, Boolean> tempMark = new HashMap<>();
            while (notPermCells.size() > 0) {
                for (String key : notPermCells.keySet()) {
                    visit(key, allCells, notPermCells, tempMark, sorted);
                    break;
                }
            }

            Map<String, Integer> indexes = new HashMap<>();

            Deque<String> tmpSorted = new ArrayDeque<>(sorted);
            int len = tmpSorted.size();
            for (int i = 0; i < len; i++) {
                indexes.put(tmpSorted.pop(), i);
            }
            int x = 0;
            for (String key : indexes.keySet()) {
                x++;
                if (x > 3) {
                    break;
                }
            }

            List<Cell> result = new ArrayList<>();
            for (String ent : sorted) {
                Cell rrr = allCells.get(ent);
                rrr.index = indexes.get(Utils.bytesToHex(rrr.hash()));
                for (Cell ref : rrr.refs) {
                    ref.index = indexes.get(Utils.bytesToHex(ref.hash()));
                }
                result.add(rrr);
            }

            return result;
        } catch (Exception e) {
            return null;
        }
    }

    private void visit(String hash, Map<String, Cell> allCells, Map<String, Cell> notPermCells, Map<String, Boolean> tempMark, Deque<String> sorted) {
        if (!notPermCells.containsKey(hash)) {
            return;
        }

        if (tempMark.containsKey(hash)) {
            System.err.println("Unknown branch, hash exists");
            return;
        }

        tempMark.put(hash, true);

        for (Cell ref : allCells.get(hash).refs) {
            visit(Utils.bytesToHex(ref.hash()), allCells, notPermCells, tempMark, sorted);
        }

        sorted.addFirst(hash);
        tempMark.remove(hash);
        notPermCells.remove(hash);
    }

    private List<Integer> serialize(int refIndexSzBytes) {
        int[] body = CellSlice.beginParse(this).loadSlice(this.bits.getLength());
        List<Integer> data = new ArrayList<>();
        Pair<Integer, Integer> descriptors = getDescriptors(this.levelMask);
        data.add(descriptors.getLeft());
        data.add(descriptors.getRight());

        data.addAll(Arrays.stream(body).boxed().toList());

        int unusedBits = 8 - (bits.getLength() % 8);

        if (unusedBits != 8) {
            data.set(2 + body.length - 1, data.get(2 + body.length - 1) + (1 << (unusedBits - 1)));
        }

        for (Cell ref : refs) {
            data.addAll(Arrays.stream(Utils.dynamicIntBytes(BigInteger.valueOf(ref.index), refIndexSzBytes)).boxed().toList());
        }

        return data;
    }

    private Pair<Integer, Integer> getDescriptors(LevelMask levelMask) {
        int ln = (bits.getLength() / 8) * 2;
        if (bits.getLength() % 8 != 0) {
            ln++;
        }

        byte specialBit = 0;
        if (this.special) {
            specialBit = 8;
        }

        return Pair.of(this.refs.size() + specialBit + levelMask.getMask() * 32, ln);
    }

    public CellType getCellType() {
        if (!special) {
            return CellType.ORDINARY;
        }

        if (bits.getLength() < 8) {
            return CellType.UNKNOWN;
        }

        BitString clonedBits = bits.clone();
        switch (clonedBits.readUint(8).intValue()) {
            case PRUNED_CELL_TYPE: {
                if (bits.getLength() >= 288) {
                    //int msk = clonedBits.readUint(8).intValue();
                    LevelMask msk = new LevelMask(clonedBits.readUint(8).intValue());
//                    byte msk = levelMask;
                    int lvl = msk.getLevel();
                    if ((lvl > 0) && (lvl <= 3) && (bits.getLength() >= 16 + (256 + 16) * msk.apply(lvl - 1).getHashIndex() + 1)) {
                        return CellType.PRUNED_BRANCH;
                    }
                }
            }
            case MERKLE_PROOF_CELL_TYPE: {
                if ((refs.size() == 1) && (bits.getLength() == 280)) {
                    return CellType.MERKLE_PROOF;
                }
            }
            case MERKLE_UPDATE_CELL_TYPE: {
                if ((refs.size() == 1) && (bits.getLength() == 552)) {
                    return CellType.MERKLE_UPDATE;
                }
            }
            case LIBRARY_CELL_TYPE: {
                if (bits.getLength() == (8 + 256)) {
                    return CellType.LIBRARY;
                }
            }
        }
        return CellType.UNKNOWN;
    }

}

