package org.ton.java.cell;

import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.ton.java.address.Address;
import org.ton.java.utils.Utils;

import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
@RunWith(JUnit4.class)
public class TestCellSerialization {

    @Test
    public void testCellSerialization0() {
        Cell c1 = CellBuilder.beginCell().storeUint(17, 8).endCell();
        Cell c2 = CellBuilder.beginCell().storeUint(42, 7).storeRef(c1).endCell();
        log.info(Utils.bytesToHex(c1.hash()));
        log.info(Utils.bytesToHex(c2.hash()));
        log.info(Utils.bytesToHex(c2.toBoc()));
    }

    @Test
    public void testCellSerialization1() {
        Cell c0 = CellBuilder.beginCell().storeUint(17, 8).endCell();
        Cell c2 = CellBuilder.beginCell().storeUint(42, 7).storeRef(c0).endCell();
        Cell c3 = CellBuilder.beginCell().storeUint(73, 255).endCell();
        Cell c4 = CellBuilder.beginCell().storeUint((long) Math.pow(2, 42), 44).endCell();
        Cell c5 = CellBuilder.beginCell().storeUint((long) Math.pow(2, 41), 42).endCell();


        log.info(c2.print());
        log.info(Utils.bytesToHex(c2.toBoc(true)));

        assertThat(c2.bitStringToHex()).isEqualTo("55_");

        log.info(Utils.bytesToHex(c3.toBoc(true)));
        assertThat(c3.bitStringToHex()).isEqualTo("0000000000000000000000000000000000000000000000000000000000000093_");

        assertThat(Utils.bytesToHex(c2.toBoc(true))).isEqualTo("b5ee9c7241010201000700010155010002113256999a");
        assertThat(Utils.bytesToHex(c3.toBoc(true))).isEqualTo("b5ee9c7241010101002200003f000000000000000000000000000000000000000000000000000000000000009352a2f27c");
        assertThat(Utils.bytesToHex(c4.toBoc(true))).isEqualTo("b5ee9c7241010101000800000b4000000000085566f674");

        Cell c1 = CellBuilder.beginCell()
                .storeUint(0, 8)
                .storeRef(c2)
                .storeRef(c3)
                .storeRef(c4)
                .storeRef(c5)
                .endCell();


        log.info("================= {}", c1.hash());
        log.info("================= {}", c1.print());
        log.info("================= {}", Utils.bytesToHex(c1.toBoc(true)));
        assertThat(Utils.bytesToHex(c1.toBoc(true))).isEqualTo("b5ee9c724101060100400004020004030201000b800000000020000b400000000008003f0000000000000000000000000000000000000000000000000000000000000093010155050002113edf05ed");
    }

    @Test
    public void testCellSerialization2() {
        CellBuilder c0 = CellBuilder.beginCell();
        c0.storeUint(73, 255);
        assertThat(c0.bits.toHex()).isEqualTo("0000000000000000000000000000000000000000000000000000000000000093_");
        assertThat(c0.bits.toHex()).isEqualTo("0000000000000000000000000000000000000000000000000000000000000093_");

        CellBuilder c1 = CellBuilder.beginCell();
        CellBuilder c2 = CellBuilder.beginCell();
        CellBuilder c3 = CellBuilder.beginCell();
        CellBuilder c4 = CellBuilder.beginCell();

        long l1 = (long) Math.pow(2, 25);
        long l2 = (long) Math.pow(2, 37);
        long l3 = (long) Math.pow(2, 41);
        long l4 = (long) Math.pow(2, 42);

        c1.storeUint(l1, 26);
        c2.storeUint(l2, 38);
        c3.storeUint(l3, 42);
        c4.storeUint(l4, 44);

        assertThat(c1.toHex()).isEqualTo("8000002_");
        assertThat(c2.toHex()).isEqualTo("8000000002_");
        assertThat(c3.toHex()).isEqualTo("80000000002_");
        assertThat(c4.toHex()).isEqualTo("40000000000");
    }

    @Test
    public void testCellSerialization3() {
        CellBuilder c1 = CellBuilder.beginCell();
        c1.calculateHashes();

        long l1 = (long) Math.pow(2, 25);
        c1.storeUint(l1, 26);
        log.info("c1 bitString  {}", c1.toBitString());
        log.info("c1 hex        {}", c1.bitStringToHex());
        assertThat(c1.bitStringToHex()).isEqualTo("8000002_");

        log.info("hashes " + c1.hashes.size());
        log.info("hashes(1) " + c1.hashes.get(0));
        log.info("depth " + c1.depths.size());
        log.info("hash old " + Utils.bytesToHex(c1.hash()));
        log.info("hash new " + Utils.bytesToHex(c1.getHash()));

        byte[] serializedCell1 = c1.toBoc(true);
        Cell dc1 = Cell.fromBoc(serializedCell1);
        log.info("dc1 bitString {}", dc1.toBitString());
        log.info("dc1 hex       {}", dc1.bitStringToHex());

        assertThat(dc1.bitStringToHex()).isEqualTo("8000002_");
    }

    @Test
    public void testCellSerialization4() {
        Cell c1 = CellBuilder.beginCell().storeUint((long) Math.pow(2, 25), 26).endCell();
        Cell c2 = CellBuilder.beginCell().storeUint((long) Math.pow(2, 37), 38).endCell();
        Cell c3 = CellBuilder.beginCell().storeUint((long) Math.pow(2, 41), 42).endCell();
        Cell c4 = CellBuilder.beginCell().storeUint((long) Math.pow(2, 42), 44).endCell();
        Cell c5 = CellBuilder.beginCell()
                .storeAddress(Address.parseFriendlyAddress("0QAljlSWOKaYCuXTx2OCr9P08y40SC2vw3UeM1hYnI3gDY7I"))
                .endCell();

        log.info("c1 " + c1.toHex());
        log.info("c2 " + c2.toHex());
        log.info("c3 " + c3.toHex());
        log.info("c4 " + c4.toHex());
        log.info("c5 " + c5.toHex());

        assertThat(Utils.bytesToHex(c1.toBoc(true))).isEqualTo("b5ee9c7241010101000600000780000020d742a310");
        assertThat(Utils.bytesToHex(c2.toBoc(true))).isEqualTo("b5ee9c72410101010007000009800000000245bf8536");
        assertThat(Utils.bytesToHex(c3.toBoc(true))).isEqualTo("b5ee9c7241010101000800000b800000000020795c9ecc");
        assertThat(Utils.bytesToHex(c4.toBoc(true))).isEqualTo("b5ee9c7241010101000800000b4000000000085566f674");
        assertThat(Utils.bytesToHex(c5.toBoc(true))).isEqualTo("b5ee9c724101010100240000438004b1ca92c714d3015cba78ec7055fa7e9e65c68905b5f86ea3c66b0b1391bc01b0dedc73c3");

        assertThat(c1.bits.toHex()).isEqualTo("8000002_");


        c3.refs.add(c5);
        c2.refs.add(c3);
        c1.refs.add(c2);
        c1.refs.add(c4);


        log.info("c1 " + c1.toHex());
        log.info("c2 " + c2.toHex());
        log.info("c3 " + c3.toHex());
        log.info("c4 " + c4.toHex());
        log.info("c5 " + c5.toHex());

        log.info("c1 hex: {}", Utils.bytesToHex(c1.toBoc(true)));
        assertThat(Utils.bytesToHex(c1.toBoc(true))).isEqualTo("b5ee9c72410105010045000207800000200201000b4000000000080109800000000203010b8000000000200400438004b1ca92c714d3015cba78ec7055fa7e9e65c68905b5f86ea3c66b0b1391bc01b0b8a46275");

        byte[] serializedCell1 = c1.toBoc(true);
        log.info("c1 hex {}", Utils.bytesToHex(serializedCell1));
        Cell dc1 = Cell.fromBoc(serializedCell1);
        log.info("c1 deserialized");
        log.info(dc1.print());
        log.info("dc1 hex: {}", Utils.bytesToHex(dc1.toBoc(true)));

        assertThat(Utils.bytesToHex(serializedCell1)).isEqualTo(Utils.bytesToHex(dc1.toBoc(true)));
        assertThat(Utils.bytesToHex(serializedCell1)).isEqualTo(dc1.toHex(true));
        assertThat(serializedCell1).isEqualTo(dc1.toBoc(true));

        Cell c6 = c1.clone();
        assertThat(Utils.bytesToHex(serializedCell1)).isEqualTo(c6.toHex(true));
        assertThat(serializedCell1).isEqualTo(c6.toBoc(true));
        assertThat(c1.toBoc(true)).isEqualTo(dc1.toBoc(true));
        assertThat(c1.toBoc(true)).isEqualTo(Cell.fromBoc(dc1.toBoc(true)).toBoc(true));

        assertThat(dc1.bits.toString()).isEqualTo(c1.bits.toString());
        assertThat(dc1.refs.get(0).bits.toString()).isEqualTo(c2.bits.toString());
        assertThat(dc1.refs.get(1).bits.toString()).isEqualTo(c4.bits.toString());
        assertThat(dc1.refs.get(0).refs.get(0).bits.toString()).isEqualTo(c3.bits.toString());
        assertThat(dc1.refs.get(0).refs.get(0).refs.get(0).bits.toString()).isEqualTo(c5.bits.toString());
    }

    @Test
    public void testBocDeserializationNew() {
        CellBuilder c2 = CellBuilder.beginCell();
        c2.storeUint(42, 7);
        byte[] serializedCell2 = c2.toBoc(true);

        log.info(c2.print());
        log.info(Utils.bytesToHex(serializedCell2));
        log.info(c2.bitStringToHex());

        assertThat(c2.bitStringToHex()).isEqualTo("55_");
        assertThat(c2.toBitString()).isEqualTo("0101010");
        assertThat(Utils.bytesToHex(serializedCell2)).isEqualTo("b5ee9c72410101010003000001558501ef11");

        Cell dc2 = Cell.fromBoc(serializedCell2);
        log.info("dc2 bitString {}", dc2.bits.toBitString());
        log.info("dc2 hex       {}", dc2.bits.toHex());
        assertThat(dc2.toBitString()).isEqualTo("0101010"); // bad 101010101
        assertThat(dc2.bitStringToHex()).isEqualTo("55_"); // bad 000155
    }

    @Test
    public void testCellUintsOld() {
        CellBuilder c2 = CellBuilder.beginCell();
        c2.storeUint(42, 7);
        byte[] serializedCell2 = c2.toBoc(true);

        log.info(c2.print());
        log.info(Utils.bytesToHex(serializedCell2));
        log.info(c2.bitStringToHex());

        assertThat(c2.bitStringToHex()).isEqualTo("55_");
        assertThat(c2.toBitString()).isEqualTo("0101010");
        assertThat(Utils.bytesToHex(serializedCell2)).isEqualTo("b5ee9c72410101010003000001558501ef11");

        Cell dc2 = Cell.fromBoc(serializedCell2);
        log.info("dc2 bitString {}", dc2.toBitString());
        log.info("dc2 hex       {}", dc2.toHex());
        assertThat(dc2.toBitString()).isEqualTo("0101010");
        assertThat(dc2.bitStringToHex()).isEqualTo("55_");
    }

    @Test
    public void testCellSerializationNew() {
        Cell c2 = CellBuilder.beginCell().storeUint(42, 7).endCell();
        log.info("c2 {}", c2.print());
        byte[] boc = c2.toBoc(true);
        log.info("boc {}", boc);

        Cell c = Cell.fromBoc(boc);
        log.info("int {}", CellSlice.beginParse(c).loadUint(7));
        assertThat(CellSlice.beginParse(c).loadUint(7)).isEqualTo(42);
    }
}
