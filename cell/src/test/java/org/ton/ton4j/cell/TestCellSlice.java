package org.ton.ton4j.cell;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.ton.ton4j.address.Address;
import org.ton.ton4j.bitstring.BitString;

@Slf4j
@RunWith(JUnit4.class)
public class TestCellSlice {

  @Test
  public void testCellSlice() {

    BitString bs0 = new BitString(10);

    bs0.writeUint(40, 8);

    Cell cRef0 = CellBuilder.beginCell().storeUint(1, 3).storeUint(100, 8).endCell();
    Cell cRef1 = CellBuilder.beginCell().storeUint(2, 3).storeUint(200, 8).endCell();

    Address addr = Address.of("0QAs9VlT6S776tq3unJcP5Ogsj-ELLunLXuOb1EKcOQi4-QO");

    Cell c0 =
        CellBuilder.beginCell()
            .storeUint(10, 8)
            .storeUint(20, 8)
            .storeInt(30, 8)
            .storeRef(cRef0)
            .storeRef(cRef1)
            .storeBitString(bs0)
            .storeAddress(addr)
            .endCell();

    log.info("CellType {}", c0.getCellType());

    CellSlice cs0 = CellSlice.beginParse(c0);
    assertThat(cs0.loadUint(8).longValue()).isEqualTo(10);
    assertThat(cs0.loadUint(8).longValue()).isEqualTo(20);
    assertThat(cs0.loadUint(8).longValue()).isEqualTo(30);
    CellSlice csRef0 = CellSlice.beginParse(cs0.loadRef());
    CellSlice csRef1 = CellSlice.beginParse(cs0.loadRef());
    assertThat(csRef0.loadUint(3)).isEqualTo(1);
    assertThat(csRef0.loadUint(8)).isEqualTo(100);
    assertThat(cs0.loadUint(8).longValue()).isEqualTo(40);
    assertThat(csRef1.loadUint(3)).isEqualTo(2);
    assertThat(csRef1.loadUint(8)).isEqualTo(200);
    assertThat(cs0.loadAddress().toString(false))
        .isEqualTo(
            Address.of("0:2cf55953e92efbeadab7ba725c3f93a0b23f842cbba72d7b8e6f510a70e422e3")
                .toString(false));
  }

  @Test
  public void testBitStringVarUint1() {

    for (int i = 3; i <= 18; i++) {
      CellBuilder cell = CellBuilder.beginCell().storeVarUint(BigInteger.valueOf(777), i);
      BigInteger loadedValue = CellSlice.beginParse(cell.endCell()).loadVarUInteger(i);
      log.info("loaded {}", loadedValue);
      if (loadedValue.intValue() != 777) {
        assertFalse(true);
      }
    }
  }

  @Test
  public void testBitStringVarUint2() {
    CellBuilder cell1 = CellBuilder.beginCell().storeVarUint(BigInteger.valueOf(10), 10);
    BigInteger loadedValue1 = CellSlice.beginParse(cell1.endCell()).loadVarUInteger(10);
    assertThat(loadedValue1).isEqualTo(BigInteger.valueOf(10));
  }

  @Test
  public void testCellSliceUintsFromBoc() {
    Cell c1 = CellBuilder.beginCell().fromBoc("b5ee9c72410101010003000001558501ef11").endCell();
    CellSlice cs = CellSlice.beginParse(c1);
    BigInteger i = cs.loadUint(7);
    assertThat(i.longValue()).isEqualTo(42);
    cs.endParse();

    Cell c2 =
        CellBuilder.beginCell()
            .fromBoc(
                "B5EE9C7241010101002200003F000000000000000000000000000000000000000000000000000000000000009352A2F27C")
            .endCell();
    CellSlice cs2 = CellSlice.beginParse(c2);
    BigInteger j = cs2.loadUint(255);
    assertThat(j.longValue()).isEqualTo(73);
    cs2.endParse();

    Cell c3 = Cell.fromHex("0000010100000000000000457C_");
    CellSlice cs3 = CellSlice.beginParse(c3);

    BigInteger x = cs3.loadUint(16);
    assertThat(x.longValue()).isEqualTo(0);

    x = cs3.loadUint(8);
    assertThat(x.longValue()).isEqualTo(1);

    x = cs3.loadUint(8);
    assertThat(x.longValue()).isEqualTo(1);

    x = cs3.loadUint(64);
    assertThat(x.longValue()).isEqualTo(69);

    x = cs3.loadUint(5);
    assertThat(x.longValue()).isEqualTo(15);

    cs3.endParse();
  }

  @Test
  public void testCellSliceIntsFromBoc() {
    Cell c4 =
        Cell.fromHex("0000FF880FFFFFFFFFFFFFFDDC_"); // 0000000000000000 11111111 10001 00000001
    // 1111111111111111111111111111111111111111111111111111111110111011 100

    CellSlice cs4 = CellSlice.beginParse(c4);

    BigInteger y = cs4.loadInt(16);
    assertThat(y.longValue()).isEqualTo(0);

    y = cs4.loadInt(8);
    assertThat(y.longValue()).isEqualTo(-1);

    y = cs4.loadInt(5);
    assertThat(y.longValue()).isEqualTo(-15);

    y = cs4.loadInt(8);
    assertThat(y.longValue()).isEqualTo(1);

    y = cs4.loadInt(64);
    assertThat(y.longValue()).isEqualTo(-69);

    cs4.endParse();
  }

  @Test
  public void testCellSliceEmpty() {
    Cell c1 = CellBuilder.beginCell().endCell();
    CellSlice cs = CellSlice.beginParse(c1);
    assertThat(cs.refs.size()).isZero();
    assertThat(cs.bits.getLength()).isEqualTo(1023);
    assertThat(cs.bits.getUsedBits()).isZero();
    assertThat(cs.bits.getFreeBits()).isEqualTo(1023);
    assertThat(cs.bits.getUsedBytes()).isZero();
    assertThat(cs.isSliceEmpty()).isTrue();
  }

  @Test
  public void testCellSliceSkipBits() {
    Cell c1 = CellBuilder.beginCell().storeBits(Arrays.asList(false, false, true, true)).endCell();
    CellSlice cs = CellSlice.beginParse(c1);
    BitString bs = cs.skipBits(2).loadBits(2);
    log.info("bs {}", bs.toBitString());
    assertThat(bs.toBitString()).isEqualTo("11");

    assertThrows(Error.class, () -> cs.skipBits(6).loadBits(2));
  }

  @Test
  public void testCellSliceSnakeString() {
    String result =
        "123456789_123456789_123456789_123456789_123456789_123456789_123456789_123456789_123456789_123456789_123456789_123456789_123456789_123456789_123456789_123456789_123456789_123456789_123456789";
    Cell c1 = CellBuilder.beginCell().storeUint(10, 32).storeSnakeString(result).endCell();
    CellSlice cs = CellSlice.beginParse(c1);
    BigInteger i = cs.loadUint(32);
    String s = cs.loadSnakeString();

    log.info("i {}, s {}", i, s);

    assertThat(s).isEqualTo(result);
  }

  @Test
  public void testCellSliceSnakeString2() {
    String result =
        "123456789_123456789_123456789_123456789_123456789_123456789_123456789_123456789_123456789_123456789_123456789_123456789_123456789_123456789_123456789_123456789_123456789_123456789_123456789";
    Cell c1 = CellBuilder.beginCell().storeSnakeString(result).endCell();
    CellSlice cs = CellSlice.beginParse(c1);
    String s = cs.loadSnakeString();

    log.info("s {}", s);

    assertThat(s).isEqualTo(result);
  }

  @Test
  public void testCellSliceLoadSlice() {
    Cell c1 = CellBuilder.beginCell().storeBits(Arrays.asList(false, false, true, true)).endCell();
    int[] slice = CellSlice.beginParse(c1).loadSlice(c1.bits.getUsedBits());
    log.info("slice {} hex {}", slice.length, Integer.toHexString(slice[0]));
  }

  @Test
  public void testCellSliceSkipBitsOverflow() {
    Cell c1 = CellBuilder.beginCell().storeBits(Arrays.asList(false, false, true, true)).endCell();

    assertThrows(Error.class, () -> CellSlice.beginParse(c1).skipBits(6).loadBits(2));

    assertThrows(
        Error.class,
        () -> {
          CellSlice cs = CellSlice.beginParse(c1);
          BitString bs = cs.loadBits(2);
          log.info(bs.toBitString());
          cs.skipBits(3).loadBits(2);
        });
  }

  @Test
  public void testCellSliceSkipRefs() {
    Cell ref1 = CellBuilder.beginCell().storeBits(Arrays.asList(false, true)).endCell();
    Cell ref2 = CellBuilder.beginCell().storeBits(Arrays.asList(true, false)).endCell();
    Cell c1 = CellBuilder.beginCell().storeRefs(Arrays.asList(ref1, ref2)).endCell();

    CellSlice cs1 = CellSlice.beginParse(c1);
    Cell cRef1 = cs1.loadRef();
    Cell cRef2 = cs1.loadRef();
    assertThat(cRef1.bits.toBitString()).isEqualTo("01");
    assertThat(cRef2.bits.toBitString()).isEqualTo("10");
    cs1.endParse();

    CellSlice cs2 = CellSlice.beginParse(c1);
    cs2.skipRefs(1);
    Cell secondRef = cs2.loadRef();
    assertThat(secondRef.bits.toBitString()).isEqualTo("10");

    assertThrows(Error.class, cs2::loadRef);

    assertThrows(Error.class, () -> CellSlice.beginParse(c1).skipRefs(2).loadRef());
  }

  //    @Test
  //    public void testCellSliceSkipDict() {
  //        int dictKeySize = 9;
  //        TonHashMap x = new TonHashMap(dictKeySize);
  //
  //        x.elements.put(100L, (byte) 1);
  //        x.elements.put(200L, (byte) 2);
  //
  //        Cell dict = x.serialize(
  //                k -> CellBuilder.beginCell().storeUint((Long) k,
  // dictKeySize).endCell().getBits(),
  //                v -> CellBuilder.beginCell().storeUint((byte) v, 3).endCell()
  //        );
  //
  //        Cell cellDict = CellBuilder.beginCell()
  //                .storeUint(10, 5)
  //                .storeDictInLine(dict)
  //                .storeRef(CellBuilder.beginCell().storeUint(7, 8).endCell())
  //                .endCell();
  //
  //        log.info(cellDict.print());
  //
  //        CellSlice cs = CellSlice.beginParse(cellDict);
  //        BigInteger i = cs.loadUint(5);
  //        log.info("i = {}", i);
  //        assertThat(i.longValue()).isEqualTo(10);
  //
  //        CellSlice t = cs.skipDict(dictKeySize);
  //        log.info(t.toString());
  //
  //        Cell cRef = cs.loadRef();
  //
  //        assertThat(CellSlice.beginParse(cRef).loadUint(8).longValue()).isEqualTo(7);
  //    }

  @Test
  public void testCellSliceSkipDictE() {
    int dictKeySize = 9;
    TonHashMap x = new TonHashMapE(dictKeySize);

    x.elements.put(100L, (byte) 1);
    x.elements.put(200L, (byte) 2);

    Cell dict =
        x.serialize(
            k -> CellBuilder.beginCell().storeUint((Long) k, dictKeySize).endCell().getBits(),
            v -> CellBuilder.beginCell().storeUint((byte) v, 3).endCell());

    Cell cellDict =
        CellBuilder.beginCell()
            .storeUint(10, 5)
            .storeDict(dict)
            .storeRef(CellBuilder.beginCell().storeUint(7, 8).endCell())
            .endCell();

    log.info(cellDict.print());

    CellSlice cs = CellSlice.beginParse(cellDict);
    BigInteger i = cs.loadUint(5);
    log.info("i = {}", i);
    assertThat(i.longValue()).isEqualTo(10);

    CellSlice t = cs.skipDictE();
    log.info(t.toString());

    Cell cRef = cs.loadRef();

    assertThat(CellSlice.beginParse(cRef).loadUint(8).longValue()).isEqualTo(7);
  }

  @Test
  public void testCellSliceEmptyDictLast() {
    TonHashMap x = new TonHashMap(9);

    assertThrows(
        Error.class,
        () ->
            x.serialize(
                k -> CellBuilder.beginCell().storeUint((Long) k, 9).endCell().getBits(),
                v -> CellBuilder.beginCell().storeUint((byte) v, 3).endCell()));
  }

  //    @Test
  //    public void testCellSliceWithCellWithDict() {
  //        int dictKeySize = 9;
  //        TonHashMap x = new TonHashMap(dictKeySize);
  //
  //        x.elements.put(100L, (byte) 1);
  //        x.elements.put(200L, (byte) 2);
  //
  //        Cell dictCell = x.serialize(
  //                k -> CellBuilder.beginCell().storeUint((Long) k,
  // dictKeySize).endCell().getBits(),
  //                v -> CellBuilder.beginCell().storeUint((byte) v, 3).endCell()
  //        );
  //
  //        log.info("cell dict bits length: {}", dictCell.bits.getUsedBits());
  //
  //        Cell cellWithDict = CellBuilder.beginCell()
  //                .storeUint(10, 5)
  //                .storeDictInLine(dictCell)
  //                .storeRef(CellBuilder.beginCell().storeUint(7, 8).endCell())
  //                .endCell();
  //
  //        log.info(cellWithDict.print());
  //
  //        CellSlice cs = CellSlice.beginParse(cellWithDict);
  //        BigInteger i = cs.loadUint(5);
  //        log.info("i = {}", i);
  //
  //        TonHashMap loadedDict = cs
  //                .loadDict(dictKeySize,
  //                        k -> k.readUint(dictKeySize),
  //                        v -> CellSlice.beginParse(v).loadUint(3)
  //                );
  //        Cell cRef = cs.loadRef();
  //
  //        assertThat(i.longValue()).isEqualTo(10);
  //        assertThat(loadedDict.elements.size()).isEqualTo(2);
  //        int j = 1;
  //        for (Map.Entry<Object, Object> entry : loadedDict.elements.entrySet()) {
  //            log.info("key {}, value {}", entry.getKey(), entry.getValue());
  //            assertThat((BigInteger) entry.getKey()).isEqualTo(100 * j);
  //            assertThat((BigInteger) entry.getValue()).isEqualTo(j);
  //            j++;
  //        }
  //        assertThat(CellSlice.beginParse(cRef).loadUint(8).longValue()).isEqualTo(7);
  //    }

  /** Test TonHashMapE - where empty dict is allowed */
  @Test
  public void testCellSliceWithCellWithEmptyDict() {
    int dictKeySize = 9;
    TonHashMapE x = new TonHashMapE(dictKeySize);

    x.elements.put(100L, (byte) 2);
    x.elements.put(200L, (byte) 4);

    Cell dict =
        x.serialize(
            k -> CellBuilder.beginCell().storeUint((Long) k, dictKeySize).endCell().getBits(),
            v -> CellBuilder.beginCell().storeUint((byte) v, 3).endCell());

    log.info("cell dict bits length: {}", dict.bits.getUsedBits());
    for (Map.Entry<Object, Object> entry : x.elements.entrySet()) {
      log.info("serialized key {}, value {}", entry.getKey(), entry.getValue());
    }

    Cell cellDict =
        CellBuilder.beginCell()
            .storeUint(10, 5)
            .storeDict(dict)
            .storeRef(CellBuilder.beginCell().storeUint(7, 8).endCell())
            .endCell();

    log.info(cellDict.print());

    CellSlice cs = CellSlice.beginParse(cellDict);
    BigInteger i = cs.loadUint(5);
    log.info("i = {}", i);
    assertThat(i.longValue()).isEqualTo(10);

    TonHashMap loadedDict =
        cs.loadDictE(
            dictKeySize, k -> k.readUint(dictKeySize), v -> CellSlice.beginParse(v).loadUint(3));
    Cell cRef = cs.loadRef();

    assertThat(loadedDict.elements.size()).isEqualTo(2);

    int j = 1;
    for (Map.Entry<Object, Object> entry : loadedDict.elements.entrySet()) {
      log.info("key {}, value {}", entry.getKey(), entry.getValue());
      assertThat((BigInteger) entry.getKey()).isEqualTo(100 * j);
      assertThat((BigInteger) entry.getValue()).isEqualTo(2 * j);
      j++;
    }
    long iRef = CellSlice.beginParse(cRef).loadUint(8).longValue();
    log.info("ref i = {}", iRef);
    assertThat(iRef).isEqualTo(7);
  }

  @Test
  public void testCellSliceWith2DictsAndPreload() {
    int keySizeX = 10;
    TonHashMapE x = new TonHashMapE(keySizeX);

    x.elements.put(100L, (byte) 1);
    x.elements.put(200L, (byte) 2);
    x.elements.put(300L, (byte) 3);
    x.elements.put(400L, (byte) 4);
    x.elements.put(500L, (byte) 5);
    x.elements.put(600L, (byte) 6);

    Cell dictX =
        x.serialize(
            k -> CellBuilder.beginCell().storeUint((Long) k, keySizeX).endCell().getBits(),
            v -> CellBuilder.beginCell().storeUint((byte) v, 8).endCell());

    log.info(
        "cell dict X bits length: {}, refs.size {}", dictX.bits.getUsedBits(), dictX.refs.size());

    int keySizeY = 64;
    TonHashMapE y = new TonHashMapE(keySizeY);

    y.elements.put(10000L, (byte) 77);
    y.elements.put(20000L, (byte) 88);
    y.elements.put(30000L, (byte) 99);

    Cell dictY =
        y.serialize(
            k -> CellBuilder.beginCell().storeUint((Long) k, keySizeY).endCell().getBits(),
            v -> CellBuilder.beginCell().storeUint((byte) v, 8).endCell());

    log.info(
        "cell dict Y bits length: {}, refs.size {}", dictX.bits.getUsedBits(), dictY.refs.size());

    Cell cell =
        CellBuilder.beginCell()
            .storeDict(dictX)
            .storeDict(dictY)
            .storeRef(CellBuilder.beginCell().storeUint(7, 8).endCell())
            .endCell();

    log.info("cell with uint, two dicts and ref:\n{}", cell.print());

    CellSlice cs = CellSlice.beginParse(cell);

    TonHashMap loadedDictX =
        cs.loadDictE(keySizeX, k -> k.readUint(keySizeX), v -> CellSlice.beginParse(v).loadUint(8));

    //        assertThat(i.longValue()).isEqualTo(10);

    assertThat(loadedDictX.elements.size()).isEqualTo(6);
    log.info("deserialized dictX:");
    int j = 1;
    for (Map.Entry<Object, Object> entry : loadedDictX.elements.entrySet()) {
      log.info("key {}, value {}", entry.getKey(), entry.getValue());
      assertThat((BigInteger) entry.getKey()).isEqualTo(100 * j);
      //            assertThat((BigInteger) entry.getValue()).isEqualTo(1 * j);
      j++;
    }

    TonHashMap loadedDictY =
        cs.loadDictE(keySizeY, k -> k.readUint(keySizeY), v -> CellSlice.beginParse(v).loadUint(8));

    assertThat(loadedDictY.elements.size()).isEqualTo(3);
    log.info("deserialized dictY:");
    int m = 1;
    for (Map.Entry<Object, Object> entry : loadedDictY.elements.entrySet()) {
      log.info("key {}, value {}", entry.getKey(), entry.getValue());
      assertThat((BigInteger) entry.getKey()).isEqualTo(10000L * m);
      //            assertThat((BigInteger) entry.getValue()).isEqualTo(1 * j);
      m++;
    }
    Cell cRef = cs.loadRef();
    assertThat(CellSlice.beginParse(cRef).loadUint(8).longValue()).isEqualTo(7);
  }

  @Test
  public void testCellSlicePreloadsWishHashMapE() {
    BitString bs0 = new BitString(10);

    bs0.writeUint(40, 8);

    Cell cRef0 = CellBuilder.beginCell().storeUint(1, 3).storeUint(100, 8).endCell();
    Cell cRef1 = CellBuilder.beginCell().storeUint(2, 3).storeUint(200, 8).endCell();

    int keySizeX = 10;
    TonHashMapE dict = new TonHashMapE(keySizeX);

    dict.elements.put(100L, (byte) 1);
    dict.elements.put(200L, (byte) 2);
    dict.elements.put(300L, (byte) 3);

    Cell dictCell =
        dict.serialize(
            k -> CellBuilder.beginCell().storeUint((Long) k, keySizeX).endCell().getBits(),
            v -> CellBuilder.beginCell().storeUint((byte) v, 8).endCell());

    Cell c0 =
        CellBuilder.beginCell()
            .storeUint(10, 8)
            .storeUint(20, 8)
            .storeInt(30, 8)
            .storeRef(cRef0)
            .storeRef(cRef1)
            .storeDict(dictCell)
            .storeBitString(bs0) // 40
            .endCell();

    CellSlice cs0 = CellSlice.beginParse(c0);

    assertThat(cs0.preloadUint(8)).isEqualTo(10);
    assertThat(cs0.preloadUint(8)).isEqualTo(10);
    assertThat(cs0.preloadUint(8)).isEqualTo(10);
    cs0.loadUint(8);
    assertThat(cs0.preloadUint(8)).isEqualTo(20);
    assertThat(cs0.preloadUint(8)).isEqualTo(20);
    cs0.loadUint(8);
    cs0.loadUint(8);

    assertThat(CellSlice.beginParse(cs0.preloadRef()).loadUint(3).longValue()).isEqualTo(1);
    assertThat(CellSlice.beginParse(cs0.preloadRef()).loadUint(3).longValue()).isEqualTo(1);
    Cell ref1 = cs0.loadRef();
    assertThat(CellSlice.beginParse(ref1).loadUint(3).longValue()).isEqualTo(1);
    assertThat(CellSlice.beginParse(cs0.preloadRef()).loadUint(3).longValue()).isEqualTo(2);
    cs0.loadRef();

    TonHashMap dictLoaded =
        cs0.preloadDictE(
            keySizeX, k -> k.readUint(keySizeX), v -> CellSlice.beginParse(v).loadUint(8));

    int j = 1;
    for (Map.Entry<Object, Object> entry : dictLoaded.elements.entrySet()) {
      log.info("key {}, value {}", entry.getKey(), entry.getValue());
      assertThat((BigInteger) entry.getKey()).isEqualTo(100 * j);
      assertThat((BigInteger) entry.getValue()).isEqualTo(j);
      j++;
    }

    TonHashMap dictLoadedReal =
        cs0.loadDictE(
            keySizeX, k -> k.readUint(keySizeX), v -> CellSlice.beginParse(v).loadUint(8));

    j = 1;
    for (Map.Entry<Object, Object> entry : dictLoadedReal.elements.entrySet()) {
      log.info("key {}, value {}", entry.getKey(), entry.getValue());
      assertThat((BigInteger) entry.getKey()).isEqualTo(100 * j);
      assertThat((BigInteger) entry.getValue()).isEqualTo(j);
      j++;
    }

    assertThat(cs0.preloadUint(8)).isEqualTo(40);
    assertThat(cs0.preloadBit()).isEqualTo(false);

    assertThrows(Error.class, () -> CellSlice.beginParse(cs0.preloadRef()).loadUint(3));
  }

  @Test
  public void testCellSlicePreloadsWithHashMap() {
    BitString bs0 = new BitString(10);

    bs0.writeUint(40, 8);

    Cell cRef0 = CellBuilder.beginCell().storeUint(1, 3).storeUint(100, 8).endCell();
    Cell cRef1 = CellBuilder.beginCell().storeUint(2, 3).storeUint(200, 8).endCell();

    int keySizeX = 10;
    TonHashMap dict = new TonHashMap(keySizeX);

    dict.elements.put(100L, (byte) 1);
    dict.elements.put(200L, (byte) 2);
    dict.elements.put(300L, (byte) 3);

    Cell dictCell =
        dict.serialize(
            k -> CellBuilder.beginCell().storeUint((Long) k, keySizeX).endCell().getBits(),
            v -> CellBuilder.beginCell().storeUint((byte) v, 8).endCell());

    Cell c0 =
        CellBuilder.beginCell()
            .storeUint(10, 8)
            .storeUint(20, 8)
            .storeInt(30, 8)
            .storeRef(cRef0)
            .storeRef(cRef1)
            .storeDictInLine(dictCell)
            .storeBitString(bs0) // 40
            .endCell();

    CellSlice cs0 = CellSlice.beginParse(c0);

    assertThat(cs0.preloadUint(8)).isEqualTo(10);
    assertThat(cs0.preloadUint(8)).isEqualTo(10);
    assertThat(cs0.preloadUint(8)).isEqualTo(10);
    cs0.loadUint(8);
    assertThat(cs0.preloadUint(8)).isEqualTo(20);
    assertThat(cs0.preloadUint(8)).isEqualTo(20);
    cs0.loadUint(8);
    cs0.loadUint(8);

    assertThat(CellSlice.beginParse(cs0.preloadRef()).loadUint(3).longValue()).isEqualTo(1);
    assertThat(CellSlice.beginParse(cs0.preloadRef()).loadUint(3).longValue()).isEqualTo(1);
    Cell ref1 = cs0.loadRef();
    assertThat(CellSlice.beginParse(ref1).loadUint(3).longValue()).isEqualTo(1);
    assertThat(CellSlice.beginParse(cs0.preloadRef()).loadUint(3).longValue()).isEqualTo(2);
    cs0.loadRef();

    TonHashMap dictLoaded =
        cs0.preloadDict(
            keySizeX, k -> k.readUint(keySizeX), v -> CellSlice.beginParse(v).loadUint(8));

    int j = 1;
    for (Map.Entry<Object, Object> entry : dictLoaded.elements.entrySet()) {
      log.info("key {}, value {}", entry.getKey(), entry.getValue());
      assertThat((BigInteger) entry.getKey()).isEqualTo(100 * j);
      assertThat((BigInteger) entry.getValue()).isEqualTo(j);
      j++;
    }
    // load again
    cs0.loadDict(keySizeX, k -> k.readUint(keySizeX), v -> CellSlice.beginParse(v).loadUint(8));

    assertThat(cs0.preloadUint(8)).isEqualTo(40);
    assertThat(cs0.preloadBit()).isEqualTo(false);

    assertThrows(Error.class, () -> CellSlice.beginParse(cs0.preloadRef()).loadUint(3));
  }

  @Test
  public void testCellSliceLoadCoins() {
    Cell cRef0 = CellBuilder.beginCell().storeUint(1, 3).storeUint(100, 8).endCell();

    Cell c0 =
        CellBuilder.beginCell()
            .storeUint(10, 8)
            .storeCoins(BigInteger.valueOf(12345))
            .storeUint(20, 8)
            .storeInt(-30, 8)
            .storeRef(cRef0)
            .endCell();

    CellSlice cs0 = CellSlice.beginParse(c0);

    assertThat(cs0.loadUint(8)).isEqualTo(10);

    assertThat(cs0.preloadCoins()).isEqualTo(new BigInteger("12345"));
    assertThat(cs0.loadCoins()).isEqualTo(new BigInteger("12345"));
    assertThat(cs0.loadUint(8)).isEqualTo(20);
    assertThat(cs0.loadInt(8)).isEqualTo(-30);
  }

  @Test
  public void testCellSlicePreloadCoins() {
    Cell cRef0 = CellBuilder.beginCell().storeUint(1, 3).storeUint(100, 8).endCell();

    Cell c0 =
        CellBuilder.beginCell()
            .storeUint(10, 8)
            .storeCoins(BigInteger.valueOf(12345))
            .storeInt(20, 8)
            .storeInt(30, 8)
            .storeRef(cRef0)
            .endCell();

    CellSlice cs0 = CellSlice.beginParse(c0);

    assertThat(cs0.loadUint(8)).isEqualTo(10);
    assertThat(cs0.preloadCoins()).isEqualTo(new BigInteger("12345"));
    assertThat(cs0.loadCoins()).isEqualTo(new BigInteger("12345"));
    assertThat(cs0.preloadInt(8)).isEqualTo(20);
    assertThat(cs0.loadInt(8)).isEqualTo(20);
    assertThat(cs0.loadUint(8)).isEqualTo(30);
  }

  @Test
  public void testCellSliceSkipCoins() {
    Cell cRef0 = CellBuilder.beginCell().storeUint(1, 3).storeUint(100, 8).endCell();

    Cell c0 =
        CellBuilder.beginCell()
            .storeUint(10, 8)
            .storeCoins(BigInteger.valueOf(12345))
            .storeInt(-20, 8)
            .storeInt(-30, 8)
            .storeRef(cRef0)
            .endCell();

    CellSlice cs0 = CellSlice.beginParse(c0);

    assertThat(cs0.preloadUint(8)).isEqualTo(10);
    cs0.skipBits(8);
    assertThat(cs0.preloadCoins()).isEqualTo(new BigInteger("12345"));
    assertThat(cs0.preloadCoins()).isEqualTo(new BigInteger("12345"));
    cs0.skipCoins();
    //        assertThrows(Error.class, cs0::preloadCoins);
    assertThat(cs0.preloadInt(8)).isEqualTo(-20);
    // assertThat(cs0.loadInt(8)).isEqualTo(-20);
    cs0.skipBits(8); // or simply skipBits(8)
    assertThat(cs0.preloadInt(8)).isEqualTo(-30);
    assertThat(cs0.loadInt(8)).isEqualTo(-30);
  }

  @Test
  public void testCellSliceWithCellWithDictOneEntry() {
    int dictKeySize = 9;
    TonHashMap x = new TonHashMap(dictKeySize);

    x.elements.put(100L, (byte) 1);

    Cell dict =
        x.serialize(
            k -> CellBuilder.beginCell().storeUint((Long) k, dictKeySize).endCell().getBits(),
            v -> CellBuilder.beginCell().storeUint((byte) v, 3).endCell());

    log.info("cell dict bits length: {}", dict.bits.getUsedBits());

    Cell cellDict = CellBuilder.beginCell().storeDictInLine(dict).endCell();

    log.info(cellDict.print());

    CellSlice cs = CellSlice.beginParse(cellDict);

    TonHashMap loadedDict =
        cs.loadDict(
            dictKeySize, k -> k.readUint(dictKeySize), v -> CellSlice.beginParse(v).loadUint(3));
    assertThat(loadedDict.elements.size()).isEqualTo(1);
    int j = 1;
    for (Map.Entry<Object, Object> entry : loadedDict.elements.entrySet()) {
      log.info("key {}, value {}", entry.getKey(), entry.getValue());
      assertThat((BigInteger) entry.getKey()).isEqualTo(100 * j);
      assertThat((BigInteger) entry.getValue()).isEqualTo(j);
      j++;
    }
  }
}
