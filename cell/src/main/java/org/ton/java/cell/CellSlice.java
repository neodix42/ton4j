package org.ton.java.cell;


import org.ton.java.address.Address;
import org.ton.java.bitstring.BitString;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public class CellSlice {

    BitString bits;
    List<Cell> refs;

    private CellSlice() {
    }

    private CellSlice(BitString bits, List<Cell> refs) {
        this.bits = bits.clone();
        this.refs = new ArrayList<>(refs);
    }

    public static CellSlice beginParse(Cell cell) {
        return new CellSlice(cell.bits, cell.refs);
    }


    public CellSlice clone() {
        return new CellSlice(this.bits, this.refs);
    }

    public Cell sliceToCell() {
        return new Cell(bits, refs, refs.size());
    }

    public void endParse() {
        if (bits.readCursor != bits.getUsedBits()) {
            throw new Error("readCursor: " + bits.readCursor + " != bits.length: " + bits.getUsedBits());
        }
    }

    public Cell loadRefX() {
        return loadRef();
    }

    public Cell loadMaybeX() {
        boolean maybe = loadBit();
        if (!maybe) {
            return null;
        }
        return new Cell(bits, refs, refs.size());
    }

    public Cell loadMaybeRefX() { // test, loadMaybeRefX(slice, parser) {
        boolean maybe = loadBit();
        if (!maybe) {
            return null;
        }
        return loadRefX();
    }

    public Cell loadEither() { // test, loadEither(slice, parser_x, parser_y) {
        if (loadBit()) {
            return new Cell(bits, refs, refs.size()); // parser_x
        } else {
            return new Cell(bits, refs, refs.size()); //parser_y
        }
    }

    public Cell loadEitherXorRefX() {
        if (loadBit()) {
            return new Cell(bits, refs, refs.size());
        } else {
            return loadRefX();
        }
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

    /**
     * Check whether slice was read to the end
     */
    public boolean isSliceEmpty() {
        return bits.readCursor == bits.writeCursor;
    }

    public List<Cell> loadRefs(int count) {
        List<Cell> result = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            result.add(loadRef());
        }
        return result;
    }

    /**
     * Loads the first reference from the slice.
     */
    public Cell loadRef() {

        checkRefsOverflow();
        Cell cell = refs.get(0);
        refs.remove(0);
        return cell;
    }

    public CellSlice skipRefs(int length) {
        if (length > 0) {
            refs.subList(0, length).clear();
        }
        return this;
    }

    /**
     * Loads the reference from the slice at current position without moving refs cursor
     */
    public Cell preloadRef() {
        checkRefsOverflow();
        return refs.get(0);
    }

    public List<Cell> preloadRefs(int count) {
        List<Cell> result = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            result.add(refs.get(i));
        }
        return result;
    }

    public TonHashMap loadDict(int n, Function<BitString, Object> keyParser, Function<Cell, Object> valueParser) {
        TonHashMap x = new TonHashMap(n);
        x.deserialize(this, keyParser, valueParser);
        /*
        Cell c = this.sliceToCell();
        x.loadHashMapX2Y(c, keyParser, valueParser);

        bits = c.bits;
        refs = c.refs;
        */
        // move readRefCursor
        if (refs.size() != 0) {
            refs.remove(0);
        }
        if (refs.size() != 0) {
            refs.remove(0);
        }
//        for (int i = 0; i < this.bits.readref; i++) {
//            refs.remove(0);
//        }

        return x;
    }

    public TonHashMapE loadDictE(int n, Function<BitString, Object> keyParser, Function<Cell, Object> valueParser) {
        boolean isEmpty = !this.loadBit();
        if (isEmpty) {
            return new TonHashMapE(n);
        } else {
            TonHashMapE hashMap = new TonHashMapE(n);
            hashMap.deserialize(CellSlice.beginParse(this.loadRef()), keyParser, valueParser);
            return hashMap;
        }
    }

    public TonPfxHashMap loadDictPfx(int n, Function<BitString, Object> keyParser, Function<Cell, Object> valueParser) {
        TonPfxHashMap x = new TonPfxHashMap(n);
        x.deserialize(this, keyParser, valueParser);
        return x;
    }


    public TonPfxHashMapE loadDictPfxE(int n, Function<BitString, Object> keyParser, Function<Cell, Object> valueParser) {
        boolean isEmpty = !this.loadBit();
        if (isEmpty) {
            return new TonPfxHashMapE(n);
        } else {
            TonPfxHashMapE hashMap = new TonPfxHashMapE(n);
            hashMap.deserialize(CellSlice.beginParse(this.loadRef()), keyParser, valueParser);
            return hashMap;
        }
    }

    /**
     * Preloads dict (HashMap) without modifying the actual cell slice.
     *
     * @param n           - dict key size
     * @param keyParser   - key deserializor
     * @param valueParser - value deserializor
     * @return TonHashMap - dict
     */
    public TonHashMap preloadDict(int n, Function<BitString, Object> keyParser, Function<Cell, Object> valueParser) {
        TonHashMap x = new TonHashMap(n);
        x.deserialize(this.clone(), keyParser, valueParser);
        return x;
    }

    /**
     * Preloads dict (HashMapE) without modifying the actual cell slice.
     *
     * @param n           - dict key size
     * @param keyParser   - key deserializor
     * @param valueParser - value deserializor
     * @return TonHashMap - dict
     */
    public TonHashMap preloadDictE(int n, Function<BitString, Object> keyParser, Function<Cell, Object> valueParser) {
        boolean isEmpty = !this.preloadBit();
        if (isEmpty) {
            return new TonHashMap(n);
        } else {
            TonHashMap x = new TonHashMap(n);
            CellSlice cs = this.clone();
            cs.skipBit();
//            Cell c = this.sliceToCell();
//            c.loadBit();
//            Cell dict = c.readRef();
//            skipBit();
            x.deserialize(CellSlice.beginParse(cs.loadRef()), keyParser, valueParser);
            return x;
        }
    }

//    public TonHashMapE loadDictE(int n, Function<Cell, Object> keyParser, Function<Cell, Object> valueParser) {
//
//        TonHashMapE hashMap = new TonHashMapE(n);
//        Cell c = this.loadRef();
//        hashMap.loadHashMapX2Y(c, keyParser, valueParser);
//        return hashMap;
//    }

    public CellSlice skipDictE() {
        boolean isEmpty = loadBit();
        return isEmpty ? skipRefs(1) : this;
    }

    /**
     * TODO - skip without traversing the actual hashmap
     */
    public CellSlice skipDict(int dictKeySize) {
        loadDict(dictKeySize,
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
        return bits.prereadBit();
    }

    public CellSlice skipBits(int length) {
        checkBitsOverflow(length);
        bits.readCursor += length;
        return this;
    }

    public CellSlice skipBit() {
        checkBitsOverflow(1);
        bits.readCursor += 1;
        return this;
    }

    public CellSlice skipUint(int length) {
        return skipBits(length);
    }

    public CellSlice skipInt(int length) {
        return skipBits(length);
    }

    /**
     * @param length in bits
     * @return byte array
     */
    public byte[] loadBytes(int length) {
        checkBitsOverflow(length);
        BitString bitString = bits.readBits(length);
        return bitString.toByteArray();
    }

    public String loadString(int length) {
        checkBitsOverflow(length);
        BitString bitString = bits.readBits(length);
        return new String(bitString.toByteArray());
    }

    public BitString loadBits(int length) {
        checkBitsOverflow(length);
        return bits.readBits(length);
    }

    public BigInteger loadInt(int length) {
        return bits.readInt(length);
    }

    public BigInteger loadUint(int length) {
        checkBitsOverflow(length);
        if (length == 0) return BigInteger.ZERO;
        BitString i = loadBits(length);
        return new BigInteger(i.toBitString(), 2);
    }

    public BigInteger preloadInt(int bitLength) {
        BitString savedBits = bits.clone();
        try {
            BigInteger result = loadInt(bitLength);
            bits = savedBits;
            return result;
        } catch (Throwable e) {
            bits = savedBits;
            throw e;
        }
    }

    public BigInteger preloadUint(int bitLength) {
        BitString savedBits = bits.clone();
        try {
            BigInteger result = loadUint(bitLength);
            bits = savedBits;
            return result;
        } catch (Throwable e) {
            bits = savedBits;
            throw e;
        }
    }

    public BigInteger loadUintLEQ(BigInteger n) {
        BigInteger result = loadUint(n.bitLength());
        if (result.compareTo(n) > 0) {
            throw new Error("Cannot load {<= x}: encoded number is too high");
        }
        return result;
    }

    /**
     * Loads unsigned integer less than n by reading minimal number of bits encoding n-1
     * <p>
     * #<= p
     */
    public BigInteger loadUintLess(BigInteger n) {
        return loadUintLEQ(n.subtract(BigInteger.ONE));
    }

    /**
     * Loads VarUInteger
     * <p>
     * var_uint$_ {n:#} len:(#< n) value:(uint (len * 8)) = VarUInteger n;
     */
    public BigInteger loadVarUInteger(BigInteger n) {
        BigInteger len = loadUintLess(n);
        if (len.compareTo(BigInteger.ZERO) == 0) {
            return BigInteger.ZERO;
        } else {
            return loadUint(len.multiply(BigInteger.valueOf(8L)).intValue());
        }
    }

    /**
     * Loads coins amount
     * <p>
     * nanograms$_ amount:(VarUInteger 16) = Grams;
     */
    public BigInteger loadCoins() {
        return loadVarUInteger(BigInteger.valueOf(16L));
    }

    public BigInteger preloadCoins() {
        BitString savedBits = bits.clone();
        try {
            BigInteger result = loadVarUInteger(BigInteger.valueOf(16L));
            bits = savedBits;
            return result;
        } catch (Throwable e) {
            bits = savedBits;
            throw e;
        }
    }

    public BigInteger skipCoins() {
        return loadVarUInteger(BigInteger.valueOf(16L));
    }

    void checkBitsOverflow(int length) {
        if (bits.readCursor + length > bits.writeCursor) {
            throw new Error("Bits overflow. Can't load " + length + " bits. " + bits.getFreeBits() + " bits left.");
        }
    }

    void checkRefsOverflow() {
        if (refs.isEmpty()) {
            throw new Error("Refs overflow.");
        }
    }

    public String toString() {
        return bits.toBitString();
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

        String address = workchain + ":" + String.format("%64s", hashPart.toString(16)).replace(' ', '0');
        return Address.of(address);
    }
}
