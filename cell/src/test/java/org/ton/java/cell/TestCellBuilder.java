package org.ton.java.cell;

import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.ton.java.address.Address;
import org.ton.java.utils.Utils;

import java.math.BigInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertThrows;

@Slf4j
@RunWith(JUnit4.class)
public class TestCellBuilder {

    @Test
    public void testCellBuilderEmpty() {
        Cell c = CellBuilder.beginCell().endCell();
        assertThat(0).isEqualTo(c.bits.getUsedBits());
        log.info(Utils.bytesToHex(c.toBocNew(false)));
        log.info("CellType {}", c.getCellType());
    }

    @Test
    public void testCellBuilderSingleBit() {
        Cell c = CellBuilder.beginCell()
                .storeBit(true)
                .endCell();

        Cell r = new Cell();
        r.bits.writeBit(true);
        assertThat(r.bits.toBitString()).isEqualTo(c.bits.toBitString());
    }

    @Test
    public void testCellBuilderMultipleBits() {
        Cell c = CellBuilder.beginCell()
                .storeBit(true)
                .storeBit(false)
                .storeBit(false)
                .storeBit(true)
                .storeBit(false)
                .endCell();

        Cell r = new Cell();
        r.bits.writeBit(true);
        r.bits.writeBit(false);
        r.bits.writeBit(false);
        r.bits.writeBit(true);
        r.bits.writeBit(false);

        assertThat(r.bits.toBitString()).isEqualTo(c.bits.toBitString());
    }

    @Test(expected = java.lang.Error.class)
    public void testCellTooManyBitsAdded() {
        CellBuilder cellBuilder = CellBuilder.beginCell().storeUint(0, 2000);
        assertThat(10).isEqualTo(cellBuilder.getUsedBits());

        cellBuilder.storeBit(true);
    }

    @Test
    public void testCellBuilderNumber() {
        //uint
        assertThat(CellBuilder.beginCell().storeUint(42, 7).endCell().bits.toHex()).isEqualTo("55_");
        assertThat(CellBuilder.beginCell().storeUint(0, 8).endCell().bits.toHex()).isEqualTo("00");
        assertThat(CellBuilder.beginCell().storeUint(1, 8).endCell().bits.toHex()).isEqualTo("01");
        assertThat(CellBuilder.beginCell().storeUint(5, 8).endCell().bits.toHex()).isEqualTo("05");
        assertThat(CellBuilder.beginCell().storeUint(33, 8).endCell().bits.toHex()).isEqualTo("21");
        assertThat(CellBuilder.beginCell().storeUint(127, 8).endCell().bits.toHex()).isEqualTo("7F");
        assertThat(CellBuilder.beginCell().storeUint(128, 8).endCell().bits.toHex()).isEqualTo("80");
        assertThat(CellBuilder.beginCell().storeUint(255, 8).endCell().bits.toHex()).isEqualTo("FF");
        assertThat(CellBuilder.beginCell().storeUint(256, 9).endCell().bits.toHex()).isEqualTo("804_");

        //int
        assertThat(CellBuilder.beginCell().storeInt(127, 8).endCell().bits.toHex()).isEqualTo("7F");
        assertThat(CellBuilder.beginCell().storeInt(0, 8).endCell().bits.toHex()).isEqualTo("00");
        assertThat(CellBuilder.beginCell().storeInt(-1, 8).endCell().bits.toHex()).isEqualTo("FF");
        assertThat(CellBuilder.beginCell().storeInt(-5, 8).endCell().bits.toHex()).isEqualTo("FB");
        assertThat(CellBuilder.beginCell().storeInt(-33, 8).endCell().bits.toHex()).isEqualTo("DF");
        assertThat(CellBuilder.beginCell().storeInt(-127, 8).endCell().bits.toHex()).isEqualTo("81");
        assertThat(CellBuilder.beginCell().storeInt(-128, 8).endCell().bits.toHex()).isEqualTo("80");
        assertThat(CellBuilder.beginCell().storeInt(-129, 9).endCell().bits.toHex()).isEqualTo("BFC_");
        assertThat(CellBuilder.beginCell().storeInt(17239, 16).endCell().bits.toHex()).isEqualTo("4357");
        assertThat(CellBuilder.beginCell().storeInt(-17, 11).endCell().bits.toHex()).isEqualTo("FDF_");
        assertThat(CellBuilder.beginCell().storeInt((short) -17, 11).endCell().bits.toHex()).isEqualTo("FDF_");
        assertThat(CellBuilder.beginCell().storeInt((byte) -17, 11).endCell().bits.toHex()).isEqualTo("FDF_");
        assertThat(CellBuilder.beginCell().storeInt(1000000239, 32).endCell().bits.toHex()).isEqualTo("3B9ACAEF");

        assertThat(CellBuilder.beginCell().storeInt(1000000239L * 1000000239, 91).endCell().bits.toHex()).isEqualTo("00000001BC16E45E4D41643_");
        assertThat(CellBuilder.beginCell().storeInt(new BigInteger("-1000000000000000000000000239"), 91).endCell().bits.toHex()).isEqualTo("989A386C05EFF862FFFFE23_");
    }

    @Test(expected = java.lang.Error.class)
    public void testCellBuilderFailures() {
        assertThat(CellBuilder.beginCell().storeUint(256, 8).endCell().bits.toHex()).isEqualTo("804_");
        assertThat(CellBuilder.beginCell().storeUint(-1, 8).endCell().bits.toHex()).isEqualTo("804_");
        assertThat(CellBuilder.beginCell().storeUint(-129, 91).endCell().bits.toHex()).isEqualTo("804_");
    }

    @Test
    public void testCellBuilderMultipleNumbers() {
        Cell c = CellBuilder.beginCell()
                .storeUint(0, 16)
                .storeUint(1, 8)
                .storeUint(1, 8)
                .storeUint(69, 64)
                .endCell();
        assertThat(c.bits.toHex()).isEqualTo("000001010000000000000045");
    }

    @Test
    public void testCellBuilderRefs() {
        Cell c1 = CellBuilder.beginCell().storeUint(0, 8).endCell();
        Cell c2 = CellBuilder.beginCell().storeUint(42, 7).storeInt(10, 5).storeRef(c1).endCell();
        log.info(c2.bits.toHex());
    }

    @Test
    public void testCellBuilderCells() {
        Cell c1 = CellBuilder.beginCell().storeUint((long) Math.pow(2, 25), 26).endCell();

        Cell c2 = CellBuilder.beginCell()
                .storeUint((long) Math.pow(2, 37), 38)
                .storeRef(c1)
                .endCell();

        Cell c3 = CellBuilder.beginCell().storeUint((long) Math.pow(2, 41), 42).endCell();
        Cell c4 = CellBuilder.beginCell().storeUint((long) Math.pow(2, 42), 44).endCell();
        Cell c5 = CellBuilder.beginCell()
                .storeAddress(Address.parseFriendlyAddress("0QAljlSWOKaYCuXTx2OCr9P08y40SC2vw3UeM1hYnI3gDY7I"))
                .storeString("HELLO")
                .storeRef(c2)
                .storeRefs(c3, c4)
                .endCell();

        log.info("c1 {}", c1.bits);
        log.info("c2 {}", c2.bits);
        log.info("c2:\n{}", c2.print());
        log.info("c3 {}", c3.bits);
        log.info("c4 {}", c4.bits);
        log.info("c5 {}", c5.bits);
        log.info("c5:\n{}", c5.print());

        assertThat(c5.getUsedRefs()).isEqualTo(3);

        int[] serializedCell5 = c5.toBocNew(false);

        Cell dc5 = Cell.fromBoc(serializedCell5);
        log.info("c5 deserialized:\n{}", dc5.print());

    }

    @Test
    public void testCellBuilderStoreCoins() {

        assertThrows(Error.class, () -> {
            BigInteger coins0 = Utils.toNano(9999.99999999999);
            Cell c0 = CellBuilder.beginCell().storeCoins(coins0);
            BigInteger i = CellSlice.beginParse(c0).loadCoins();
            assertThat(i.toString()).isEqualTo("9999.99999999999");

            BigInteger coins1 = new BigInteger("9999999999999999999999999999999999999999999999999999999999");
            Cell c1 = CellBuilder.beginCell().storeCoins(coins1);
            BigInteger i1 = CellSlice.beginParse(c1).loadCoins();
            assertThat(i1.toString()).isEqualTo("9999999999999999999999999999999999999999999999999999999999");
        });

        BigInteger coins0 = Utils.toNano(9999.99999999);
        Cell c0 = CellBuilder.beginCell().storeCoins(coins0);
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
        Cell dict = x.serialize(
                k -> CellBuilder.beginCell().storeUint((Long) k, 9).bits,
                v -> CellBuilder.beginCell().storeUint((byte) v, 3)
        );
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