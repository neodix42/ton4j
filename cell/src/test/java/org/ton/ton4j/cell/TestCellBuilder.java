package org.ton.ton4j.cell;

import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.ton.ton4j.address.Address;
import org.ton.ton4j.utils.Utils;

import java.math.BigInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertThrows;

@Slf4j
@RunWith(JUnit4.class)
public class TestCellBuilder {

  @Test
  public void testCell() {
    Cell c = new Cell();
  }

  @Test
  public void testCellBuilderEmpty() {
    Cell c = CellBuilder.beginCell().endCell();
    assertThat(0).isEqualTo(c.bits.getUsedBits());
    log.info("Hash {}", Utils.bytesToHex(c.getHash()));
    log.info(Utils.bytesToHex(c.toBoc(false)));
    log.info("CellType {}", c.getCellType());
  }

  @Test(expected = java.lang.Error.class)
  public void testCellTooManyBitsAdded() {
    CellBuilder cellBuilder = CellBuilder.beginCell().storeUint(0, 2000);
    assertThat(10).isEqualTo(cellBuilder.getUsedBits());

    cellBuilder.storeBit(true);
  }

  @Test
  public void testCellBuilderNumber() {
    // uint
    assertThat(CellBuilder.beginCell().storeUint(42, 7).endCell().bitStringToHex())
        .isEqualTo("55_");
    assertThat(CellBuilder.beginCell().storeUint(0, 8).endCell().bitStringToHex()).isEqualTo("00");
    assertThat(CellBuilder.beginCell().storeUint(1, 8).endCell().bitStringToHex()).isEqualTo("01");
    assertThat(CellBuilder.beginCell().storeUint(5, 8).endCell().bitStringToHex()).isEqualTo("05");
    assertThat(CellBuilder.beginCell().storeUint(33, 8).endCell().bitStringToHex()).isEqualTo("21");
    assertThat(CellBuilder.beginCell().storeUint(127, 8).endCell().bitStringToHex())
        .isEqualTo("7F");
    assertThat(CellBuilder.beginCell().storeUint(128, 8).endCell().bitStringToHex())
        .isEqualTo("80");
    assertThat(CellBuilder.beginCell().storeUint(255, 8).endCell().bitStringToHex())
        .isEqualTo("FF");
    assertThat(CellBuilder.beginCell().storeUint(256, 9).endCell().bitStringToHex())
        .isEqualTo("804_");

    // int
    assertThat(CellBuilder.beginCell().storeInt(127, 8).endCell().bitStringToHex()).isEqualTo("7F");
    assertThat(CellBuilder.beginCell().storeInt(0, 8).endCell().bitStringToHex()).isEqualTo("00");
    assertThat(CellBuilder.beginCell().storeInt(-1, 8).endCell().bitStringToHex()).isEqualTo("FF");
    assertThat(CellBuilder.beginCell().storeInt(-5, 8).endCell().bitStringToHex()).isEqualTo("FB");
    assertThat(CellBuilder.beginCell().storeInt(-33, 8).endCell().bitStringToHex()).isEqualTo("DF");
    assertThat(CellBuilder.beginCell().storeInt(-127, 8).endCell().bitStringToHex())
        .isEqualTo("81");
    assertThat(CellBuilder.beginCell().storeInt(-128, 8).endCell().bitStringToHex())
        .isEqualTo("80");
    assertThat(CellBuilder.beginCell().storeInt(-129, 9).endCell().bitStringToHex())
        .isEqualTo("BFC_");
    assertThat(CellBuilder.beginCell().storeInt(17239, 16).endCell().bitStringToHex())
        .isEqualTo("4357");
    assertThat(CellBuilder.beginCell().storeInt(-17, 11).endCell().bitStringToHex())
        .isEqualTo("FDF_");
    assertThat(CellBuilder.beginCell().storeInt((short) -17, 11).endCell().bitStringToHex())
        .isEqualTo("FDF_");
    assertThat(CellBuilder.beginCell().storeInt((byte) -17, 11).endCell().bitStringToHex())
        .isEqualTo("FDF_");
    assertThat(CellBuilder.beginCell().storeInt(1000000239, 32).endCell().bitStringToHex())
        .isEqualTo("3B9ACAEF");

    assertThat(
            CellBuilder.beginCell()
                .storeInt(1000000239L * 1000000239, 91)
                .endCell()
                .bitStringToHex())
        .isEqualTo("00000001BC16E45E4D41643_");
    assertThat(
            CellBuilder.beginCell()
                .storeInt(new BigInteger("-1000000000000000000000000239"), 91)
                .endCell()
                .bitStringToHex())
        .isEqualTo("989A386C05EFF862FFFFE23_");
  }

  @Test(expected = java.lang.Error.class)
  public void testCellBuilderFailures() {
    assertThat(CellBuilder.beginCell().storeUint(256, 8).endCell().bitStringToHex())
        .isEqualTo("804_");
    assertThat(CellBuilder.beginCell().storeUint(-1, 8).endCell().bitStringToHex())
        .isEqualTo("804_");
    assertThat(CellBuilder.beginCell().storeUint(-129, 91).endCell().bitStringToHex())
        .isEqualTo("804_");
  }

  @Test
  public void testCellBuilderMultipleNumbers() {
    Cell c =
        CellBuilder.beginCell()
            .storeUint(0, 16)
            .storeUint(1, 8)
            .storeUint(1, 8)
            .storeUint(69, 64)
            .endCell();
    assertThat(c.bitStringToHex()).isEqualTo("000001010000000000000045");
  }

  @Test
  public void testCellBuilderRefs() {
    Cell c1 = CellBuilder.beginCell().storeUint(0, 8).endCell();
    Cell c2 = CellBuilder.beginCell().storeUint(42, 7).storeInt(10, 5).storeRef(c1).endCell();
    log.info(c2.bitStringToHex());
  }

  @Test
  public void testCellBuilderCells() {
    Cell c1 = CellBuilder.beginCell().storeUint((long) Math.pow(2, 25), 26).endCell();

    Cell c2 = CellBuilder.beginCell().storeUint((long) Math.pow(2, 37), 38).storeRef(c1).endCell();

    Cell c3 = CellBuilder.beginCell().storeUint((long) Math.pow(2, 41), 42).endCell();
    Cell c4 = CellBuilder.beginCell().storeUint((long) Math.pow(2, 42), 44).endCell();
    Cell c5 =
        CellBuilder.beginCell()
            .storeAddress(
                Address.parseFriendlyAddress("0QAljlSWOKaYCuXTx2OCr9P08y40SC2vw3UeM1hYnI3gDY7I"))
            .storeString("HELLO")
            .storeRef(c2)
            .storeRefs(c3, c4)
            .endCell();

    assertThat(c5.getUsedRefs()).isEqualTo(3);

    byte[] serializedCell5 = c5.toBoc(false);

    Cell dc5 = CellBuilder.beginCell().fromBoc(serializedCell5).endCell();
    log.info("c5 deserialized:\n{}", dc5.print());
  }

  @Test
  public void testCellBuilderStoreCoins() {

    assertThrows(
        Error.class,
        () -> {
          BigInteger coins0 = Utils.toNano(9999.99999999999);
          Cell c0 = CellBuilder.beginCell().storeCoins(coins0).endCell();
          BigInteger i = CellSlice.beginParse(c0).loadCoins();
          assertThat(i.toString()).isEqualTo("9999.99999999999");

          BigInteger coins1 =
              new BigInteger("9999999999999999999999999999999999999999999999999999999999");
          Cell c1 = CellBuilder.beginCell().storeCoins(coins1).endCell();
          BigInteger i1 = CellSlice.beginParse(c1).loadCoins();
          assertThat(i1.toString())
              .isEqualTo("9999999999999999999999999999999999999999999999999999999999");
        });

    BigInteger coins0 = Utils.toNano(9999.99999999);
    Cell c0 = CellBuilder.beginCell().storeCoins(coins0).endCell();
    BigInteger i = CellSlice.beginParse(c0).loadCoins();
    assertThat(i.toString()).isEqualTo("9999999999990");
  }

  @Test
  public void testCellBuilderStoreDict() {
    TonHashMap x = new TonHashMap(9);
    x.elements.put(100L, (byte) 1);
    x.elements.put(200L, (byte) 2);
    x.elements.put(300L, (byte) 3);
    x.elements.put(400L, (byte) 4);
    Cell dict =
        x.serialize(
            k -> CellBuilder.beginCell().storeUint((Long) k, 9).endCell().getBits(),
            v -> CellBuilder.beginCell().storeUint((byte) v, 3).endCell());
    Cell cellDict = CellBuilder.beginCell().storeDict(dict).endCell();
    assertThat(cellDict).isNotNull();
  }

  @Test
  public void testCellMask() {
    //        log.info("min bits {} for int {}", Cell.calculateMinimumBits(6), 6);
    //        log.info("min bits {} for int {}", Cell.calculateMinimumBits(7), 7);
    //        log.info("min bits {} for int {}", Cell.calculateMinimumBits(11), 11);
    //        log.info("ones bits {} for int {}", Cell.calculateOnesBits(7), 7);
    //        log.info("ones bits {} for int {}", Cell.calculateOnesBits(6), 6);
    //        log.info("ones bits {} for int {}", Cell.calculateOnesBits(11), 11);
  }
}
