package org.ton.ton4j.hashmaps;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.ton.ton4j.address.Address;
import org.ton.ton4j.bitstring.BitString;
import org.ton.ton4j.cell.*;
import org.ton.ton4j.utils.Utils;

@Slf4j
@RunWith(JUnit4.class)
public class TestHashMapE {

  @Test
  public void testEmptyHashMapESerialization() {
    int dictKeySize = 9;
    TonHashMapE x = new TonHashMapE(dictKeySize);

    Cell cellDict =
        x.serialize(
            k -> CellBuilder.beginCell().storeUint((Long) k, dictKeySize).endCell().getBits(),
            v -> CellBuilder.beginCell().storeUint((byte) v, 3).endCell());

    assertThat(cellDict).isNull();
    Cell cellWithDict = CellBuilder.beginCell().storeDict(cellDict).endCell();
    assertThat(cellWithDict.print().trim()).isEqualTo("x{4_}");
  }

  @Test
  public void testHashMapESerializationOneEntry() {
    int keySizeX = 9;
    TonHashMapE x = new TonHashMapE(keySizeX);

    x.elements.put(100L, (byte) 1);

    Cell cell =
        x.serialize(
            k -> CellBuilder.beginCell().storeUint((Long) k, keySizeX).endCell().getBits(),
            v -> CellBuilder.beginCell().storeUint((byte) v, 3).endCell());

    log.info("serialized cell: \n{}", cell.print());
    log.info("serialized boc: \n{}", cell.toHex());
    log.info("cell hash {}", Utils.bytesToHex(cell.hash()));
    assertThat(StringUtils.trim(cell.print())).isEqualTo("x{A4C86_}");
  }

  @Test
  public void testHashMapESerialization() {

    TonHashMapE x = new TonHashMapE(9);

    x.elements.put(100L, Address.of("0QAljlSWOKaYCuXTx2OCr9P08y40SC2vw3UeM1hYnI3gDY7I"));
    x.elements.put(200L, Address.of("Uf-CRYz9HRGdb19t7DOZUfUjwUZmngz-zJvpD8vpmF3xqeXg"));
    x.elements.put(300L, Address.of("UQCnuv+ZuR0QsIh5vwxUBuzzocSowbCa7ctdwl6QizBKzDiJ"));
    x.elements.put(400L, Address.of("Ef8zMzMzMzMzMzMzMzMzMzMzMzMzMzMzMzMzMzMzMzMzM0vF"));

    Cell dictCell =
        x.serialize(
            k -> CellBuilder.beginCell().storeUint((Long) k, 9).endCell().getBits(),
            v -> CellBuilder.beginCell().storeAddress((Address) v).endCell());

    log.info("serialized cell: \n{}", dictCell.print());
    log.info("serialized boc: \n{}", dictCell.toHex());
    log.info("cell hash {}", Utils.bytesToHex(dictCell.hash()));

    CellSlice cs = CellSlice.beginParse(dictCell);
    TonHashMap dex = cs.loadDict(9, k -> k.readUint(9), v -> CellSlice.beginParse(v).loadAddress());

    log.info("Deserialized hashmap from cell {}", dex);

    assertThat(dex.elements.size()).isEqualTo(4);
  }

  @Test
  public void testHashMapEOneEntrySerializationParse() {

    TonHashMapE x = new TonHashMapE(9);

    x.elements.put(100L, Address.of("0QAljlSWOKaYCuXTx2OCr9P08y40SC2vw3UeM1hYnI3gDY7I"));

    Cell dictCell =
        x.serialize(
            k -> CellBuilder.beginCell().storeUint((Long) k, 9).endCell().getBits(),
            v -> CellBuilder.beginCell().storeAddress((Address) v).endCell());

    log.info("serialized cell: \n{}", dictCell.print());
    log.info("serialized boc: \n{}", dictCell.toHex());
    log.info("cell hash {}", Utils.bytesToHex(dictCell.hash()));

    CellSlice cs = CellSlice.beginParse(dictCell);
    TonHashMap dex = cs.loadDict(9, k -> k.readUint(9), v -> CellSlice.beginParse(v).loadAddress());

    log.info("Deserialized hashmap from cell {}", dex);

    assertThat(dex.elements.size()).isEqualTo(1);
  }

  /**
   * Feel the difference: parseDictE - parses a cell that is already dict/hoshmap loadDictE - looks
   * for a dict in a cellSlice and loads it
   */
  @Test
  public void testHashMapEOneEntrySerialization() {

    TonHashMapE x = new TonHashMapE(9);

    x.elements.put(100L, Address.of("0QAljlSWOKaYCuXTx2OCr9P08y40SC2vw3UeM1hYnI3gDY7I"));

    Cell dictCell =
        x.serialize(
            k -> CellBuilder.beginCell().storeUint((Long) k, 9).endCell().getBits(),
            v -> CellBuilder.beginCell().storeAddress((Address) v).endCell());

    log.info("serialized cell: \n{}", dictCell.print());
    log.info("serialized boc: \n{}", dictCell.toHex());
    log.info("cell hash {}", Utils.bytesToHex(dictCell.hash()));

    Cell cellWithDict = CellBuilder.beginCell().storeDict(dictCell).endCell();

    CellSlice cs = CellSlice.beginParse(cellWithDict);
    TonHashMapE dex =
        cs.loadDictE(9, k -> k.readUint(9), v -> CellSlice.beginParse(v).loadAddress());

    log.info("Deserialized hashmap from cell {}", dex);

    assertThat(dex.elements.size()).isEqualTo(1);
  }

  @Test
  public void testEmptyHashMapEDeserialization() {
    int dictKeySize = 9;
    TonHashMapE x = new TonHashMapE(dictKeySize);

    Cell dictCell =
        x.serialize(
            k -> CellBuilder.beginCell().storeUint((Long) k, dictKeySize).endCell().getBits(),
            v -> CellBuilder.beginCell().storeUint((byte) v, 3).endCell());
    assertThat(dictCell).isNull();

    Cell cellWithDict = CellBuilder.beginCell().storeDict(dictCell).endCell();
    log.info("serialized cell: \n{}", cellWithDict.print());
    log.info("serialized boc: \n{}", cellWithDict.toHex());
    log.info("cell hash {}", Utils.bytesToHex(cellWithDict.hash()));

    CellSlice cs = CellSlice.beginParse(cellWithDict);

    TonHashMapE loadedDict =
        cs.loadDictE(
            dictKeySize, k -> k.readUint(dictKeySize), v -> CellSlice.beginParse(v).loadUint(3));

    log.info("Deserialized hashmap from cell {}", loadedDict);
  }

  @Test
  public void testHashMapEDeserializationOneEntry() {
    int keySizeX = 9;
    TonHashMapE x = new TonHashMapE(keySizeX);

    x.elements.put(100L, (byte) 1);

    Cell cellX =
        x.serialize(
            k -> CellBuilder.beginCell().storeUint((Long) k, keySizeX).endCell().getBits(),
            v -> CellBuilder.beginCell().storeUint((byte) v, 3).endCell());

    log.info("serialized cellX {}", cellX.print());
  }

  @Test
  public void testHashMapEDeserializationWithEmptyRightEdge() {

    String t =
        "B5EE9C72410106010020000101C0010202C8020302016204050007BEFDF2180007A68054C00007A08090C08D16037D";

    Cell cellWithDict = CellBuilder.beginCell().fromBoc(t).endCell();
    log.info("cell {}", cellWithDict.print());

    CellSlice cs = CellSlice.beginParse(cellWithDict);

    TonHashMapE loadedDict =
        cs.loadDictE(16, k -> k.readUint(16), v -> CellSlice.beginParse(v).loadUint(16));

    log.info("Deserialized hashmap from cell {}, count {}", loadedDict, loadedDict.elements.size());

    // serialize
    loadedDict.serialize(
        k -> CellBuilder.beginCell().storeUint((BigInteger) k, 16).endCell().getBits(),
        v -> CellBuilder.beginCell().storeUint((BigInteger) v, 16).endCell());
  }

  @Test
  public void testHashMapEDeserialization15() {
    int dictXKeySize = 15;
    TonHashMapE x = new TonHashMapE(dictXKeySize);

    x.elements.put(100L, CellBuilder.beginCell().storeUint(5, 5).endCell());
    x.elements.put(200L, CellBuilder.beginCell().storeUint(6, 5).endCell());
    x.elements.put(300L, CellBuilder.beginCell().storeUint(7, 5).endCell());

    Cell cellX =
        x.serialize(
            k -> CellBuilder.beginCell().storeUint((Long) k, dictXKeySize).endCell().getBits(),
            v -> CellBuilder.beginCell().storeSlice(CellSlice.beginParse((Cell) v)).endCell());

    log.info("serialized cellX:\n{}", cellX.print());
    log.info("Deserialized hashmap from cell {}", x);
  }

  @Test
  public void testHashMapEDeserializationTwoDicts() {
    int dictXKeySize = 9;
    TonHashMapE x = new TonHashMapE(dictXKeySize);

    x.elements.put(100L, (byte) 1);
    x.elements.put(200L, (byte) 2);
    x.elements.put(300L, (byte) 3);

    Cell cellX =
        x.serialize(
            k -> CellBuilder.beginCell().storeUint((Long) k, dictXKeySize).endCell().getBits(),
            v -> CellBuilder.beginCell().storeUint((byte) v, 3).endCell());

    log.info("serialized cellX:\n{}", cellX.print());

    int dictYKeySize = 64;
    TonHashMapE y = new TonHashMapE(dictYKeySize);

    y.elements.put(400L, (byte) 40);
    y.elements.put(300L, (byte) 30);
    y.elements.put(200L, (byte) 20);
    y.elements.put(100L, (byte) 10);

    log.info("hashmap y {}", y);

    Cell cellY =
        y.serialize(
            k -> CellBuilder.beginCell().storeUint((Long) k, dictYKeySize).endCell().getBits(),
            v -> CellBuilder.beginCell().storeUint((byte) v, 8).endCell());
    log.info("serialized cellY:\n{}", cellY.print());

    Cell cell = CellBuilder.beginCell().storeDict(cellX).storeDict(cellY).endCell();

    log.info("serialized cell (x+y):\n{}", cell.print());

    CellSlice cs = CellSlice.beginParse(cell);
    TonHashMap loadedDictX =
        cs.loadDictE(
            dictXKeySize, k -> k.readUint(dictXKeySize), v -> CellSlice.beginParse(v).loadUint(3));

    log.info("hashmap loadedDictX {}", loadedDictX);

    int j = 1;
    log.info("Deserialized hashmap from loadedDictX");
    for (Map.Entry<Object, Object> entry : loadedDictX.elements.entrySet()) {
      log.info("key {}, value {}", entry.getKey(), entry.getValue());
      assertThat((BigInteger) entry.getKey()).isEqualTo(100 * j);
      assertThat((BigInteger) entry.getValue()).isEqualTo(j);
      j++;
    }

    TonHashMap loadedDictY =
        cs.loadDictE(
            dictYKeySize, k -> k.readUint(dictYKeySize), v -> CellSlice.beginParse(v).loadUint(8));

    log.info("hashmap loadedDictX {}", loadedDictY);
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

    log.info("hashmap e {}", x);

    Cell cellX =
        x.serialize(
            k -> CellBuilder.beginCell().storeString((String) k).endCell().getBits(),
            v -> CellBuilder.beginCell().storeUint((byte) v, 3).endCell());

    log.info("serialized cellX:\n{}", cellX.print());

    Cell cell = CellBuilder.beginCell().storeDict(cellX).endCell();

    log.info("serialized cell:\n{}", cell.print());

    CellSlice cs = CellSlice.beginParse(cell);
    TonHashMap loadedDictX =
        cs.loadDictE(40, k -> k.readString(40), v -> CellSlice.beginParse(v).loadUint(3));

    log.info("hashmap loadedDictX {}", loadedDictX);

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

    log.info("hashmap x {}", x);

    Cell cellX =
        x.serialize(
            k -> CellBuilder.beginCell().storeBytes((byte[]) k).endCell().getBits(),
            v -> CellBuilder.beginCell().storeUint((byte) v, 3).endCell());

    log.info("serialized cellX:\n{}", cellX.print());

    Cell cell = CellBuilder.beginCell().storeDict(cellX).endCell();

    log.info("serialized cell:\n{}", cell.print());

    CellSlice cs = CellSlice.beginParse(cell);
    TonHashMap loadedDictX =
        cs.loadDictE(
            dictKeySize, k -> k.readBytes(dictKeySize), v -> CellSlice.beginParse(v).loadUint(3));

    log.info("hashmap loadedDictX {}", loadedDictX);

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

    log.info("hashmap x {}", x);

    Cell cellX =
        x.serialize(
            k -> CellBuilder.beginCell().storeBitString((BitString) k).endCell().getBits(),
            v -> CellBuilder.beginCell().storeUint((byte) v, 3).endCell());

    log.info("serialized cellX:\n{}", cellX.print());

    Cell cell = CellBuilder.beginCell().storeDict(cellX).endCell();

    log.info("serialized cell:\n{}", cell.print());

    CellSlice cs = CellSlice.beginParse(cell);
    TonHashMap loadedDictX =
        cs.loadDictE(
            dictKeySize, k -> k.readBits(dictKeySize), v -> CellSlice.beginParse(v).loadUint(3));

    log.info("hashmap loadedDictX {}", loadedDictX);
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

    Address addr1 =
        Address.of("0:2cf55953e92efbeadab7ba725c3f93a0b23f842cbba72d7b8e6f510a70e422e3");
    Address addr2 =
        Address.of("0:0000000000000000000000000000000000000000000000000000000000000000");
    Address addr3 =
        Address.of("0:1111111111111111111111111111111111111111111111111111111111111111");
    Address addr4 =
        Address.of("0:3333333333333333333333333333333333333333333333333333333333333333");
    Address addr5 =
        Address.of("0:5555555555555555555555555555555555555555555555555555555555555555");
    x.elements.put(addr1, (byte) 1);
    x.elements.put(addr2, (byte) 2);
    x.elements.put(addr3, (byte) 3);
    x.elements.put(addr4, (byte) 4);
    x.elements.put(addr5, (byte) 5);

    log.info("hashmap x {}", x);

    Cell cellX =
        x.serialize(
            k -> CellBuilder.beginCell().storeAddress((Address) k).endCell().getBits(),
            v -> CellBuilder.beginCell().storeUint((byte) v, 3).endCell());

    log.info("serialized cellX:\n{}", cellX.print());

    Cell cell = CellBuilder.beginCell().storeDict(cellX).endCell();

    log.info("serialized cell:\n{}", cell.print());

    CellSlice cs = CellSlice.beginParse(cell);
    TonHashMap loadedDictX =
        cs.loadDictE(dictKeySize, BitString::readAddress, v -> CellSlice.beginParse(v).loadUint(3));

    assertThat(loadedDictX.elements.containsValue(new BigInteger("5"))).isTrue();

    assertThat(loadedDictX.elements.size()).isEqualTo(5);
  }

  /** <a href="https://github.com/neodix42/ton4j/issues/63">issue 64</a> */
  @Test
  public void testIssue63() throws IOException {
    String longDict =
        IOUtils.toString(
            Objects.requireNonNull(getClass().getResourceAsStream("/long-dict-base64.txt")),
            StandardCharsets.UTF_8);
    Cell c = CellBuilder.beginCell().fromBocBase64(longDict).endCell();

    log.info("cell print {}", c.print());

    CellSlice cs = CellSlice.beginParse(c);
    TonHashMap loadedDictX =
        cs.loadDict(267, BitString::readAddress, v -> CellSlice.beginParse(v).loadCoins());

    log.info("dict {}", loadedDictX.toString());
  }
}
