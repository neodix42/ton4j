package org.ton.ton4j.cell;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.ton.ton4j.address.Address;
import org.ton.ton4j.bitstring.BitString;
import org.ton.ton4j.utils.Utils;

@Slf4j
@RunWith(JUnit4.class)
public class TestCellSerialization {

  @Test
  public void testCellSerialization00() {
    Cell c0 = CellBuilder.beginCell().endCell();
    assertThat(Utils.bytesToHex(c0.hash()))
        .isEqualTo("96a296d224f285c67bee93c30f8a309157f0daa35dc5b87e410b78630a09cfc7");

    Cell c00 = CellBuilder.beginCell().storeUint(0, 2).endCell();
    assertThat(Utils.bytesToHex(c00.hash()))
        .isEqualTo("a1bb2a842d54edb8942f95bedaf53923d2d788d698232cfb256571e9e8b10a86");

    Cell cbs = CellBuilder.beginCell().storeUint(0x5f21, 15).endCell();
    assertThat(Utils.bytesToHex(cbs.hash()))
        .isEqualTo("7c617d77cf0f8561eb82bede8fcc95e728710b4c31510699eb78b5793fd8c6c2");
  }

  @Test
  public void testCellSerialization0() {

    Cell c1 = CellBuilder.beginCell().storeUint(42, 7).endCell();
    log.info("c1 {}", c1.toString());
    Cell c2 = CellBuilder.beginCell().storeUint(12, 8).storeRef(c1).endCell();
    Cell c3 = CellBuilder.beginCell().storeUint(13, 8).storeRef(c1).storeRef(c2).endCell();
    log.info("c1-hash: {}", Utils.bytesToHex(c1.hash()));
    log.info("c2-hash: {}", Utils.bytesToHex(c2.hash()));
    log.info("c3-hash: {}", Utils.bytesToHex(c3.hash()));
    assertThat(Utils.bytesToHex(c1.hash()))
        .isEqualTo("9184089c2c7fe2f12874575da31cf5d15ea91a3b7b5e41e910d4ccf935bf0a76");
    assertThat(Utils.bytesToHex(c2.hash()))
        .isEqualTo("13f640f9f30969b9f7b6d51a6ad277e719bc93c792c81e14cf9cddd7b387ff47");
    assertThat(Utils.bytesToHex(c3.hash()))
        .isEqualTo("6c2f0317132aad2b120968921bac0e3788b7588cc6ff470946e3ada3430d3338"); // ok

    log.info(Utils.bytesToHex(c2.hash()));
    log.info(Utils.bytesToHex(c2.toBoc()));
    log.info(Utils.bytesToHex(c2.toBoc(false)));
    log.info(Utils.bytesToHex(c3.toBoc(true, false)));
    assertThat(Utils.bytesToHex(c3.toBoc(true, false)))
        .isEqualTo("b5ee9c7241010301000c0002020d020101020c02000155921e09df");
    assertThat(Utils.bytesToHex(c3.toBoc(true, true)))
        .isEqualTo("b5ee9c72c1010301000c0005090c02020d020101020c02000155b647f116");
  }

  @Test
  public void testCellSerialization1() {
    Cell c0 = CellBuilder.beginCell().storeUint(17, 8).endCell();
    Cell c2 = CellBuilder.beginCell().storeUint(42, 7).storeRef(c0).endCell();
    Cell c3 = CellBuilder.beginCell().storeUint(73, 255).endCell();
    Cell c4 = CellBuilder.beginCell().storeUint((long) Math.pow(2, 42), 44).endCell();
    Cell c5 = CellBuilder.beginCell().storeUint((long) Math.pow(2, 41), 42).endCell();

    //        log.info(c0.print());
    log.info(Utils.bytesToHex(c0.hash()));
    log.info(Utils.bytesToHex(c0.toBoc(true)));
    log.info(Utils.bytesToHex(c0.toBoc(false)));

    //        log.info(c2.print());
    log.info(Utils.bytesToHex(c2.hash()));
    log.info(Utils.bytesToHex(c2.toBoc(true)));
    log.info(Utils.bytesToHex(c2.toBoc(false)));

    assertThat(c2.bitStringToHex()).isEqualTo("55_");

    log.info(Utils.bytesToHex(c3.toBoc(true)));
    assertThat(c3.bitStringToHex())
        .isEqualTo("0000000000000000000000000000000000000000000000000000000000000093_");

    assertThat(Utils.bytesToHex(c2.toBoc(true)))
        .isEqualTo("b5ee9c7241010201000700010155010002113256999a");
    assertThat(Utils.bytesToHex(c3.toBoc(true)))
        .isEqualTo(
            "b5ee9c7241010101002200003f000000000000000000000000000000000000000000000000000000000000009352a2f27c");
    assertThat(Utils.bytesToHex(c4.toBoc(true)))
        .isEqualTo("b5ee9c7241010101000800000b4000000000085566f674");

    Cell c1 =
        CellBuilder.beginCell()
            .storeUint(0, 8)
            .storeRef(c2)
            .storeRef(c3)
            .storeRef(c4)
            .storeRef(c5)
            .endCell();

    log.info("================= {}", c1.hash());
    log.info("================= {}", c1.print());
    log.info("================= {}", Utils.bytesToHex(c1.toBoc(true)));
    assertThat(Utils.bytesToHex(c1.toBoc(true)))
        .isEqualTo(
            "b5ee9c72410106010040000402000102030401015505003f0000000000000000000000000000000000000000000000000000000000000093000b400000000008000b8000000000200002115085e80e");
  }

  @Test
  public void testCellSerialization2() {
    CellBuilder c0 = CellBuilder.beginCell();
    c0.storeUint(73, 255);
    assertThat(c0.endCell().bitStringToHex())
        .isEqualTo("0000000000000000000000000000000000000000000000000000000000000093_");
    assertThat(c0.endCell().bitStringToHex())
        .isEqualTo("0000000000000000000000000000000000000000000000000000000000000093_");

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

    assertThat(c1.endCell().bitStringToHex()).isEqualTo("8000002_");
    assertThat(c2.endCell().bitStringToHex()).isEqualTo("8000000002_");
    assertThat(c3.endCell().bitStringToHex()).isEqualTo("80000000002_");
    assertThat(c4.endCell().bitStringToHex()).isEqualTo("40000000000");
  }

  @Test
  public void testCellSerialization3() {
    CellBuilder cb = CellBuilder.beginCell();
    //        c1.calculateHashes();

    long l1 = (long) Math.pow(2, 25);
    cb.storeUint(l1, 26);
    Cell c1 = cb.endCell();
    log.info("c1 bitString  {}", c1.toBitString());
    log.info("c1 hex        {}", c1.bitStringToHex());
    assertThat(c1.bitStringToHex()).isEqualTo("8000002_");

    log.info("hashes " + c1.getHashes().length);
    log.info("hashes(1) " + c1.getHashes()[0]);
    log.info("depth " + c1.getDepthLevels().length);
    log.info("hash old " + Utils.bytesToHex(c1.hash()));
    log.info("hash new " + Utils.bytesToHex(c1.getHash()));

    byte[] serializedCell1 = c1.toBoc(true);
    Cell dc1 = CellBuilder.beginCell().fromBoc(serializedCell1).endCell();
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
    Cell c5 =
        CellBuilder.beginCell()
            .storeAddress(
                Address.parseFriendlyAddress("0QAljlSWOKaYCuXTx2OCr9P08y40SC2vw3UeM1hYnI3gDY7I"))
            .endCell();

    log.info("c1 " + c1.toHex());
    log.info("c2 " + c2.toHex());
    log.info("c3 " + c3.toHex());
    log.info("c4 " + c4.toHex());
    log.info("c5 " + c5.toHex());

    assertThat(Utils.bytesToHex(c1.toBoc(true)))
        .isEqualTo("b5ee9c7241010101000600000780000020d742a310");
    assertThat(Utils.bytesToHex(c2.toBoc(true)))
        .isEqualTo("b5ee9c72410101010007000009800000000245bf8536");
    assertThat(Utils.bytesToHex(c3.toBoc(true)))
        .isEqualTo("b5ee9c7241010101000800000b800000000020795c9ecc");
    assertThat(Utils.bytesToHex(c4.toBoc(true)))
        .isEqualTo("b5ee9c7241010101000800000b4000000000085566f674");
    assertThat(Utils.bytesToHex(c5.toBoc(true)))
        .isEqualTo(
            "b5ee9c724101010100240000438004b1ca92c714d3015cba78ec7055fa7e9e65c68905b5f86ea3c66b0b1391bc01b0dedc73c3");

    assertThat(c1.bits.toHex()).isEqualTo("8000002_");

    Cell c3_more = CellBuilder.beginCell().storeCell(c3).storeRef(c5).endCell();
    Cell c2_more = CellBuilder.beginCell().storeCell(c2).storeRef(c3_more).endCell();
    Cell c1_more = CellBuilder.beginCell().storeCell(c1).storeRef(c2_more).storeRef(c4).endCell();
    //        Cell c1_more_2 = CellBuilder.beginCell().storeCell(c1).storeRef(c4).endCell();

    // c1.refs.add(c4);

    log.info("boc hashes {}", c1_more.getHashes().length);
    log.info("boc depths {}", c1_more.getDepthLevels()[0]);

    log.info("c1_more " + c1_more.toHex());
    log.info("c2_more " + c2_more.toHex());
    log.info("c3_more " + c3_more.toHex());
    log.info("c4 " + c4.toHex());
    log.info("c5 " + c5.toHex());

    log.info("c1_more hex: {}", Utils.bytesToHex(c1_more.toBoc(true)));
    assertThat(Utils.bytesToHex(c1_more.toBoc(true)))
        .isEqualTo(
            "b5ee9c724101050100450002078000002001020109800000000203000b400000000008010b8000000000200400438004b1ca92c714d3015cba78ec7055fa7e9e65c68905b5f86ea3c66b0b1391bc01b00e6fe71e");

    byte[] serializedCell1 = c1.toBoc(true);
    log.info("c1 hex {}", Utils.bytesToHex(serializedCell1));
    Cell dc1 = CellBuilder.beginCell().fromBoc(serializedCell1).endCell();
    log.info("c1 deserialized");
    log.info(dc1.print());
    log.info("dc1 hex: {}", Utils.bytesToHex(dc1.toBoc(true)));

    assertThat(Utils.bytesToHex(serializedCell1)).isEqualTo(Utils.bytesToHex(dc1.toBoc(true)));
    assertThat(Utils.bytesToHex(serializedCell1)).isEqualTo(dc1.toHex(true));
    assertThat(serializedCell1).isEqualTo(dc1.toBoc(true));

    Cell c6 = CellBuilder.beginCell().storeCell(c1).endCell();
    assertThat(Utils.bytesToHex(serializedCell1)).isEqualTo(c6.toHex(true));
    assertThat(serializedCell1).isEqualTo(c6.toBoc(true));
    assertThat(c1.toBoc(true)).isEqualTo(dc1.toBoc(true));
    assertThat(c1.toBoc(true))
        .isEqualTo(CellBuilder.beginCell().fromBoc(dc1.toBoc(true)).endCell().toBoc(true));

    byte[] serializedCell2 = c1_more.toBoc(true);
    log.info("serializedCell2 hex {}", Utils.bytesToHex(serializedCell1));
    Cell dc2 = CellBuilder.beginCell().fromBoc(serializedCell2).endCell();
    assertThat(CellSlice.beginParse(dc2).loadRef().bits.toString()).isEqualTo(c2.bits.toString());
    assertThat(dc2.refs.get(1).bits.toString()).isEqualTo(c4.bits.toString());
    assertThat(dc2.refs.get(0).refs.get(0).bits.toString()).isEqualTo(c3.bits.toString());
    assertThat(dc2.refs.get(0).refs.get(0).refs.get(0).bits.toString())
        .isEqualTo(c5.bits.toString());
  }

  @Test
  public void testBocDeserializationNew() {
    CellBuilder cb = CellBuilder.beginCell();
    cb.storeUint(42, 7);
    Cell c2 = cb.endCell();
    byte[] serializedCell2 = c2.toBoc(true);

    log.info(c2.print());
    log.info(Utils.bytesToHex(serializedCell2));
    log.info(c2.bitStringToHex());

    assertThat(c2.bitStringToHex()).isEqualTo("55_");
    assertThat(c2.toBitString()).isEqualTo("0101010");
    assertThat(Utils.bytesToHex(serializedCell2)).isEqualTo("b5ee9c72410101010003000001558501ef11");

    Cell dc2 = CellBuilder.beginCell().fromBoc(serializedCell2).endCell();
    log.info("dc2 bitString {}", dc2.bits.toBitString());
    log.info("dc2 hex       {}", dc2.bits.toHex());
    assertThat(dc2.toBitString()).isEqualTo("0101010"); // bad 101010101
    assertThat(dc2.bitStringToHex()).isEqualTo("55_"); // bad 000155
  }

  @Test
  public void testCellUintsOld() {
    CellBuilder cb = CellBuilder.beginCell();
    cb.storeUint(42, 7);
    Cell c2 = cb.endCell();
    byte[] serializedCell2 = c2.toBoc(true);

    log.info(c2.print());
    log.info(Utils.bytesToHex(serializedCell2));
    log.info(c2.bitStringToHex());

    assertThat(c2.bitStringToHex()).isEqualTo("55_");
    assertThat(c2.toBitString()).isEqualTo("0101010");
    assertThat(Utils.bytesToHex(serializedCell2)).isEqualTo("b5ee9c72410101010003000001558501ef11");

    Cell dc2 = CellBuilder.beginCell().fromBoc(serializedCell2).endCell();
    log.info("dc2 bitString {}", dc2.toBitString());
    log.info("dc2 hex       {}", dc2.toHex());
    assertThat(dc2.toBitString()).isEqualTo("0101010");
    assertThat(dc2.bitStringToHex()).isEqualTo("55_");
  }

  @Test
  public void testCellSerializationNew() {
    BitString bs = new BitString(2);
    bs.writeBits("01");

    Cell cell =
        CellBuilder.beginCell()
            .storeBitString(bs)
            .storeUint(5, 32)
            .storeUint(6, 31)
            .storeBit(true)
            .endCell();
    log.info("cell1 {}", cell.print());
    byte[] boc = cell.toBoc(true);
    log.info("boc {}", boc);
    log.info("boc {}", cell.toHex(true));
    log.info("boc hash {}", Utils.bytesToHex(cell.getHash()));
    log.info("boc hashes {}", cell.getHashes().length);
    log.info("boc depths {}", cell.getDepthLevels()[0]);
    assertThat(StringUtils.trim(cell.print())).isEqualTo("x{40000001400000036_}");
    assertThat(cell.toHex(true)).isEqualTo("b5ee9c7241010101000b0000114000000140000003603c39fda2");
  }

  @Test
  public void testCellSerializationWithRef() {

    Cell cell1 =
        CellBuilder.beginCell()
            .storeBytes(new byte[] {65, 66, 67})
            .storeUint(
                new BigInteger(
                    "538bd272cc81b9d5f470a18a946cbb8fb621ca57593836014e0f12fd5d34942f", 16),
                256)
            .storeString("test sdk")
            .endCell();

    Cell cell2 =
        CellBuilder.beginCell()
            .storeInt(
                new BigInteger(
                    "568E7E73CDA9C3D5FF8641E77EED4EE65EDB702EA100290F2E7A043771C9CA5A", 16),
                256)
            .storeCoins(Utils.toNano("2.56"))
            .storeAddress(Address.of("0QAljlSWOKaYCuXTx2OCr9P08y40SC2vw3UeM1hYnI3gDY7I"))
            .storeRef(cell1)
            .endCell();

    log.info("cell2 {}", cell2.print());
    byte[] boc = cell2.toBoc(true);
    log.info("boc {}", cell2.toHex(true));
    log.info("boc {}", cell2.toHex(false, true));
    log.info("boc {}", cell2.toHex(true, true));
    log.info("boc hash {}", Utils.bytesToHex(cell2.getHash()));
    log.info("boc hashes {}", cell2.getHashes().length);
    log.info("boc depths {}", cell2.getDepthLevels()[0]);
    assertThat(StringUtils.trim(cell2.print()))
        .isEqualTo(
            "x{568E7E73CDA9C3D5FF8641E77EED4EE65EDB702EA100290F2E7A043771C9CA5A4989680008004B1CA92C714D3015CBA78EC7055FA7E9E65C68905B5F86EA3C66B0B1391BC01B_}\n"
                + " x{414243538BD272CC81B9D5F470A18A946CBB8FB621CA57593836014E0F12FD5D34942F746573742073646B}");
    assertThat(cell2.toHex(true, false))
        .isEqualTo(
            "b5ee9c7241010201007600018b568e7e73cda9c3d5ff8641e77eed4ee65edb702ea100290f2e7a043771c9ca5a4989680008004b1ca92c714d3015cba78ec7055fa7e9e65c68905b5f86ea3c66b0b1391bc01b010056414243538bd272cc81b9d5f470a18a946cbb8fb621ca57593836014e0f12fd5d34942f746573742073646bd55ad0db");
    assertThat(cell2.toHex(false, true))
        .isEqualTo(
            "b5ee9c72810102010076004976018b568e7e73cda9c3d5ff8641e77eed4ee65edb702ea100290f2e7a043771c9ca5a4989680008004b1ca92c714d3015cba78ec7055fa7e9e65c68905b5f86ea3c66b0b1391bc01b010056414243538bd272cc81b9d5f470a18a946cbb8fb621ca57593836014e0f12fd5d34942f746573742073646b");
    assertThat(cell2.toHex(true, true))
        .isEqualTo(
            "b5ee9c72c10102010076004976018b568e7e73cda9c3d5ff8641e77eed4ee65edb702ea100290f2e7a043771c9ca5a4989680008004b1ca92c714d3015cba78ec7055fa7e9e65c68905b5f86ea3c66b0b1391bc01b010056414243538bd272cc81b9d5f470a18a946cbb8fb621ca57593836014e0f12fd5d34942f746573742073646b27f47afd");
  }

  @Test
  public void testCellDepthBitsRefsDescriptor() {
    Cell c1 = CellBuilder.beginCell().storeBit(true).endCell();
    Cell c2 = CellBuilder.beginCell().storeBytes(new int[] {23}).storeRef(c1).endCell();
    Cell c3 = CellBuilder.beginCell().storeAddress(null).storeRef(c2).endCell(); // addr_none$00
    Cell c4 = CellBuilder.beginCell().storeCoins(BigInteger.ZERO).storeRef(c3).endCell();
    Cell c5 =
        CellBuilder.beginCell()
            .storeBytes(new int[] {45, 67})
            .storeRef(c4)
            .storeRef(c3)
            .storeRef(c2)
            .storeRef(c1)
            .endCell();
    log.info("print {}", c5.print());
    log.info("boc {}", c5.toHex(true));
    log.info("boc {}", Utils.bytesToHex(c5.getHash()));
    log.info("depth {}", c5.getDepthLevels()[0]);
    log.info("refsDescriptor {}", Utils.bytesToHex(c5.getRefsDescriptor(0)));
    log.info("bitsDescriptor {}", Utils.bytesToHex(c5.getBitsDescriptor()));
  }

  @Test
  public void testPrunedBranchCell() {
    Cell c =
        CellBuilder.beginCell()
            .storeUint(1, 8) // Pruned Cell Type
            .storeUint(1, 8)
            .storeUint(123456, 256)
            .storeUint(1, 16)
            .endCell();

    log.info("c levelMask {}, maxLevel {}", c.resolveMask(), c.getMaxLevel());

    Cell pb =
        new Cell(c.getBits(), c.getBits().getLength(), c.getRefs(), true, CellType.PRUNED_BRANCH);
    pb.calculateHashes();
    log.info("pb levelMask {}, maxLevel {}", pb.resolveMask(), pb.getMaxLevel());

    Cell pbm =
        CellBuilder.beginCell().fromBoc(c.toHex()).cellType(CellType.PRUNED_BRANCH).endCell();
    log.info("pbm levelMask {}, maxLevel {}", pbm.resolveMask(), pbm.getMaxLevel());

    Cell cc = CellBuilder.beginCell().storeUint(1, 1).storeRef(pbm).endCell();

    log.info("cc levelMask {}, maxLevel {}", cc.resolveMask(), cc.getMaxLevel());
    log.info("cc {}", Cell.fromBoc(cc.toHex(true, true)).print());
    log.info("cc levelMask {}, maxLevel {}", cc.resolveMask(), cc.getMaxLevel());
  }

  @Test
  public void testMerkleProofCell() {
    Cell mc = CellBuilder.beginCell().storeUint(1, 1).endCell();
    log.info("child level {}", mc.resolveMask());

    Cell c =
        CellBuilder.beginCell()
            .storeUint(3, 8) // Merkle Proof Cell Type
            .storeBytes(mc.getHash())
            .storeUint(mc.getDepthLevels()[0], 16)
            .storeRef(mc)
            .endCell();
    Cell pb = new Cell(c.getBits(), c.getBitLength(), c.getRefs(), true, CellType.MERKLE_PROOF);
    pb.calculateHashes();
    log.info("merkle proof levelMask {}, maxLevel {}", pb.resolveMask(), pb.getMaxLevel());

    Cell pbm = CellBuilder.beginCell().fromBoc(c.toHex()).cellType(CellType.MERKLE_PROOF).endCell();
    log.info("pbm levelMask {}, maxLevel {}", pbm.resolveMask(), pbm.getMaxLevel());

    Cell cc = CellBuilder.beginCell().storeUint(1, 1).storeRef(pb).endCell();
    log.info("cc {}", cc.toHex());
    log.info("cc levelMask {}, maxLevel {}", cc.resolveMask(), cc.getMaxLevel());
  }

  @Ignore
  @Test
  public void testMultiRootCellDeSerialization() {
    Cell c1 = CellBuilder.beginCell().storeUint(42, 7).endCell();
    Cell c2 = CellBuilder.beginCell().storeUint(12, 8).storeRef(c1).endCell();
    Cell c3 = CellBuilder.beginCell().storeUint(13, 8).storeRef(c1).storeRef(c2).endCell();
    byte[] bocWithRoots =
        new Cell().toBocMultiRoot(Arrays.asList(c1, c2, c3), true, true, false, false, false);
    String bocWithRootsHex = Utils.bytesToHex(bocWithRoots);
    List<Cell> cellWithRoots = Cell.fromBocMultiRoot(bocWithRoots);
    log.info("cell with size {}", cellWithRoots.size());
    log.info("cell with roots {}", cellWithRoots.size());
  }
}
