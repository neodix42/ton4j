package org.ton.java.cell;

import org.apache.commons.lang3.StringUtils;
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
import static org.ton.java.cell.CellType.ORDINARY;
import static org.ton.java.cell.CellType.UNKNOWN;

/**
 * Implements Cell class, where BitString having elements of Boolean type.
 * Later will be supporting BitString where each element will be stored as one bit in memory.
 */
public class Cell {

    public static final int ORDINARY_CELL_TYPE = -0x01;
    public static final int PRUNED_CELL_TYPE = 0x01;
    public static final int LIBRARY_CELL_TYPE = 0x02;
    public static final int MERKLE_PROOF_CELL_TYPE = 0x03;
    public static final int MERKLE_UPDATE_CELL_TYPE = 0x04;
    public static final int UNKNOWN_CELL_TYPE = 0xFF;

    public BitString bits;
    public List<Cell> refs = new ArrayList<>();

    public CellType type;
    private int[] refsIndexes;

    public int index;
    public boolean special;
    public LevelMask levelMask;

    public List<String> hashes = new ArrayList<>(); // todo private
    public List<Integer> depths = new ArrayList<>(); // todo private

//    private byte[] descriptors;
//    private byte[] dataBytes;


    public Cell() {
        this.bits = new BitString();
        this.special = false;
        this.type = ORDINARY;
        this.levelMask = new LevelMask(0);
//        descriptors = getDescriptors(levelMask.getMask());
//        dataBytes = getDataBytes();
//        calculateHashes();
    }

    public Cell(int bitSize) {
        this.bits = new BitString(bitSize);
        this.special = false;
        this.type = ORDINARY;
        this.levelMask = resolveMask();
//        descriptors = getDescriptors(levelMask.getMask());
//        dataBytes = getDataBytes();
//        calculateHashes();
    }

    public Cell(BitString bits, List<Cell> refs) {
        this.bits = new BitString(bits.getLength());
        this.bits.writeBitString(bits.clone());
        this.refs = new ArrayList<>(refs);
        this.special = false;
        this.type = ORDINARY;
        this.levelMask = new LevelMask(0);
//        descriptors = getDescriptors(levelMask.getMask());
//        dataBytes = getDataBytes();
//        calculateHashes();
    }

    public Cell(BitString bits, List<Cell> refs, int cellType) {
        this.bits = new BitString(bits.getLength());
        this.bits.writeBitString(bits.clone());
        this.refs = new ArrayList<>(refs);
        this.special = false;
        this.type = toCellType(cellType);
        this.levelMask = new LevelMask(0);
//        descriptors = getDescriptors(levelMask.getMask());
//        dataBytes = getDataBytes();
//        calculateHashes();
    }

    public Cell(BitString bits, int bitSize, List<Cell> refs, boolean special, LevelMask levelMask) {
        this.bits = new BitString(bitSize);
        this.bits.writeBitString(bits);
        this.refs = new ArrayList<>(refs);
        this.special = special;
        this.type = ORDINARY;
        this.levelMask = levelMask;
//        descriptors = getDescriptors(levelMask.getMask());
//        dataBytes = getDataBytes();
//        calculateHashes();
    }

    public Cell(BitString bits, int bitSize, List<Cell> refs, boolean special, CellType cellType) {
        this.bits = new BitString(bitSize);
        this.bits.writeBitString(bits);
        this.refs = new ArrayList<>(refs);
        this.special = special;
        this.type = cellType;
        this.levelMask = resolveMask();
//        descriptors = getDescriptors(levelMask.getMask());
//        dataBytes = getDataBytes();
//        calculateHashes();
    }

    public Cell(BitString bits, int[] refsIndexes, CellType cellType) {
        this.bits = new BitString(bits);
        this.refsIndexes = refsIndexes;
        this.type = cellType;
        this.levelMask = new LevelMask(0);
//        descriptors = getDescriptors(levelMask.getMask());
//        dataBytes = getDataBytes();
//        calculateHashes();
    }

    public Cell(BitString bits, int bitSize, List<Cell> refs, CellType cellType) {
        this.bits = new BitString(bitSize);
        this.bits.writeBitString(bits);
        this.refs = new ArrayList<>(refs);
        this.type = cellType;
        this.levelMask = resolveMask();
//        descriptors = getDescriptors(levelMask.getMask());
//        dataBytes = getDataBytes();
//        calculateHashes();
    }

    public static CellType toCellType(int cellType) {
        return switch (cellType) {
            case -1 -> ORDINARY;
            case 1 -> CellType.PRUNED_BRANCH;
            case 2 -> CellType.LIBRARY;
            case 3 -> CellType.MERKLE_PROOF;
            case 4 -> CellType.MERKLE_UPDATE;
            default -> UNKNOWN;
        };
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

    public void calculateHashes() {

        int totalHashCount = levelMask.getHashIndex() + 1;
        int hashCount = totalHashCount;
        if (type == CellType.PRUNED_BRANCH) {
            hashCount = 1;
        }
        int hashIndexOffset = totalHashCount - hashCount;
        int hashIndex = 0;
        int level = levelMask.getLevel();

        int off;

        for (int li = 0; li < level + 1; li++) {
            if (!levelMask.isSignificant(li)) {
                continue;
            }
            if (li < hashIndexOffset) {
                hashIndex++;
                continue;
            }
            byte[] dsc = getDescriptors(levelMask.apply(li).getLevel());

            byte[] hash = new byte[0];
            hash = Utils.concatBytes(hash, dsc);
            System.out.println(Utils.bytesToHex(dsc));
            System.out.println(Utils.sha256(hash));
            if (hashIndex == hashIndexOffset) {
                if ((li != 0) && (type == CellType.PRUNED_BRANCH)) {
                    throw new Error("neither pruned nor 0");
                }
                byte[] data = getDataBytes();
                System.out.println(Utils.bytesToHex(data));
                hash = Utils.concatBytes(hash, data);
                System.out.println(Utils.sha256(hash));
            } else {
                if ((li != 0) && (type == CellType.PRUNED_BRANCH)) {
                    throw new Error("neither pruned nor 0");
                }
                off = hashIndex - hashIndexOffset - 1;
                hash = Utils.concatBytes(hash, Utils.hexToSignedBytes(hashes.get(off)));
                System.out.println(Utils.sha256(hash));
            }
            int depth = 0;

            for (Cell r : refs) {
                int refDepth;
                if ((type == CellType.MERKLE_PROOF) || (type == CellType.MERKLE_UPDATE)) {
                    refDepth = r.getDepth(li + 1);
                } else {
                    refDepth = r.getDepth(li);
                }

                hash = Utils.concatBytes(hash, Utils.intToByteArray(refDepth));
                System.out.println(Utils.sha256(hash));
                if (refDepth > depth) {
                    depth = refDepth;
                }
            }
            if (refs.size() > 0) {
                depth++;
                if (depth >= 1024) {
                    throw new Error("depth is more than max depth (1023)");
                }
            }

            for (Cell r : refs) {
                if ((type == CellType.MERKLE_PROOF) || (type == CellType.MERKLE_UPDATE)) {
                    hash = Utils.concatBytes(hash, r.getHash(li + 1));
                    System.out.println(Utils.sha256(hash));
                } else {
                    hash = Utils.concatBytes(hash, r.getHash(li));
                    System.out.println(Utils.sha256(hash));
                }
            }
            off = hashIndex - hashIndexOffset;
            depths.add(depth);
            hashes.add(Utils.sha256(hash));
            hashIndex++;
        }
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
        return bits.toHex();
    }

    public int getBitLength() {
        return bits.toBitString().length();
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
        c.hashes = new ArrayList<>(this.hashes);
        c.depths = new ArrayList<>(this.depths);
//        c.dataBytes = Arrays.copyOf(this.dataBytes, this.dataBytes.length);
        //c.calculateHashes();
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

//            BitString f = new BitString(bss.getBitString().length());
//            f.writeBitString(bss);

            return CellBuilder.beginCell().storeBitString(bss).endCell();
        } catch (Exception e) {
            throw new Error("Cannot convert hex BitString to Cell. Error " + e.getMessage());
        }
    }

    /**
     * taken from pytoniq-core
     */
    static Pair<Cell, Integer> deserializeCell(byte[] data, int refIndexSize) {
        int dataLen = data.length;
        int refsDescriptor = data[0] & 0xFF;
        int level = refsDescriptor >> 5;
        int totalRefs = refsDescriptor & 7;
        boolean hasHashes = (refsDescriptor & 16) != 0;
        boolean isExotic = (refsDescriptor & 8) != 0;
        boolean isAbsent = (totalRefs == 7) && hasHashes;
        if (isAbsent) {
            throw new Error("Cannot deserialize absent cell");
        }
        int bitsDescriptor = data[1] & 0xFF;
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

        byte[] ret = Arrays.copyOfRange(data, i, i + dataSize);
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
            cellType = bits.preReadUint(8).intValue();
        }
        int[] cellRefsIndex = new int[totalRefs];

        for (int j = 0; j < totalRefs; j++) {
            cellRefsIndex[j] = Utils.bytesToIntX(Arrays.copyOfRange(data, i, i + refIndexSize));
            i += refIndexSize;
        }

        Cell c = new Cell(bits, cellRefsIndex, toCellType(cellType));
        return Pair.of(c, i);

    }

    static List<Cell> fromBocMultiRoot(byte[] data) {
        if (data.length < 10) {
            throw new Error("Invalid boc");
        }
        Boc boc = deserializeBocHeader(data);
        Cell[] cells = new Cell[boc.getCells()];
        int i = 0;
        for (int x = 0; x < boc.getCells(); x++) {
            byte[] ret = Arrays.copyOfRange(boc.getCellData(), i, boc.getCellData().length);
            Pair<Cell, Integer> ci = deserializeCell(ret, boc.getSize());
            i += ci.getRight();
            cells[x] = ci.getLeft();
        }

        System.out.println("size " + cells.length);

        for (int ci = boc.getCells() - 1; ci >= 0; ci--) {
            System.out.println("ci  " + ci + ", " + cells[ci].bits.toBitString());
            Cell c = cells[ci];
            List<Cell> refs = new ArrayList<>();

            for (int ri = 0; ri < c.refsIndexes.length; ri++) {
                int r = (c.refsIndexes[ri] < 0) ? c.refsIndexes[ri] & 0xFF : c.refsIndexes[ri];
                if (r < ci) {
                    throw new Error("Topological order is broken");
                }
                refs.add(cells[r]);
            }

            cells[ci] = new Cell(cells[ci].bits, refs, cells[ci].type.getValue());
            cells[ci].calculateHashes();
        }

        System.out.println("size " + cells.length);
        List<Cell> rootCells = new ArrayList<>();
        for (int ri = 0; ri < boc.getRootList().size(); ri++) {
            rootCells.add(cells[ri]);
        }
        System.out.println("rootCells " + rootCells.size());
        return rootCells;
    }

    private static Boc deserializeBocHeader(byte[] data) {
        Cell rawCell = CellBuilder.beginCell(data.length * 8).storeBytes(data).endCell();
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

        byte[] boc = toBoc(withCrc);
        try {
            Files.write(Paths.get(filename), boc);
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

    /**
     * BoC to hex
     *
     * @return
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

    public String toBase64(boolean withCrc) {
        return Utils.bytesToBase64(toBoc(withCrc));
    }

    public byte[] hash() {
        return getHash();
    }

    public byte[] getHash() {
        return getHash(levelMask.getLevel());
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
        return Utils.hexToSignedBytes(hashes.get(hashIndex));
    }

    byte[] getRefsDescriptor(int lvl) {
        byte[] d1 = new byte[1];
        d1[0] = (byte) (isNull(refs) ? 0 : refs.size() + ((special ? 1 : 0) * 8) + lvl * 32);
        return d1;
    }

    byte[] getBitsDescriptor() {
        int bitsLength = bits.getLength();
        byte d3 = (byte) (Math.floor((double) bitsLength / 8) * 2);
        if ((bitsLength % 8) != 0) {
            d3++;
        }
        return new byte[]{d3};
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
    public byte[] toBoc() {
        return toBoc(true, false, false);
    }

    public byte[] toBoc(boolean withCRC) {
        return toBoc(withCRC, false, false);
    }

    public byte[] toBoc(boolean hasCrc32c, boolean hasIdx, boolean hasCacheBits) {
        // recursively go through cells, build hash index and store unique in slice
        List<Cell> orderCells = flattenIndex(List.of(this.clone()));

        int cellSizeBits = Utils.log2(orderCells.size() + 1);
        int cellSizeBytes = (int) Math.ceil((double) cellSizeBits / 8);

        byte[] payload = new byte[0];
        for (Cell orderCell : orderCells) {
            payload = Utils.concatBytes(payload, orderCell.serialize(cellSizeBytes));
        }
        // bytes needed to store len of payload
        int sizeBits = Utils.log2(payload.length + 1);
        int sizeBytes = (int) Math.ceil((double) sizeBits / 8);

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
                .totalCellsSize(payload.length)
                .rootList(List.of(BigInteger.ZERO))
                .index(List.of(BigInteger.ZERO)) // len in bytes of all cells, todo
                .cellData(payload)
                .build();

        return boc.toCell().bits.toByteArray();
    }

    private List<Cell> flattenIndex(List<Cell> src) {

        List<Cell> pending = src;
        Map<String, Cell> allCells = new HashMap<>();
        Map<String, Cell> notPermCells = new HashMap<>();

        Deque<String> sorted = new ArrayDeque<>();

        while (pending.size() > 0) {
            List<Cell> cells = new ArrayList<>(pending);
            pending = new ArrayList<>();

            for (Cell cell : cells) {
                String hash = Utils.bytesToHex(cell.hash());
                if (allCells.containsKey(hash)) {
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
        for (String ignored : indexes.keySet()) {
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

    private byte[] serialize(int refIndexSzBytes) {
        byte[] body = Utils.unsignedBytesToSigned(CellSlice.beginParse(this).loadSlice(this.bits.getLength()));
        byte[] data;
        byte[] descriptors = getDescriptors(levelMask.getMask());
        data = Utils.concatBytes(descriptors, body);
//        data.add(descriptors[0]);
//        data.add(descriptors[1]);
//
//        data.addAll(Arrays.stream(body).boxed().toList());

        int unusedBits = 8 - (bits.getLength() % 8);

        if (unusedBits != 8) {
//            data.set(2 + body.length - 1, data.get(2 + body.length - 1) + (1 << (unusedBits - 1)));
            data[2 + body.length - 1] = (byte) (data[2 + body.length - 1] + (1 << (unusedBits - 1)));
        }

        for (Cell ref : refs) {
            //data.addAll(Arrays.stream(Utils.dynamicIntBytes(BigInteger.valueOf(ref.index), refIndexSzBytes)).boxed().toList());
            data = Utils.concatBytes(data, Utils.dynamicIntBytes(BigInteger.valueOf(ref.index), refIndexSzBytes));

        }

//        return Arrays.stream(data).boxed().toList();
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
                int[] dst = new int[2];
                System.arraycopy(getDataBytes(), off, dst, 0, 2); // review
                return Utils.intsToInt(dst);
            }
        }
        return depths.get(hashIndex);
    }

    private byte[] getDataBytes() {
        if ((bits.getLength() % 8) > 0) {
            String s = bits.toBitString();
            s = s + "1";
            if ((s.length() % 8) > 0) {
                s = s + StringUtils.repeat("0", 8 - (s.length() % 8));
            }
            return Utils.bitStringToByteArray(s);
        } else {
            return bits.toByteArray();
        }
    }


    public CellType getCellType() {
        if (!special) {
            return ORDINARY;
        }

        if (bits.getLength() < 8) {
            return UNKNOWN;
        }

        BitString clonedBits = bits.clone();
        switch (clonedBits.readUint(8).intValue()) {
            case ORDINARY_CELL_TYPE: {
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
        return UNKNOWN;
    }
}

