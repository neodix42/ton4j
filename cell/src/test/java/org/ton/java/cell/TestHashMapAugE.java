package org.ton.java.cell;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.ton.java.address.Address;
import org.ton.java.bitstring.BitString;
import org.ton.java.tlb.loader.Tlb;
import org.ton.java.tlb.types.ImportFees;
import org.ton.java.tlb.types.InMsgDescr;

import java.math.BigInteger;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
@RunWith(JUnit4.class)
public class TestHashMapAugE {

    @Test
    public void testEmptyHashMapESerialization() {
        int dictKeySize = 9;
        TonHashMapE x = new TonHashMapE(dictKeySize);

        Cell cell = x.serialize(
                k -> CellBuilder.beginCell().storeUint((Long) k, dictKeySize).bits,
                v -> CellBuilder.beginCell().storeUint((byte) v, 3)
        );
        log.info("cell {}", cell.print());
    }

    @Test
    public void testEmptyHashMapEDeserialization() {
        int dictKeySize = 9;
        TonHashMapE x = new TonHashMapE(dictKeySize);

        Cell dict = x.serialize(
                k -> CellBuilder.beginCell().storeUint((Long) k, dictKeySize).bits,
                v -> CellBuilder.beginCell().storeUint((byte) v, 3)
        );
        log.info("dict {}", dict.print());

        Cell cellDict = CellBuilder.beginCell()
                .storeDict(dict)
                .endCell();
        log.info("cell {}", cellDict.print());

        CellSlice cs = CellSlice.beginParse(cellDict);

        TonHashMap loadedDict = cs
                .loadDictE(dictKeySize,
                        k -> k.readUint(dictKeySize),
                        v -> CellSlice.beginParse(v).loadUint(3)
                );

        for (Map.Entry<Object, Object> entry : loadedDict.elements.entrySet()) {
            log.info("key {}, value {}", entry.getKey(), entry.getValue());
        }
    }

    @Test
    @Ignore // TODO fails HashMapE serialization with one entry
    public void testHashMapEDeserializationOneEntry() {
        int keySizeX = 9;
        TonHashMapE x = new TonHashMapE(keySizeX);

        x.elements.put(100L, (byte) 1);

        Cell cellX = x.serialize(
                k -> CellBuilder.beginCell().storeUint((Long) k, keySizeX).bits,
                v -> CellBuilder.beginCell().storeUint((byte) v, 3)
        );

        log.info("serialized cellX {}", cellX.print());
    }

    @Test
    public void testHashMapAugEDeserializationInMsgDescr() {

        String t = "b5ee9c724102300100078e0001099b2dcef0200102090d96e7781005020247bfeb2c6cdde302223a4667582853f778afd6739a9ac8bfbb0da1d2b3a9ccc8ab0b801840040303b57ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffef00000000000f42450000000000000000000000000000000000000000000000000000000000000000000000000000000065219c150003e665b94482c230d01064606002f02090d96e77810061202090d96e778100e070245bf50d1763b76220cf438cca5853eaf73c66af55c9efb00ff364c0bc93abad5a99c00610a0803af7333333333333333333333333333333333333333333333333333333333333333300000000000f424268cdef8878fb15c067852b78d9754b3c3f8b2fa4f8b394f438742f546e7e2c4a00000000000f424165219c1500014080b091e0082725ba1b155c295b8ebea99f2c912396a0d35faee4579badd0845a44cb9bbdc01fb82dd19fd36372d5ab8832ce7d130c62f61e1e99f3f526d8b50b88e1432c742c701064606000c0101a00c00ab69fe00000000000000000000000000000000000000000000000000000000000000013fccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccd1954fc4000000000000001e8480ca43382a40001f040901efe92001c0332dca2365b9de400251bf79785c8f89b1207602d53cbc9c7b6960ed74312a27b6c7842240c79e892ca8766cb73bc066cb73bd2b0f03af704f64c6afbff3dd10d8ba6707790ac9670d540f37a9448b0337baa6a5a92acac00000000000f4247263bd6928f98ae2cbc0f303d329c829bd393035f1be23be8d4ab2c3de1b8df1a00000000000f424165219c150001408252210020f0c0901c9c3801811111f009c402468bb8000000000000000000400000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000020350041a130245bf0e0e75e07e9214255394cd1448f7c64783bede963a648e7ef2a666a7fe473fac00c2191403af7000000000000000000000000000000000000000000000000000000000000000000000000000f4243d0bf5f557c1aed8251bffe420bef01d9bf30be7fc470ed01b39f3e69ec64fa5e00000000000f424165219c150001408181715020f0409017d78401811161f009a27c89c40000000000000000003000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000082728be9c5faf47ff46225c87911c45170c798e1742a43b3a2a81a927fe1d1a037d3686291e24b9a8bf03d7ce2190ae767de765d0e58b81de1a1d06fac6fc60f88ea0101a0210106460600210245bf27539cb77556f2ef3d09c97f7aa369ffea058435732dba38f67cdb4213079dfc00c2261b03af7000000000000000000000000000000000000000000000000000000000000000000000000000f42410000000000000000000000000000000000000000000000000000000000000000000000000000000065219c1500014081d241c0113040829a40cd41efffe02280101a027020f04091954fc401811201f005bc00000000000000000000000012d452da449e50b8cf7dd27861f146122afe1b546bb8b70fc8216f0c614139f8e04009e41778c0a604000000000000000003e0000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000ab29fe09ec98d5f7fe7ba21b174ce0ef21592ce1aa81e6f528916066f754d4b52559593fc0000000000000000000000000000000000000000000000000000000000000001017d784000000000000001e8484ca43382a40008272bf8229e6e684701825a6f8e1fb0f8155aba759d58b82e73b8405092874e23fa816617de1e9f3a79e4c7d8a3a51ac0c8b54b6bbcd4f824c9f800aa2f36fee267200827290aec8965afabb16ebc3cb9b408ebae71b618d78788bc80d09843593cac98da490aec8965afabb16ebc3cb9b408ebae71b618d78788bc80d09843593cac98da4008272085964ae4ba126108446f6e07ea348acb998ea5dc15c026c8fefbf72b79bd3278be9c5faf47ff46225c87911c45170c798e1742a43b3a2a81a927fe1d1a037d30101a02e01064606002701a369fe00000000000000000000000000000000000000000000000000000000000000013fc000000000000000000000000000000000000000000000000000000000000000020000000000001e8480ca43382a40280201202a290015bfffffffbcbd1a94a200100015be000003bcb3670dc15550010c46060365b9de2e0201e02f2d0101df2e00b959ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffdf3fc13d931abeffcf744362e99c1de42b259c35503cdea5122c0cdeea9a96a4ab2b101c9c380006cb73bc00000000001e848cca43382a7fffffffc000ab29fe09ec98d5f7fe7ba21b174ce0ef21592ce1aa81e6f528916066f754d4b52559593ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffbd01efe92000000000000001e8488ca43382a40edd09d3c";

        Cell cell = Cell.fromBoc(t);
        log.info("cell {}", cell.print());

        CellSlice cs = CellSlice.beginParse(cell);

        TonHashMapAugE loadedDict = cs.loadDictAugE(256,
                k -> k.readUint(256),
                v -> Pair.of(
                        Tlb.load(InMsgDescr.class, CellSlice.beginParse((Cell) v.getLeft()), false),
                        Tlb.load(ImportFees.class, CellSlice.beginParse((Cell) v.getLeft()), false))
        );

        // traverse deserialized hashmap
        log.info("Deserialized hashmap from cell");
        for (Map.Entry<Object, Pair<Cell, Cell>> entry : loadedDict.elements.entrySet()) {
            log.info("key {}, value {}", entry.getKey(), entry.getValue());
        }

        // serialize
        loadedDict.serialize(
                k -> CellBuilder.beginCell().storeUint((BigInteger) k, 256).bits,
                v -> Pair.of(
                        Tlb.load(InMsgDescr.class, CellSlice.beginParse((Cell) v.getLeft()), false),
                        Tlb.load(ImportFees.class, CellSlice.beginParse((Cell) v.getLeft()), false))
        );
    }

    @Test
    public void testHashMapAugEDeserializationOutMsgDescr() {

        String t = "b5ee9c72410241010008c7000109a06dac2c020102091036d616010c0202091017d784010903020b4405f5e1004007040203641006050343be4b1b3778c0888e9199d60a14fdde2bf59ce6a6b22feec36874acea73322ac2e0500b0f0a0243be63d3ca1239eca11fb1faf25f0f610fca88c7422ec4e0b1fe9e7d9dc942f790c010241a024bbf034855615853f3e8aed58c18425b4737968f60d775680c8703d31f01a620ab45017d784006081a0106460600260244bfa2eff9fc4848fb07ce54aa8d56f1c6a6ca4c1f8a5be46f5502785648f65ee63f00170f0201610b2e0106460600400209101efe9201280d020b5407bfa48040190e0343bf0e0e75e07e9214255394cd1448f7c64783bede963a648e7ef2a666a7fe473fac0a3a0f3303af704f64c6afbff3dd10d8ba6707790ac9670d540f37a9448b0337baa6a5a92acac00000000000f42410000000000000000000000000000000000000000000000000000000000000000000000000000000065219c15000740812111002052030241d1c008272f1f67337526ee9890e137d5255f3150400e63d15dfe04b26653d275f2d0a6b04bf8229e6e684701825a6f8e1fb0f8155aba759d58b82e73b8405092874e23fa8010160130201db151401014840020120181601012017008be7f827b26357dff9ee886c5d3383bc8564b386aa079bd4a245819bdd5352d495656240e8cae6e880eb79a200000000000f424365219c1500012195b1b1bcb081ddbdc9b190860101203b024bbf2921fcc47593c379a9b9edc6369e3b98a063b71db9bc8b8333241b4ce1ac125501efe92006271a03af704f64c6afbff3dd10d8ba6707790ac9670d540f37a9448b0337baa6a5a92acac00000000000f42483f1963df77a2c54d9afd49aa5fe07e84de131708952f7756f7c52271cf32802200000000000f424765219c1500074081f1e1b02053030241d1c0061c000000000000600000000000719ae84f17b8f8b22026a975ff55f1ab19fde4a768744d2178dfa63bb533e107a40d03c04009e42664e625a000000000000000000300000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000827216617de1e9f3a79e4c7d8a3a51ac0c8b54b6bbcd4f824c9f800aa2f36fee2672d4d2e789fd57af824d33228887af40bcfb492aebe39ec74d665726b6471454ab010160200201db22210101483c020120252301012024008be7f827b26357dff9ee886c5d3383bc8564b386aa079bd4a245819bdd5352d495656240e8cae6e880eb79a200000000000f424a65219c1500012195b1b1bcb081ddbdc9b190860101202600ab29fe09ec98d5f7fe7ba21b174ce0ef21592ce1aa81e6f528916066f754d4b52559593fc0000000000000000000000000000000000000000000000000000000000000001017d784000000000000001e8492ca43382a4001064606003c0344bf9cbc2e47c4d8903b016a9e5e4e3db4b076ba189513db63c2112063cf4496543b02312e29020766cb73bd312a03af704f64c6afbff3dd10d8ba6707790ac9670d540f37a9448b0337baa6a5a92acac00000000000f4247263bd6928f98ae2cbc0f303d329c829bd393035f1be23be8d4ab2c3de1b8df1a00000000000f424165219c150001408322d2b020f0c0901c9c38018112c36009c402468bb8000000000000000000400000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000008272bf8229e6e684701825a6f8e1fb0f8155aba759d58b82e73b8405092874e23fa816617de1e9f3a79e4c7d8a3a51ac0c8b54b6bbcd4f824c9f800aa2f36fee267203b57ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffef00000000000f42450000000000000000000000000000000000000000000000000000000000000000000000000000000065219c150003e665b94483d302f001f040901efe92001c0332dca2365b9de4000827290aec8965afabb16ebc3cb9b408ebae71b618d78788bc80d09843593cac98da490aec8965afabb16ebc3cb9b408ebae71b618d78788bc80d09843593cac98da4010c46060365b9de3f0101a03f0201613a3403af7000000000000000000000000000000000000000000000000000000000000000000000000000f4243d0bf5f557c1aed8251bffe420bef01d9bf30be7fc470ed01b39f3e69ec64fa5e00000000000f424165219c150001408393835020f0409017d784018113736005bc00000000000000000000000012d452da449e50b8cf7dd27861f146122afe1b546bb8b70fc8216f0c614139f8e04009a27c89c40000000000000000003000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000082728be9c5faf47ff46225c87911c45170c798e1742a43b3a2a81a927fe1d1a037d3686291e24b9a8bf03d7ce2190ae767de765d0e58b81de1a1d06fac6fc60f88ea0101a03b01064606003b00ab29fe09ec98d5f7fe7ba21b174ce0ef21592ce1aa81e6f528916066f754d4b52559593fc0000000000000000000000000000000000000000000000000000000000000001017d784000000000000001e8484ca43382a4000ab29fe09ec98d5f7fe7ba21b174ce0ef21592ce1aa81e6f528916066f754d4b52559593ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffbd01efe92000000000000001e8496ca43382a400201e0403e0101df3f00b959ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffdf3fc13d931abeffcf744362e99c1de42b259c35503cdea5122c0cdeea9a96a4ab2b101c9c380006cb73bc00000000001e848cca43382a7fffffffc000ab29fe09ec98d5f7fe7ba21b174ce0ef21592ce1aa81e6f528916066f754d4b52559593ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffbd01efe92000000000000001e8488ca43382a4079230714";

        Cell cell = Cell.fromBoc(t);
        log.info("cell {}", cell.print());

        CellSlice cs = CellSlice.beginParse(cell);

        TonHashMapAugE loadedDict = cs.loadDictAugE(256,
                k -> k.readUint(256),
                v -> Pair.of(v.getLeft(), v.getRight())
        );

        // traverse deserialized hashmap
        log.info("Deserialized hashmap from cell");
        for (Map.Entry<Object, Pair<Cell, Cell>> entry : loadedDict.elements.entrySet()) {
            log.info("key {}, value {}", entry.getKey(), entry.getValue());
        }

        // serialize
        loadedDict.serialize(
                k -> CellBuilder.beginCell().storeUint((BigInteger) k, 256).bits,
                v -> Pair.of(v.getLeft(), v.getRight())
        );
    }

    @Test
    public void testHashMapEDeserialization15() {
        int dictXKeySize = 15;
        TonHashMapE x = new TonHashMapE(dictXKeySize);

        x.elements.put(100L, CellBuilder.beginCell().storeUint(5, 5));
        x.elements.put(200L, CellBuilder.beginCell().storeUint(6, 5));
        x.elements.put(300L, CellBuilder.beginCell().storeUint(7, 5));

        Cell cellX = x.serialize(
                k -> CellBuilder.beginCell().storeUint((Long) k, dictXKeySize).bits,
                v -> CellBuilder.beginCell().storeSlice(CellSlice.beginParse((Cell) v))
        );

        log.info("serialized cellX:\n{}", cellX.print());
    }

    @Test
    public void testHashMapEDeserializationTwoDicts() {
        int dictXKeySize = 9;
        TonHashMapE x = new TonHashMapE(dictXKeySize);

        x.elements.put(100L, (byte) 1);
        x.elements.put(200L, (byte) 2);
        x.elements.put(300L, (byte) 3);

        Cell cellX = x.serialize(
                k -> CellBuilder.beginCell().storeUint((Long) k, dictXKeySize).bits,
                v -> CellBuilder.beginCell().storeUint((byte) v, 3)
        );

        log.info("serialized cellX:\n{}", cellX.print());

        int dictYKeySize = 64;
        TonHashMapE y = new TonHashMapE(dictYKeySize);

        y.elements.put(400L, (byte) 40);
        y.elements.put(300L, (byte) 30);
        y.elements.put(200L, (byte) 20);
        y.elements.put(100L, (byte) 10);

        Cell cellY = y.serialize(
                k -> CellBuilder.beginCell().storeUint((Long) k, dictYKeySize).bits,
                v -> CellBuilder.beginCell().storeUint((byte) v, 8)
        );
        log.info("serialized cellY:\n{}", cellY.print());

        Cell cell = CellBuilder.beginCell()
                .storeDict(cellX)
                .storeDict(cellY)
                .endCell();

        log.info("serialized cell (x+y):\n{}", cell.print());

        CellSlice cs = CellSlice.beginParse(cell);
        TonHashMap loadedDictX = cs
                .loadDictE(dictXKeySize,
                        k -> k.readUint(dictXKeySize),
                        v -> CellSlice.beginParse(v).loadUint(3)
                );

        int j = 1;
        log.info("Deserialized hashmap from loadedDictX");
        for (Map.Entry<Object, Object> entry : loadedDictX.elements.entrySet()) {
            log.info("key {}, value {}", entry.getKey(), entry.getValue());
            assertThat((BigInteger) entry.getKey()).isEqualTo(100 * j);
            assertThat((BigInteger) entry.getValue()).isEqualTo(j);
            j++;
        }

        TonHashMap loadedDictY = cs
                .loadDictE(dictYKeySize,
                        k -> k.readUint(dictYKeySize),
                        v -> CellSlice.beginParse(v).loadUint(8)
                );

        log.info("Deserialized hashmap from loadedDictY");

        for (Map.Entry<Object, Object> entry : loadedDictY.elements.entrySet()) {
            log.info("key {}, value {}", entry.getKey(), entry.getValue());
            assertThat((BigInteger) entry.getKey()).isEqualTo(100);
            assertThat((BigInteger) entry.getValue()).isEqualTo(10);
            break;

        }

        assertThat(loadedDictY.elements.size()).isEqualTo(4);
    }

    @Test
    public void testHashMapEDeserializationDictString() {
        TonHashMapE x = new TonHashMapE(40);

        // All keys must be of the same length
        // with el2 = "test" it fails, with el2 = "test " it runs
        String el1 = "test1";
        String el2 = "test2";
        String el3 = "test3";
        String el4 = "test4";

        x.elements.put(el1, (byte) 1);
        x.elements.put(el2, (byte) 2);
        x.elements.put(el3, (byte) 3);
        x.elements.put(el4, (byte) 3);

        Cell cellX = x.serialize(
                k -> CellBuilder.beginCell().storeString((String) k).bits,
                v -> CellBuilder.beginCell().storeUint((byte) v, 3)
        );

        log.info("serialized cellX:\n{}", cellX.print());

        Cell cell = CellBuilder.beginCell()
                .storeDict(cellX)
                .endCell();

        log.info("serialized cell:\n{}", cell.print());

        CellSlice cs = CellSlice.beginParse(cell);
        TonHashMap loadedDictX = cs
                .loadDictE(40,
                        k -> k.readString(40),
                        v -> CellSlice.beginParse(v).loadUint(3)
                );

        log.info("Deserialized hashmap from loadedDictX");
        for (Map.Entry<Object, Object> entry : loadedDictX.elements.entrySet()) {
            log.info("key {}, value {}", entry.getKey(), entry.getValue());
            assertThat((String) entry.getKey()).isEqualTo("test1");
            break;
        }

        assertThat(loadedDictX.elements.size()).isEqualTo(4);
    }

    @Test
    public void testHashMapEDeserializationDictBytes() {
        int dictKeySize = 40;
        TonHashMapE x = new TonHashMapE(dictKeySize);

        byte[] el1 = "test1".getBytes();
        byte[] el2 = "test2".getBytes();
        byte[] el3 = "test3".getBytes();
        byte[] el4 = "test4".getBytes();

        x.elements.put(el1, (byte) 1);
        x.elements.put(el2, (byte) 2);
        x.elements.put(el3, (byte) 3);
        x.elements.put(el4, (byte) 3);

        Cell cellX = x.serialize(
                k -> CellBuilder.beginCell().storeBytes((byte[]) k).bits,
                v -> CellBuilder.beginCell().storeUint((byte) v, 3)
        );

        log.info("serialized cellX:\n{}", cellX.print());

        Cell cell = CellBuilder.beginCell()
                .storeDict(cellX)
                .endCell();

        log.info("serialized cell:\n{}", cell.print());

        CellSlice cs = CellSlice.beginParse(cell);
        TonHashMap loadedDictX = cs
                .loadDictE(dictKeySize,
                        k -> k.readBytes(dictKeySize),
                        v -> CellSlice.beginParse(v).loadUint(3)
                );

        log.info("Deserialized hashmap from loadedDictX");
        for (Map.Entry<Object, Object> entry : loadedDictX.elements.entrySet()) {
            log.info("key {}, value {}", entry.getKey(), entry.getValue());
            assertThat((byte[]) entry.getKey()).isEqualTo("test1".getBytes());
            break;
        }

        assertThat(loadedDictX.elements.size()).isEqualTo(4);
    }

    @Test
    public void testHashMapEDeserializationDictBitString() {
        int dictKeySize = 267;
        TonHashMapE x = new TonHashMapE(dictKeySize);

        BitString bs1 = new BitString(dictKeySize);
        bs1.writeUint(100L, dictKeySize);

        BitString bs2 = new BitString(dictKeySize);
        bs2.writeUint(200L, dictKeySize);

        BitString bs3 = new BitString(dictKeySize);
        bs3.writeUint(300L, dictKeySize);

        x.elements.put(bs1, (byte) 1);
        x.elements.put(bs2, (byte) 2);
        x.elements.put(bs3, (byte) 3);

        Cell cellX = x.serialize(
                k -> CellBuilder.beginCell().storeBitString((BitString) k).bits,
                v -> CellBuilder.beginCell().storeUint((byte) v, 3)
        );

        log.info("serialized cellX:\n{}", cellX.print());

        Cell cell = CellBuilder.beginCell()
                .storeDict(cellX)
                .endCell();

        log.info("serialized cell:\n{}", cell.print());

        CellSlice cs = CellSlice.beginParse(cell);
        TonHashMap loadedDictX = cs
                .loadDictE(dictKeySize,
                        k -> k.readBits(dictKeySize),
                        v -> CellSlice.beginParse(v).loadUint(3)
                );

        log.info("Deserialized hashmap from loadedDictX");
        for (Map.Entry<Object, Object> entry : loadedDictX.elements.entrySet()) {
            log.info("key {}, value {}", entry.getKey(), entry.getValue());
            BigInteger res1 = (BigInteger) entry.getValue();
//            assertThat(res1.intValue()).isEqualTo(1);
//            break;
        }

        assertThat(loadedDictX.elements.size()).isEqualTo(3);
    }

    @Test
    public void testHashMapEDeserializationDictAddresses() {
        int dictKeySize = 267;
        TonHashMapE x = new TonHashMapE(dictKeySize);

        Address addr1 = Address.of("0:2cf55953e92efbeadab7ba725c3f93a0b23f842cbba72d7b8e6f510a70e422e3");
        Address addr2 = Address.of("0:0000000000000000000000000000000000000000000000000000000000000000");
        Address addr3 = Address.of("0:1111111111111111111111111111111111111111111111111111111111111111");
        Address addr4 = Address.of("0:3333333333333333333333333333333333333333333333333333333333333333");
        Address addr5 = Address.of("0:5555555555555555555555555555555555555555555555555555555555555555");
        x.elements.put(addr1, (byte) 1);
        x.elements.put(addr2, (byte) 2);
        x.elements.put(addr3, (byte) 3);
        x.elements.put(addr4, (byte) 4);
        x.elements.put(addr5, (byte) 5);

        Cell cellX = x.serialize(
                k -> CellBuilder.beginCell().storeAddress((Address) k).bits,
                v -> CellBuilder.beginCell().storeUint((byte) v, 3)
        );

        log.info("serialized cellX:\n{}", cellX.print());

        Cell cell = CellBuilder.beginCell()
                .storeDict(cellX)
                .endCell();

        log.info("serialized cell:\n{}", cell.print());

        CellSlice cs = CellSlice.beginParse(cell);
        TonHashMap loadedDictX = cs
                .loadDictE(dictKeySize,
                        k -> k.readAddress(),
                        v -> CellSlice.beginParse(v).loadUint(3)
                );

        assertThat(loadedDictX.elements.containsValue(new BigInteger("5"))).isTrue();

        assertThat(loadedDictX.elements.size()).isEqualTo(5);
    }
}