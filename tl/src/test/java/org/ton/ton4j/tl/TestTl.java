package org.ton.ton4j.tl;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.ton.ton4j.bitstring.BitString;
import org.ton.ton4j.cell.ByteReader;
import org.ton.ton4j.cell.Cell;
import org.ton.ton4j.cell.CellBuilder;
import org.ton.ton4j.cell.CellSlice;
import org.ton.ton4j.tl.types.db.block.BlockInfo;
import org.ton.ton4j.tlb.*;
import org.ton.ton4j.utils.Utils;

@Slf4j
@RunWith(JUnit4.class)
public class TestTl {
  /**
   * hex data taken from rocksdb table archive.00000.index value column
   *
   * <p>db.block.info#4ac6e727 id:tonNode.blockIdExt flags:# prev_left:flags.1?tonNode.blockIdExt
   * prev_right:flags.2?tonNode.blockIdExt next_left:flags.3?tonNode.blockIdExt
   * next_right:flags.4?tonNode.blockIdExt lt:flags.13?long ts:flags.14?int state:flags.17?int256
   * masterchain_ref_seqno:flags.23?int = db.block.Info;
   */
  @Test
  public void testRocksDbBlockInfoParsing() {
    byte[] rawValue =
        Utils.hexToSignedBytes(
            "27e7c64a0000000000000000000000404c000000c85856c6d9dd02c1c3d9881fbd88a1d3dc14f9e8e6e99f79ec33f84cae475cf8149bd099c820a51529e3fcb8d4f5bc908d3cee250ac31640fa8ab579ae5c8844aaf2d6080000000000000000000000804b000000278adf0a7f59da0e9530164f3e42930965ce44b276c86b67363463c54a6c07829963b6bc76af71a5be6c65b64dfc55154019eaa99157a300efa842fb8aabf7730000000000000000000000404d0000003fc2b02d46c1796066e140392bfbbdc63227367a3e8d35e20748db47040645b571c85e97d60ddc1f208793062642b39fbfc7f0dbaf35c905b651369d74782670c19ee60500000000cb7ca5659d4925c426aed39b2ae99e971cb19b3fbbab27b4887c04211c8a57e2505fc9835d000000");
    ByteReader valueReader = new ByteReader(rawValue);
    String valueMagicId = Utils.bytesToHex(getBytes(rawValue, 0, 4));
    if (valueMagicId.equals("27e7c64a")) { // this is db.block.info#4ac6e727 in ton_api.tl line #471
      log.info("db.block.info#4ac6e727: {}", Utils.bytesToHex(getBytes(rawValue, 0)));

      BitString bs2 = new BitString(valueReader.readBytes());
      Cell c2 = CellBuilder.beginCell().storeBitStringUnsafe(bs2).endCell();
      CellSlice cs = CellSlice.beginParse(c2);
      BlockInfo dbBlockInfo = BlockInfo.deserialize(cs);

      log.info("dbBlockInfo {}", dbBlockInfo);
    }
  }

  @Test
  public void testRocksDbBlockInfoParsing2() {
    byte[] rawValue =
        Utils.hexToSignedBytes(
            "27e7c64a0000000000000000000000804b000000278adf0a7f59da0e9530164f3e42930965ce44b276c86b67363463c54a6c07829963b6bc76af71a5be6c65b64dfc55154019eaa99157a300efa842fb8aabf773faf2d6080000000000000000000000804a0000001eb220d5b0bcd430b14386cc629d5d5f196df2a3978f34d43a0e1bd0cdc1d8259b3e3620c7985ff0b86535551f2fdf2dacccea04cc1cae33a9bcba8faddf680b0000000000000000000000404c000000c85856c6d9dd02c1c3d9881fbd88a1d3dc14f9e8e6e99f79ec33f84cae475cf8149bd099c820a51529e3fcb8d4f5bc908d3cee250ac31640fa8ab579ae5c88440000000000000000000000c04c000000f28bba3d68ce1b0da03bde7cf6f7dbb2bce2332eff0b5c44ab7c57000f9a76d29b8f82e37f202f65cbb18656d44704414ec59025aba1df7a5cf0ab129e15d9f541118b0500000000c47ca5653032689210e75872eb9817b00e7ff4f24383db218dafcddfffdbad281625026656000000");
    ByteReader valueReader = new ByteReader(rawValue);
    String valueMagicId = Utils.bytesToHex(getBytes(rawValue, 0, 4));
    if (valueMagicId.equals("27e7c64a")) { // this is db.block.info#4ac6e727 in ton_api.tl line #471
      log.info("db.block.info#4ac6e727: {}", Utils.bytesToHex(getBytes(rawValue, 0)));

      BitString bs2 = new BitString(valueReader.readBytes());
      Cell c2 = CellBuilder.beginCell().storeBitStringUnsafe(bs2).endCell();
      CellSlice cs = CellSlice.beginParse(c2);
      BlockInfo dbBlockInfo = BlockInfo.deserialize(cs);

      log.info("dbBlockInfo {}", dbBlockInfo);
    }
  }

  @Test
  public void testRocksDbBlockInfoParsing3() {
    byte[] rawValue =
        Utils.hexToSignedBytes(
            "27e7c64affffffff000000000000008008010000155ae03cef502e0c8bcf8b7178b81a9445c00be3cf687361ded48db4d502f5451ae4c49a57ba39fac697bba2cc10ee14aa80e1eb062b99b60bd7188d15d118eda3ea5608ffffffff0000000000000080070100007964a2aab72253115ae1167acdf4b1e11fe8c5a7e647984fb0c09f0051c00ac34291e10bbda1d99e5dcfe117fcc1e4b0f41339c5d2e4251393f2d64eabc046a304ac1e1200000000a37da565db3eff18afd14ef1a7e570fa0e269f788815225875ee5d2936933988947fab15");
    ByteReader valueReader = new ByteReader(rawValue);
    String valueMagicId = Utils.bytesToHex(getBytes(rawValue, 0, 4));
    if (valueMagicId.equals("27e7c64a")) { // this is db.block.info#4ac6e727 in ton_api.tl line #471
      log.info("db.block.info#4ac6e727: {}", Utils.bytesToHex(getBytes(rawValue, 0)));

      BitString bs2 = new BitString(valueReader.readBytes());
      Cell c2 = CellBuilder.beginCell().storeBitStringUnsafe(bs2).endCell();
      CellSlice cs = CellSlice.beginParse(c2);
      BlockInfo dbBlockInfo = BlockInfo.deserialize(cs);

      log.info("dbBlockInfo {}", dbBlockInfo);
    }
  }

  @Test
  public void testRocksDbFiles1() throws IOException {
    InputStream pack =
        getClass().getClassLoader().getResourceAsStream("rocksdb/archive.00000.pack");
    byte[] b = IOUtils.toByteArray(pack);

    ByteReader r = new ByteReader(b);
    int packageHeaderMagic = r.readIntLittleEndian(); // 32 - 0xae8fdd01

    if (packageHeaderMagic != 0xae8fdd01) {
      log.error("wrong packageHeaderMagic, should 0xae8fdd01");
      return;
    }

    do {
      short entryHeaderMagic = r.readShortLittleEndian(); // 16 - 0x1e8b

      if (entryHeaderMagic != 0x1e8b) {
        log.error("wrong entryHeaderMagic, should 0x1e8b");
        return;
      }
      int filenameLength = r.readShortLittleEndian(); // 16
      int bocSize = r.readIntLittleEndian(); // 32
      String filename = new String(Utils.unsignedBytesToSigned(r.readBytes(filenameLength)));

      log.info("");
      log.info("bocSize {}, filename {}", bocSize, filename);

      int[] boc = r.readBytes(bocSize);
      log.info("boc: {}", Utils.bytesToHex(boc));
      Cell c = CellBuilder.beginCell().fromBoc(boc).endCell();
      if (c.getBits().preReadUint(8).longValue() == 0xc3) {
        // c.bits.readUint(8);
        BlockProof blockProof =
            BlockProof.deserialize(CellSlice.beginParse(c)); // block tlb magic 11ef55aa
        log.info("skip proof block: {}", blockProof);
        continue;
      }

      Cell blockCell = getFirstCellWithBlock(c);

      Block block = Block.deserialize(CellSlice.beginParse(blockCell)); // block tlb magic 11ef55aa
      log.info(
          "inMsg {}, outMsg {}, account blocks {}, block {}",
          block.getExtra().getInMsgDesc().getCount(),
          block.getExtra().getOutMsgDesc().getCount(),
          block.getExtra().getShardAccountBlocks().elements.size(),
          block);

    } while (r.getDataSize() != 0);
  }

  private Cell getFirstCellWithBlock(Cell c) {

    long blockMagic = c.getBits().preReadUint(32).longValue();
    if (blockMagic == 0x11ef55aa) {
      return c;
    }

    int i = 0;
    for (Cell ref : c.getRefs()) {
      return getFirstCellWithBlock(ref);
    }

    return null;
  }

  private byte[] getBytes(byte[] src, int from, int length) {
    if (from + length > src.length) {
      return new byte[0];
    }
    return Arrays.copyOfRange(src, from, from + length);
  }

  private byte[] getBytes(byte[] src, int from) {
    if (from > src.length) {
      return new byte[0];
    }
    return Arrays.copyOfRange(src, from, src.length);
  }
}
