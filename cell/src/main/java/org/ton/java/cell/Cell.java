package org.ton.java.cell;

import org.apache.commons.codec.binary.Hex;
import org.ton.java.bitstring.BitString;
import org.ton.java.utils.Utils;

import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

public class Cell {
    private static final byte[] reachBocMagicPrefix = Utils.hexToBytes("B5EE9C72");
    private static final byte[] leanBocMagicPrefix = Utils.hexToBytes("68ff65f3");
    private static final byte[] leanBocMagicPrefixCRC = Utils.hexToBytes("acc3a728");

    public BitString bits;
    public List<Cell> refs;
    public List<Integer> refsInt;
    public boolean isExotic;
    public int readRefCursor;

    public Cell() {
        bits = new BitString(1023);
        refs = new ArrayList<>();
        refsInt = new ArrayList<>();
        isExotic = false;
        readRefCursor = 0;
    }

    public Cell(int cellSizeInBits) {
        bits = new BitString(cellSizeInBits);
        refs = new ArrayList<>();
        refsInt = new ArrayList<>();
        isExotic = false;
    }

    public Cell(BitString b, List<Cell> c, int r) {
        bits = b.clone();
        refs = new ArrayList<>(c);
        refsInt = new ArrayList<>(Collections.nCopies(r, 0));
    }

    public String toString() {
        return bits.toBitString();
    }

    public Cell clone() {
        Cell c = new Cell();
        c.bits = this.bits.clone();
        c.refs = new ArrayList<>(this.refs);
        c.isExotic = this.isExotic;
        c.readRefCursor = this.readRefCursor;
        c.refsInt = new ArrayList<>(Collections.nCopies(this.refs.size(), 0));
        return c;
    }

    /**
     * Loads bitString to Cell. Refs are not taken into account.
     *
     * @param hexBitString - bitString in hex
     * @return Cell
     */
    public static Cell fromHex(String hexBitString) {
        try {
            boolean incomplete = ((hexBitString.length() != 0) && (hexBitString.substring(hexBitString.length() - 1).equals("_")));

            hexBitString = hexBitString.replaceAll("_", "");
            byte[] b = Hex.decodeHex(hexBitString);

            BitString bs = new BitString(hexBitString.length() * 8);
            bs.writeBytes(b);

            boolean[] ba = bs.toBitArray();
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

            BitString f = new BitString(bss.toBitArray().length);
            f.writeBitString(bss);

            Cell c = new Cell();
            c.bits = f;
            return c;
        } catch (Exception e) {
            throw new Error("Cannot convert hex BitString to Cell. Error " + e.getMessage());
        }
    }

    /**
     * @param serializedBoc String in hex
     * @return List<Cell> root cells
     */
    public static Cell fromBoc(String serializedBoc) {
        return deserializeBoc(serializedBoc);
    }

    /**
     * @param serializedBoc byte[]
     * @return List<Cell> root cells
     */
    public static Cell fromBoc(byte[] serializedBoc) {
        return deserializeBoc(serializedBoc);
    }

    /**
     * Write another cell to this cell
     *
     * @param anotherCell Cell
     */
    public void writeCell(Cell anotherCell) {
        // XXX we do not check that there are enough place in cell
        bits.writeBitString(anotherCell.bits);
        refs.addAll(anotherCell.refs);
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

    byte[] getMaxDepthAsArray() {
        int maxDepth = getMaxDepth();
        byte[] d = new byte[2];
        d[1] = (byte) (maxDepth % 256);
        d[0] = (byte) Math.floor(maxDepth / (double) 256);
        return d;
    }

    int isExplicitlyStoredHashes() {
        return 0;
    }

    byte[] getRefsDescriptor() {
        byte[] d1 = new byte[1];
        d1[0] = (byte) (refs.size() + ((isExotic ? 1 : 0) * 8) + getMaxLevel() * 32);
        return d1;
    }

    byte[] getBitsDescriptor() {
        byte[] d2 = new byte[1];
        d2[0] = (byte) (Math.ceil(bits.writeCursor / (double) 8) + Math.floor(bits.writeCursor / (double) 8));
        return d2;
    }

    byte[] getDataWithDescriptors() {
        byte[] d1 = getRefsDescriptor();
        byte[] d2 = getBitsDescriptor();
        byte[] tuBits = bits.getTopUppedArray();
        return Utils.concatBytes(Utils.concatBytes(d1, d2), tuBits);
    }

    byte[] getRepr() {
        byte[] reprArray = new byte[0];

        reprArray = Utils.concatBytes(reprArray, getDataWithDescriptors());

        for (Cell cell : refs) {
            reprArray = Utils.concatBytes(reprArray, cell.getMaxDepthAsArray());
        }

        for (Cell cell : refs) {
            reprArray = Utils.concatBytes(reprArray, cell.hash());
        }

        byte[] x = new byte[0];
        x = Utils.concatBytes(x, reprArray);
        return x;
    }

    public byte[] hash() {
        byte[] repr = getRepr();
        return Utils.hexToBytes(Utils.sha256(repr));
    }

    /**
     * Recursively prints cell's content like Fift
     *
     * @return String
     */
    public String print(String indent) {
        StringBuilder s = new StringBuilder(indent + "x{" + bits.toHex() + "}\n");
        for (Cell i : refs) {
            s.append(i.print(indent + " "));
        }
        return s.toString();
    }

    public String print() {
        String indent = "";
        StringBuilder s = new StringBuilder(indent + "x{" + bits.toHex() + "}\n");
        for (Cell i : refs) {
            s.append(i.print(indent + " "));
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

        byte[] boc = toBoc(hasIdx, hashCrc32, hasCacheBits, flags);
        try {
            Files.write(Paths.get(filename), boc);
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
    public byte[] toBoc(boolean hasIdx, boolean hashCrc32, boolean hasCacheBits, int flags) {

        Cell rootCell = this;

        TreeWalkResult treeWalkResult = rootCell.treeWalk();

        List<TopologicalOrderArray> topologicalOrder = treeWalkResult.topologicalOrderArray;

        HashMap<String, Integer> cellsIndex = treeWalkResult.indexHashmap;

        BigInteger cellsNum = BigInteger.valueOf(topologicalOrder.size());

        int s = cellsNum.toString(2).length(); // Minimal number of bits to represent reference (unused?)
        int sBytes = (int) Math.min(Math.ceil(s / (double) 8), 1);

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
                serialization.writeUint(sizeIndex.get(i), offsetBytes * 8); //review
            }
        }

        for (TopologicalOrderArray cell_info : topologicalOrder) {
            byte[] refcell_ser = cell_info.cell.serializeForBoc(cellsIndex);
            serialization.writeBytes(refcell_ser);
        }

        byte[] ser_arr = serialization.getTopUppedArray();

        if (hashCrc32) {
            byte[] crc32 = Utils.getCRC32ChecksumAsBytesReversed(ser_arr);
            ser_arr = Utils.concatBytes(ser_arr, crc32);
        }

        return ser_arr;
    }


    public byte[] toBoc(boolean hasIdx, boolean hashCrc32, boolean hasCacheBits) {
        return toBoc(hasIdx, hashCrc32, hasCacheBits, 0);
    }

    public byte[] toBoc(boolean hasIdx, boolean hashCrc32) {
        return toBoc(hasIdx, hashCrc32, false, 0);
    }

    public byte[] toBoc(boolean hasIdx) {
        return toBoc(hasIdx, true, false, 0);
    }

    public byte[] toBoc() {
        return toBoc(true, true, false, 0);
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

    byte[] serializeForBoc(HashMap<String, Integer> cellsIndex) {
        byte[] reprArray = new byte[0];

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
            byte[] reference = Utils.hexToBytes(refIndexHex);
            reprArray = Utils.concatBytes(reprArray, reference);
        }
        byte[] x = new byte[0];
        x = Utils.concatBytes(x, reprArray);
        return x;
    }

    int bocSerializationSize(HashMap<String, Integer> cellsIndex) {
        return (serializeForBoc(cellsIndex)).length;
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
            //it is possible that already seen cell is a children of more deep cell
            if (parentHash != null) {
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

    public static BocHeader parseBocHeader(byte[] serializedBoc) {

        if (serializedBoc.length < 4 + 1) {
            throw new Error("Not enough bytes for magic prefix");
        }

        byte[] inputData = serializedBoc; // Save copy for crc32

        byte[] prefix = Arrays.copyOfRange(serializedBoc, 0, 4);
        serializedBoc = Arrays.copyOfRange(serializedBoc, 4, serializedBoc.length);

        int has_idx = 0; // boolean
        int hash_crc32 = 0; // boolean
        int has_cache_bits = 0;
        int flags = 0;
        int size_bytes = 0;

        if (Utils.compareBytes(prefix, reachBocMagicPrefix)) {
            int flagsByte = (serializedBoc[0] & 0xff);
            has_idx = (flagsByte & 128);
            hash_crc32 = (flagsByte & 64);
            has_cache_bits = (flagsByte & 32);
            flags = (flagsByte & 16) * 2 + (flagsByte & 8);
            size_bytes = flagsByte % 8;
        }
        if (Utils.compareBytes(prefix, leanBocMagicPrefix)) {
            has_idx = 1;
            hash_crc32 = 0;
            has_cache_bits = 0;
            flags = 0;
            size_bytes = serializedBoc[0];
        }
        if (Utils.compareBytes(prefix, leanBocMagicPrefixCRC)) {
            has_idx = 1;
            hash_crc32 = 1;
            has_cache_bits = 0;
            flags = 0;
            size_bytes = serializedBoc[0];
        }
        serializedBoc = Arrays.copyOfRange(serializedBoc, 1, serializedBoc.length);

        if (serializedBoc.length < 1 + 5 * size_bytes) {
            throw new Error("Not enough bytes for encoding cells counters");
        }
        int offsetBytes = serializedBoc[0] & 0xff; // test with address to bytes and fromBoc

        serializedBoc = Arrays.copyOfRange(serializedBoc, 1, serializedBoc.length);
        int cellsNum = Utils.readNBytesFromArray(size_bytes, serializedBoc);

        serializedBoc = Arrays.copyOfRange(serializedBoc, size_bytes, serializedBoc.length);
        int rootsNum = Utils.readNBytesFromArray(size_bytes, serializedBoc);

        serializedBoc = Arrays.copyOfRange(serializedBoc, size_bytes, serializedBoc.length);
        int absentNum = Utils.readNBytesFromArray(size_bytes, serializedBoc);

        serializedBoc = Arrays.copyOfRange(serializedBoc, size_bytes, serializedBoc.length);
        int totCellsSize = Utils.readNBytesFromArray(offsetBytes, serializedBoc);

        serializedBoc = Arrays.copyOfRange(serializedBoc, offsetBytes, serializedBoc.length);

        if (serializedBoc.length < (rootsNum * size_bytes)) {
            throw new Error("Not enough bytes for encoding root cells hashes");
        }

        List<Integer> rootList = new ArrayList<>();
        for (int c = 0; c < rootsNum; c++) {
            rootList.add(Utils.readNBytesFromArray(size_bytes, serializedBoc)); //review
            serializedBoc = Arrays.copyOfRange(serializedBoc, size_bytes, serializedBoc.length);
        }

        int[] index = new int[cellsNum];
        if (has_idx != 0) {
            if (serializedBoc.length < offsetBytes * cellsNum) {
                throw new Error("Not enough bytes for index encoding");
            }
            for (int c = 0; c < cellsNum; c++) {
                index[c] = Utils.readNBytesFromArray(offsetBytes, serializedBoc); //review
                serializedBoc = Arrays.copyOfRange(serializedBoc, offsetBytes, serializedBoc.length);
            }
        }

        if (serializedBoc.length < totCellsSize) {
            throw new Error("Not enough bytes for cells data");
        }
        byte[] cellsData = Arrays.copyOfRange(serializedBoc, 0, totCellsSize);
        serializedBoc = Arrays.copyOfRange(serializedBoc, totCellsSize, serializedBoc.length);

        if (hash_crc32 != 0) {
            if (serializedBoc.length < 4) {
                throw new Error("Not enough bytes for crc32c hashsum");
            }
            int length = inputData.length;
            byte[] bocWithoutCrc = Arrays.copyOfRange(inputData, 0, length - 4);
            byte[] crcInBoc = Arrays.copyOfRange(serializedBoc, 0, 4);

            byte[] crc32 = Utils.getCRC32ChecksumAsBytesReversed(bocWithoutCrc);

            if (!Utils.compareBytes(crc32, crcInBoc)) {
                throw new Error("Crc32c hashsum mismatch");
            }

            serializedBoc = Arrays.copyOfRange(serializedBoc, 4, serializedBoc.length);
        }

        if (serializedBoc.length != 0) {
            throw new Error("Too many bytes in BoC serialization");
        }

        BocHeader bocHeader = new BocHeader();
        bocHeader.has_idx = has_idx;
        bocHeader.hash_crc32 = hash_crc32;
        bocHeader.has_cache_bits = has_cache_bits;
        bocHeader.flags = flags;
        bocHeader.size_bytes = size_bytes;
        bocHeader.off_bytes = offsetBytes;
        bocHeader.cells_num = cellsNum;
        bocHeader.roots_num = rootsNum;
        bocHeader.absent_num = absentNum;
        bocHeader.tot_cells_size = totCellsSize;
        bocHeader.root_list = rootList;
        bocHeader.index = index;
        bocHeader.cells_data = cellsData;

        return bocHeader;
    }

    static DeserializeCellDataResult deserializeCellData(byte[] cellData, int referenceIndexSize) {
        if (cellData.length < 2) {
            throw new Error("Not enough bytes to encode cell descriptors");
        }
        int d1 = (cellData[0] & 0xff);
        int d2 = (cellData[1] & 0xff);
        cellData = Arrays.copyOfRange(cellData, 2, cellData.length);
        boolean isExotic = (byte) (d1 & 8) != 0; //review
        int refNum = d1 % 8;
        int dataBytesize = (int) Math.ceil(d2 / (double) 2);
        boolean fullfilledBytes = ((d2 % 2) == 0);

        Cell cell = new Cell();
        cell.isExotic = isExotic;
        if (cellData.length < dataBytesize + referenceIndexSize * refNum) {
            throw new Error("Not enough bytes to encode cell data");
        }

        cell.bits.setTopUppedArray(Arrays.copyOfRange(cellData, 0, dataBytesize), fullfilledBytes);

        cellData = Arrays.copyOfRange(cellData, dataBytesize, cellData.length);

        for (int r = 0; r < refNum; r++) {
            cell.refsInt.add(Utils.readNBytesFromArray(referenceIndexSize, cellData));
            cell.refs.add(null); //increase refs counter
            cellData = Arrays.copyOfRange(cellData, referenceIndexSize, cellData.length);
        }
        return new DeserializeCellDataResult(cell, cellData);
    }

    /**
     * @param serializedBoc String hex
     * @return List<Cell> root cells
     */
    private static Cell deserializeBoc(String serializedBoc) {
        return deserializeBoc(Utils.hexToBytes(serializedBoc));
    }

    /**
     * @param serializedBoc byte[] bytearray
     * @return List<Cell> root cells
     */
    public static Cell deserializeBoc(byte[] serializedBoc) {

        BocHeader header = parseBocHeader(serializedBoc);

        byte[] cellsData = header.cells_data;
        List<Cell> cellsArray = new ArrayList<>();

        for (int ci = 0; ci < header.cells_num; ci++) {
            DeserializeCellDataResult dd = deserializeCellData(cellsData, header.size_bytes);
            cellsData = dd.cellsData;
            cellsArray.add(dd.cell);
        }

        for (int ci = header.cells_num - 1; ci >= 0; ci--) {
            Cell c = cellsArray.get(ci);
            for (int ri = 0; ri < c.refsInt.size(); ri++) {
                int r = c.refsInt.get(ri);
                if (r < ci) {
                    throw new Error("Topological order is broken");
                }
                c.refs.set(ri, cellsArray.get(r));
            }
        }

        List<Cell> root_cells = new ArrayList<>();
        for (int ri : header.root_list) {
            root_cells.add(cellsArray.get(ri));
        }
        return root_cells.get(0); //multiple roots not supported yet
    }
}
