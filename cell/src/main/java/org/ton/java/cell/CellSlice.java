package org.ton.java.cell;


import org.ton.java.address.Address;
import org.ton.java.bitstring.BitString;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.util.Objects.nonNull;

public class CellSlice {

    BitString bits;
    List<Cell> refs;

    public CellType type;

    private CellSlice() {
    }

    private CellSlice(BitString bits, List<Cell> refs) {
        this.bits = bits.clone();
        this.refs = new ArrayList<>(refs);
    }

    private CellSlice(BitString bits, List<Cell> refs, CellType cellType) {
        this.bits = bits.clone();
        this.refs = new ArrayList<>(refs);
        this.type = cellType;
    }

    public boolean isExotic() {
        return this.type != CellType.ORDINARY;
    }

    public static CellSlice beginParse(Cell cell) {
        return new CellSlice(cell.getBits(), cell.refs, cell.type);
    }

    public static CellSlice beginParse(Object cell) {
        if (!((cell instanceof Cell) || (cell instanceof CellSlice))) {
            throw new Error("CellSlice works only with Cell types");
        }
        if ((cell instanceof Cell)) {
            return beginParse((Cell) cell);
        }
        return (CellSlice) cell;
    }

    public static CellSlice of(Object cell) {
        if (!((cell instanceof Cell) || (cell instanceof CellSlice))) {
            throw new Error("CellSlice works only with Cell types");
        }
        if ((cell instanceof Cell)) {
            return beginParse((Cell) cell);
        }
        return (CellSlice) cell;
    }


    public CellSlice clone() {
        return new CellSlice(this.bits, this.refs);
    }

    public Cell sliceToCell() {
        return new Cell(bits, refs);
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

    /**
     * Check whether slice was read to the end
     */
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

    /**
     * Loads the first reference from the slice.
     */
    public Cell loadRef() {

        checkRefsOverflow();
        Cell cell = refs.get(0);
        refs.remove(0);
        return cell;
    }

    public int getRefsCount() {
        return refs.size();
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

    public TonHashMap loadDict(int n, Function<BitString, Object> keyParser, Function<Cell, Object> valueParser) {
        TonHashMap x = new TonHashMap(n);
        x.deserialize(this, keyParser, valueParser);

        if (refs.size() != 0) {
            refs.remove(0);
        }
        if (refs.size() != 0) {
            refs.remove(0);
        }

        return x;
    }

    /**
     * Returns only value and extra of all edges, without extras of fork-nodes.
     */
    public TonHashMapAug loadDictAug(int n, Function<BitString, Object> keyParser, Function<CellSlice, Object> valueParser, Function<CellSlice, Object> extraParser) {
        TonHashMapAug x = new TonHashMapAug(n);
        x.deserialize(this, keyParser, valueParser, extraParser);

        if (refs.size() != 0) {
            refs.remove(0);
        }
        if (refs.size() != 0) {
            refs.remove(0);
        }

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

    /**
     * Returns only value and extra of all edges, without extras of fork-nodes.
     */
    public TonHashMapAugE loadDictAugE(int n, Function<BitString, Object> keyParser, Function<CellSlice, Object> valueParser, Function<CellSlice, Object> extraParser) {
        if (this.isExotic()) {
            return new TonHashMapAugE(n);
        }
        boolean isEmpty = !this.loadBit();
        if (isEmpty) {
            return new TonHashMapAugE(n);
        } else {
            TonHashMapAugE hashMap = new TonHashMapAugE(n);
            hashMap.deserialize(CellSlice.beginParse(this.loadRef()), keyParser, valueParser, extraParser);
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
            x.deserialize(CellSlice.beginParse(cs.loadRef()), keyParser, valueParser);
            return x;
        }
    }

    public CellSlice skipDictE() {
        boolean isEmpty = loadBit();
        return isEmpty ? skipRefs(1) : this;
    }

    /**
     * TODO - implement skip without traversing the actual hashmap
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
        return bits.get();
    }

    public boolean preloadBitAt(int position) {
        checkBitsOverflow(position);
        BitString cloned = clone().bits;
        cloned.readBits(position - 1);
        return cloned.readBit();
    }

    public int getFreeBits() {
        return bits.getFreeBits();
    }

    public int getRestBits() {
        return bits.getUsedBits();
    }

    public CellSlice skipBits(int length) {
        checkBitsOverflow(length);
        for (int i = 0; i < length; i++) {
            bits.readBit();
        }

        return this;
    }

    public CellSlice skipBit() {
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

    public int[] loadSlice(int length) {
        checkBitsOverflow(length);

        int leftLength = length;
        int unusedBits = 0;
        int l = bits.getFreeBits() % 8;

        if (l > 0 && bits.getLength() > 0) {
            unusedBits = 8 - (l % 8);
        }

        List<Integer> loadedData = new ArrayList<>();

        int oneMoreLeft = 0, oneMoreRight = 0;

        if (unusedBits > 0 && length > unusedBits) {
            oneMoreLeft = 1;
        }

        if ((length - unusedBits) % 8 != 0 || (length - unusedBits) == 0) {
            oneMoreRight = 1;
        }

        int ln = (length - unusedBits) / 8 + oneMoreLeft + oneMoreRight;

        int i = oneMoreLeft;

        while (leftLength > 0) {
            int b = 0;
            if (oneMoreLeft > 0) {
                b = bits.toByteArray()[i - 1] << (8 - unusedBits); // (byte)
                if (i < ln) {
                    b += bits.toByteArray()[i] >> unusedBits;
                }
            } else {
                b = bits.toByteArray()[i] & 0xFF;
                if (unusedBits > 0) {
                    b <<= (8 - unusedBits);
                }
            }

            if (leftLength < 8) {
                loadedData.add(b & 0xFF);
                break;
            }

            if (i < ln) {
                b &= 0xFF;
                loadedData.add(b);
            }

            leftLength -= 8;
            i++;
        }

        if (length > unusedBits) {
            int usedBytes = (length - unusedBits) / 8;
            if (unusedBits > 0) {
                usedBytes++;
            }
        }

        bits.readBits(length);

        return loadedData.stream().mapToInt(Integer::intValue).toArray();
    }

    public String loadString(int length) {
        checkBitsOverflow(length);
        BitString bitString = bits.readBits(length);
        return new String(bitString.toByteArray());
    }

    /**
     * Loads the very long string data,  from the rest of the cell and nested refs
     *
     * @return String
     */
    public String loadSnakeString() {
        List<Integer> result = new ArrayList<>();
        checkBitsOverflow(bits.getLength()); // bitsLeft
        CellSlice ref = this.clone();

        while (nonNull(ref)) {
            try {
                BitString bitString = ref.loadBits(ref.bits.getLength());
                result.addAll(Arrays.stream(bitString.toUintArray()).boxed().toList());

                if (ref.refs.size() > 1) {
                    throw new Error("more than one ref, it is not snake string");
                }

                if (ref.refs.size() == 1) {
                    ref = CellSlice.beginParse(ref.loadRef());
                    continue;
                }
            } catch (Throwable e) {
                return null;
            }
            ref = null;
        }

        return result.stream().map(m -> new String(new byte[]{m.byteValue()})).collect(Collectors.joining());
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
    public BigInteger loadVarUInteger(BigInteger bitLength) {
        BigInteger len = loadUint(bitLength.intValue());
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
        BigInteger len = loadUint(4);
        if (len.compareTo(BigInteger.ZERO) == 0) {
            return BigInteger.ZERO;
        } else {
            return loadUint(len.multiply(BigInteger.valueOf(8L)).intValue());
        }
    }

    public BigInteger preloadCoins() {
        BitString savedBits = bits.clone();
        try {
            BigInteger len = loadUint(4);
            if (len.compareTo(BigInteger.ZERO) == 0) {
                bits = savedBits;
                return BigInteger.ZERO;
            } else {
                BigInteger result = loadUint(len.multiply(BigInteger.valueOf(8L)).intValue());
                bits = savedBits;
                return result;
            }
        } catch (Throwable e) {
            bits = savedBits;
            throw e;
        }
    }

    public BigInteger skipCoins() {
        return loadCoins();
    }

    void checkBitsOverflow(int length) {
        if (length > bits.getUsedBits()) {
            throw new Error("Bits overflow. Can't load " + length + " bits. " + bits.getUsedBits() + " bits left.");
        }
    }

    void checkRefsOverflow() {
        if (refs.isEmpty()) {
            throw new Error("Refs overflow. No more refs.");
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
