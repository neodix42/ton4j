package org.ton.java.cell;

import org.ton.java.bitstring.BitString;
import org.ton.java.utils.Utils;

import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

public class Cell {
    private static final int[] reachBocMagicPrefix = Utils.hexToUnsignedBytes("B5EE9C72");
    private static final int HASH_SIZE = 32;
    private static final int DEPTH_SIZE = 2;

    public BitString bits;
    public List<Cell> refs;

    public boolean special;
    public byte levelMask;

    public Cell() {
        this.bits = new BitString();
        this.refs = new ArrayList<>();
        this.special = false;
        this.levelMask = 0;
    }

    public Cell(BitString bits, List<Cell> refs) {
        this.bits = new BitString(bits.length);
        this.bits.writeBitString(bits);
        this.refs = new ArrayList<>(refs);
        this.special = false;
        this.levelMask = 0;
    }

    public Cell(BitString bits, int bitSize, List<Cell> refs, boolean special, byte levelMask) {
        this.bits = new BitString(bitSize);
        this.bits.writeBitString(bits);
        this.refs = refs;
        this.special = special;
        this.levelMask = levelMask;
    }

    public static Cell fromBoc(String data) {
        return fromBocMultiRoot(Utils.hexToUnsignedBytes(data)).get(0);
    }

    public static Cell fromBoc(int[] data) {
        return fromBocMultiRoot(data).get(0);
    }

    public Cell clone() {
        Cell c = new Cell();
        c.bits = this.bits.clone();
        for (Cell refCell : this.refs) {
            c.refs.add(refCell.clone());
        }
        c.special = this.special;
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

    static List<Cell> fromBocMultiRoot(int[] data) {
        if (data.length < 10) {
            throw new Error("Invalid boc");
        }
        ByteReader r = new ByteReader(data);
        System.out.println("data " + Arrays.toString(data));
        if (!Utils.compareBytes(reachBocMagicPrefix, r.readBytes(4))) {
            throw new Error("Invalid boc magic header");
        }

        BocFlags bocFlags = parseBocFlags(r.readSignedByte());
        System.out.println("BocFlags.cellNumSizeBytes " + bocFlags.cellNumSizeBytes);
        System.out.println("BocFlags.hasIndex " + bocFlags.hasIndex);
        int dataSizeBytes = r.readByte(); // off_bytes:(## 8) { off_bytes <= 8 }

        long cellsNum = dynInt(r.readSignedBytes(bocFlags.cellNumSizeBytes)); // cells:(##(size * 8))
        long rootsNum = dynInt(r.readSignedBytes(bocFlags.cellNumSizeBytes)); // roots:(##(size * 8)) { roots >= 1 }

        System.out.println("cellsNum " + cellsNum + ", rootsNum " + rootsNum + ", dataSizeBytes " + dataSizeBytes);
        r.readBytes(bocFlags.cellNumSizeBytes);
        long dataLen = dynInt(r.readSignedBytes(dataSizeBytes));

        System.out.println("dataLen " + dataLen);

        if (bocFlags.hasCrc32c) {
            System.out.println("hasCrc32c");
            int[] bocWithoutCrc = Arrays.copyOfRange(data, 0, data.length - 4);
            int[] crcInBoc = Arrays.copyOfRange(data, data.length - 4, data.length);
            int[] crc32 = Utils.getCRC32ChecksumAsBytesReversed(bocWithoutCrc);
            if (!Utils.compareBytes(crc32, crcInBoc)) {
                throw new Error("Crc32c hashsum mismatch");
            }
        }

        // root_list:(roots * ##(size * 8))
        byte[] rootList = r.readSignedBytes(rootsNum * bocFlags.cellNumSizeBytes); // todo
        long rootIndex = dynInt(Arrays.copyOfRange(rootList, 0, bocFlags.cellNumSizeBytes));

        System.out.println("rootList " + Arrays.toString(rootList) + ", rootIndex " + rootIndex);

        if (bocFlags.hasCacheBits && !bocFlags.hasIndex) {
            throw new Error("cache flag cant be set without index flag");
        }

        int[] index = new int[0];
        int j = 0;
        if (bocFlags.hasIndex) {
            System.out.println("hasIndex");
            index = new int[(int) cellsNum];
            byte[] idxData = r.readSignedBytes(cellsNum * dataSizeBytes);
            int n = 0;
            for (int i = 0; i < cellsNum; i++) {
                int off = i * dataSizeBytes;
//                Utils.readNBytesFromArray(offsetBytes, idxData); //review
                int val = dynInt(Arrays.copyOfRange(idxData, off, off + dataSizeBytes));
                //System.out.println("val " + val);
                if (bocFlags.hasCacheBits) {
                    if (val % 2 == 1) {
                        n++;
                    }
                    val = val / 2;
                    index[j++] = val;
                }
            }
        }
        System.out.println("index " + Arrays.toString(index));

        int[] payload = r.readBytes(dataLen);

        System.out.println("payload " + Arrays.toString(payload));
        List<Cell> cells = parseCells(rootsNum, cellsNum, bocFlags.cellNumSizeBytes, payload, index);

        return cells;
    }

    private static List<Cell> parseCells(long rootsNum, long cellsNum, int refSzBytes, int[] data, int[] index) {
        Cell[] cells = new Cell[(int) cellsNum];
        for (int i = 0; i < cellsNum; i++) {
            cells[i] = new Cell();
        }
        boolean[] referred = new boolean[(int) cellsNum];

        System.out.println("cells " + Arrays.toString(cells) + ", referred " + Arrays.toString(referred) + ", size " + referred.length);

        int offset = 0;
        for (int i = 0; i < cellsNum; i++) {
            if ((data.length - offset) < 2) {
                throw new Error("failed to parse cell header, corrupted data");
            }

            System.out.println("indexNull " + index);

            if (nonNull(index) && (index.length != 0)) {
                System.out.println("index");
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
            byte levelMask = (byte) (flags >> 5);

            System.out.println("flags " + flags);

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
                System.out.println("withHashes");
                int maskBits = (int) Math.ceil(Math.log(levelMask + 1) / Math.log(2)); // todo review
                int hashesNum = maskBits + 1;
                offset += hashesNum * HASH_SIZE + hashesNum * DEPTH_SIZE;
            }

            int[] payload = Arrays.copyOfRange(data, offset, offset + sz);
            System.out.println("payload " + Arrays.toString(payload));

            offset += sz;
            System.out.println("offset " + offset);
            if ((data.length - offset) < (refsNum * refSzBytes)) {
                throw new Error("failed to parse cell refs, corrupted data");
            }

            int[] refsIndex = new int[refsNum];
            int x = 0;
//            List<Integer> refsIndex = new ArrayList<>();
            for (int j = 0; j < refsNum; j++) {
                int[] t = Arrays.copyOfRange(data, offset, offset + refSzBytes);
//                refsIndex[x++] = Utils.concatBytes(dynInt(refsIndex), refsIndex);
                refsIndex[x++] = dynInt(t);

                offset += refSzBytes;
            }

            System.out.println("refsIndex " + Arrays.toString(refsIndex));

            //Cell[] refs = new Cell[refsIndex.length];
            List<Cell> refs = new ArrayList<>();

            for (int y = 0; y < refsIndex.length; y++) {
                int id = refsIndex[y];
                System.out.println("y " + y + ", id " + id);
                if (i == id) {
                    throw new Error("recursive reference of cells");
                }

                if ((id < i) && (isNull(index))) {
                    throw new Error("reference to index which is behind parent cell");
                }

                if (id >= cells.length) {
                    throw new Error("invalid index, out of scope");
                }

//                refs[y] = cells[id];
                refs.add(cells[id]);

                referred[id] = true;
            }

            System.out.println("referred " + referred.length);


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

            System.out.println("bitSz " + bitSz);

//            Cell newCell = new Cell(new BitString(payload, bitSz), bitSz, refs, special, levelMask);
            cells[i].bits = new BitString(payload, bitSz);
            cells[i].refs = refs;
            cells[i].special = special;
            cells[i].levelMask = levelMask;

        }

        System.out.println("cells " + cells.length);

        List<Cell> roots = new ArrayList<>((int) rootsNum);

        // get cells which are not referenced by another, these are root cells
        for (int i = 0; i < referred.length; i++) {
            if (!referred[i]) {
                roots.add(cells[i]);
            }
        }

//        roots.add(cells.get(0));

        if (roots.size() != rootsNum) {
            throw new Error("roots num not match actual num");
        }

        return roots;
    }

    /**
     * // has_idx:(## 1) has_crc32c:(## 1)  has_cache_bits:(## 1) flags:(## 2) { flags = 0 } size:(## 3) { size <= 4 }
     *
     * @param data
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

    public static int dynInt(byte[] data) {
        byte[] tmp = new byte[8];
        System.arraycopy(data, 0, tmp, 8 - data.length, data.length);
        return new BigInteger(tmp).intValue();
    }

    public static int dynInt(int[] data) {
        int[] tmp = new int[8];
        System.arraycopy(data, 0, tmp, 8 - data.length, data.length);

        return Integer.valueOf(Utils.bytesToHex(tmp), 16);
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
    public void toFile(String filename, boolean hasIdx,
                       boolean hashCrc32,
                       boolean hasCacheBits,
                       int flags) {

        int[] boc = toBoc(hasIdx, hashCrc32, hasCacheBits, flags);
        try {
            Files.write(Paths.get(filename), Utils.unsignedBytesToSigned(boc));
        } catch (Exception e) {
            System.err.println("Cannot write to file. " + e.getMessage());
        }
    }

    public void toFile(String filename, boolean hasIdx) {
        toFile(filename, hasIdx, true, false, 0);
    }

    /**
     * default parameters: hashCrc32 true, hasCacheBits false, flags 0
     */
    public void toFile(boolean hasIdx) {
        String filename = System.currentTimeMillis() + ".boc";
        toFile(filename, hasIdx, true, false, 0);
    }

    /**
     * default parameters: hasIdx true, hashCrc32 true, hasCacheBits false, flags 0
     */
    public void toFile() {
        String filename = System.currentTimeMillis() + ".boc";
        toFile(filename, true, true, false, 0);
    }

    //serialized_boc#b5ee9c72 has_idx:(## 1) has_crc32c:(## 1)
    //  has_cache_bits:(## 1) flags:(## 2) { flags = 0 }
    //  size:(## 3) { size <= 4 }
    //  off_bytes:(## 8) { off_bytes <= 8 }
    //  cells:(##(size * 8))
    //  roots:(##(size * 8)) { roots >= 1 }
    //  absent:(##(size * 8)) { roots + absent <= cells }
    //  tot_cells_size:(##(off_bytes * 8))
    //  root_list:(roots * ##(size * 8))
    //  index:has_idx?(cells * ##(off_bytes * 8))
    //  cell_data:(tot_cells_size * [ uint8 ])
    //  crc32c:has_crc32c?uint32
    // = BagOfCells;

    /**
     * Convert Cell to BoC
     *
     * @param hasIdx       boolean, default true
     * @param hashCrc32    boolean, default true
     * @param hasCacheBits boolean, default false
     * @param flags        int, default 0
     * @return byte[]
     */
    public int[] toBoc(boolean hasIdx, boolean hashCrc32, boolean hasCacheBits, int flags) {

        Cell rootCell = this.clone();

        TreeWalkResult treeWalkResult = rootCell.treeWalk();

        List<TopologicalOrderArray> topologicalOrder = treeWalkResult.topologicalOrderArray;

        HashMap<String, Integer> cellsIndex = treeWalkResult.indexHashmap;

        BigInteger cellsNum = BigInteger.valueOf(topologicalOrder.size());

        // Minimal number of bits to represent reference (unused?)
        int sizeBits = cellsNum.toString(2).length();
        int sBytes = (int) Math.max(Math.ceil(sizeBits / (double) 8), 1);

        BigInteger fullSize = BigInteger.ZERO;
        ArrayList<BigInteger> sizeIndex = new ArrayList<>();

        for (TopologicalOrderArray cell_info : topologicalOrder) {
            sizeIndex.add(fullSize);
            fullSize = fullSize.add(BigInteger.valueOf(cell_info.cell.bocSerializationSize(cellsIndex)));
        }

        int offsetBits = fullSize.toString(2).length();
        byte offsetBytes = (byte) Math.max(Math.ceil(offsetBits / (double) 8), 1);

        BitString serialization = new BitString((1023 + 32 * 4 + 32 * 3) * topologicalOrder.size());
        serialization.writeBytes(reachBocMagicPrefix);
        serialization.writeBitArray(new boolean[]{hasIdx, hashCrc32, hasCacheBits});
        serialization.writeUint(BigInteger.valueOf(flags), 2);
        serialization.writeUint(BigInteger.valueOf(sBytes), 3);
        serialization.writeUint8(offsetBytes & 0xff);
        serialization.writeUint(cellsNum, sBytes * 8);
        serialization.writeUint(BigInteger.ONE, sBytes * 8); // One root for now
        serialization.writeUint(BigInteger.ZERO, sBytes * 8); // Complete BOCs only
        serialization.writeUint(fullSize, offsetBytes * 8);
        serialization.writeUint(BigInteger.ZERO, sBytes * 8); // Root should have index 0

        if (hasIdx) {
            for (int i = 0; i < topologicalOrder.size(); i++) {
                serialization.writeUint(sizeIndex.get(i), offsetBytes * 8);
            }
        }

        for (TopologicalOrderArray cell_info : topologicalOrder) {
            int[] refcell_ser = cell_info.cell.serializeForBoc(cellsIndex);
            serialization.writeBytes(refcell_ser);
        }

        int[] ser_arr = serialization.getTopUppedArray();

        if (hashCrc32) {
            int[] crc32 = Utils.getCRC32ChecksumAsBytesReversed(ser_arr);
            ser_arr = Utils.concatBytes(ser_arr, crc32);
        }

        return ser_arr;
    }


    public int[] toBoc(boolean hasIdx, boolean hashCrc32, boolean hasCacheBits) {
        return toBoc(hasIdx, hashCrc32, hasCacheBits, 0);
    }

    public int[] toBoc(boolean hasIdx, boolean hashCrc32) {
        return toBoc(hasIdx, hashCrc32, false, 0);
    }

    public int[] toBoc(boolean hasIdx) {
        return toBoc(hasIdx, true, false, 0);
    }

    public int[] toBoc() {
        return toBoc(true, true, false, 0);
    }

    public String toBocBase64() {
        return Utils.bytesToBase64(toBoc(true, true, false, 0));
    }

    public String toBocBase64(boolean hasIdx) {
        return Utils.bytesToBase64(toBoc(hasIdx, true, false, 0));
    }

    public String toBocBase64(boolean hasIdx, boolean hashCrc32) {
        return Utils.bytesToBase64(toBoc(hasIdx, hashCrc32, false, 0));
    }

    public String toBocBase64(boolean hasIdx, boolean hashCrc32, boolean hasCacheBits) {
        return Utils.bytesToBase64(toBoc(hasIdx, hashCrc32, hasCacheBits, 0));
    }

    /**
     * Convert Cell to BoC
     *
     * @param hasIdx       boolean, default true
     * @param hashCrc32    boolean, default true
     * @param hasCacheBits boolean, default false
     * @param flags        int, default 0
     * @return String in base64
     */
    public String toBocBase64(boolean hasIdx, boolean hashCrc32, boolean hasCacheBits, int flags) {
        return Utils.bytesToBase64(toBoc(hasIdx, hashCrc32, hasCacheBits, flags));
    }

    public String toHex(boolean hasIdx, boolean hashCrc32, boolean hasCacheBits, int flags) {
        return Utils.bytesToHex(toBoc(hasIdx, hashCrc32, hasCacheBits, flags));
    }


    public String toHex(boolean hasIdx, boolean hashCrc32, boolean hasCacheBits) {
        return Utils.bytesToHex(toBoc(hasIdx, hashCrc32, hasCacheBits, 0));
    }

    public String toHex(boolean hasIdx) {
        return Utils.bytesToHex(toBoc(hasIdx, true, false, 0));
    }

    public String toHex() {
        return Utils.bytesToHex(toBoc(true, true, false, 0));
    }

    public String toBase64() {
        return Utils.bytesToBase64(toBoc(true, true, false, 0));
    }

    public String toBase64(boolean hasIdx) {
        return Utils.bytesToBase64(toBoc(hasIdx, true, false, 0));
    }

    public String toBase64(boolean hasIdx, boolean hasCrc32, boolean hasCacheBits, int flags) {
        return Utils.bytesToBase64(toBoc(hasIdx, hasCrc32, hasCacheBits, flags));
    }


    /**
     * @return TreeWalkResult - topologicalOrderArray and indexHashmap
     */
    TreeWalkResult treeWalk() {
        return treeWalk(this, new ArrayList<>(), new HashMap<>(), null);
    }

    /**
     * @param cell                  Cell
     * @param topologicalOrderArray array of pairs: <byte[] cellHash, Cell Cell>
     * @param indexHashmap          cellHash: <String cellHash, Integer cellIndex>
     * @param parentHash            Uint8Array, default null, added neodiX
     * @return TreeWalkResult, topologicalOrderArray and indexHashmap
     */
    TreeWalkResult treeWalk(Cell cell, List<TopologicalOrderArray> topologicalOrderArray, HashMap<String, Integer> indexHashmap, String parentHash) {
        String cellHash = Utils.bytesToHex(cell.hash());
        if (indexHashmap.containsKey(cellHash)) {
            //if (cellHash in indexHashmap){ // Duplication cell
            //it is possible that already seen cell is a child of more deep cell
            if (nonNull(parentHash)) {
                if (indexHashmap.get(parentHash) > indexHashmap.get(cellHash)) {
                    moveToTheEnd(indexHashmap, topologicalOrderArray, cellHash);
                }
            }
            return new TreeWalkResult(topologicalOrderArray, indexHashmap);
        }
        indexHashmap.put(cellHash, topologicalOrderArray.size());
        topologicalOrderArray.add(new TopologicalOrderArray(cell.hash(), cell));

        for (Cell subCell : cell.refs) {
            TreeWalkResult res = treeWalk(subCell, topologicalOrderArray, indexHashmap, cellHash);
            topologicalOrderArray = res.topologicalOrderArray;
            indexHashmap = res.indexHashmap;
        }

        return new TreeWalkResult(topologicalOrderArray, indexHashmap);
    }

    void moveToTheEnd(HashMap<String, Integer> indexHashmap, List<TopologicalOrderArray> topologicalOrderArray, String target) {
        int targetIndex = indexHashmap.get(target);
        for (Map.Entry<String, Integer> h : indexHashmap.entrySet()) {
            if (indexHashmap.get(h.getKey()) > targetIndex) {
                indexHashmap.put(h.getKey(), indexHashmap.get(h.getKey()) - 1);
            }
        }
        indexHashmap.put(target, topologicalOrderArray.size() - 1);

        TopologicalOrderArray data = topologicalOrderArray.remove(targetIndex);

        topologicalOrderArray.add(data);

        for (Cell subCell : data.cell.refs) {
            moveToTheEnd(indexHashmap, topologicalOrderArray, Utils.bytesToHex(subCell.hash()));
        }
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


    int isExplicitlyStoredHashes() {
        return 0;
    }

    int[] getRefsDescriptor() {
        int[] d1 = new int[1];
        d1[0] = (refs.size() + ((special ? 1 : 0) * 8) + getMaxLevel() * 32);
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

    int[] serializeForBoc(HashMap<String, Integer> cellsIndex) {
        int[] reprArray = new int[0];

        reprArray = Utils.concatBytes(reprArray, getDataWithDescriptors());

        if (isExplicitlyStoredHashes() != 0) {
            throw new Error("Cell hashes explicit storing is not implemented");
        }
        for (Cell cell : refs) {
            BigInteger refIndexInt = BigInteger.valueOf(cellsIndex.get(Utils.bytesToHex(cell.hash())));
            String refIndexHex = refIndexInt.toString(16);
            if (refIndexHex.length() % 2 != 0) {
                refIndexHex = "0" + refIndexHex;
            }
            int[] reference = Utils.hexToUnsignedBytes(refIndexHex);
            reprArray = Utils.concatBytes(reprArray, reference);
        }
        int[] x = new int[0];
        x = Utils.concatBytes(x, reprArray);
        return x;
    }

    int bocSerializationSize(HashMap<String, Integer> cellsIndex) {
        return (serializeForBoc(cellsIndex)).length;
    }
}

