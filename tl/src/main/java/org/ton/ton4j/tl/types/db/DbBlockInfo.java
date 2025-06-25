package org.ton.ton4j.tl.types.db;

import java.io.Serializable;
import java.math.BigInteger;
import lombok.Builder;
import lombok.Data;
import org.ton.ton4j.cell.CellSlice;
import org.ton.ton4j.tl.liteserver.responses.BlockIdExt;
import org.ton.ton4j.tl.liteserver.responses.LiteServerAnswer;
import org.ton.ton4j.utils.Utils;

/**
 *
 *
 * <pre>
 * ton_api.tl
 * db.block.info#4ac6e727
 *  id:tonNode.blockIdExt
 *  flags:# prev_left:flags.1?tonNode.blockIdExt
 *  prev_right:flags.2?tonNode.blockIdExt
 *  next_left:flags.3?tonNode.blockIdExt
 *  next_right:flags.4?tonNode.blockIdExt
 *  lt:flags.13?long
 *  ts:flags.14?int
 *  state:flags.17?int256
 *  masterchain_ref_seqno:flags.23?int = db.block.Info;
 *  </pre>
 */
@Builder
@Data
public class DbBlockInfo implements Serializable, LiteServerAnswer {

  public static final long DB_BLOCK_INFO_MAGIC = 0x4ac6e727;

  long magic;
  BlockIdExt id;
  BigInteger flags;
  BlockIdExt prevLeft;
  BlockIdExt prevRight;
  BlockIdExt nextLeft;
  BlockIdExt nextRight;
  BigInteger lt;
  BigInteger ts;
  byte[] state;
  BigInteger masterChainRefSeqNo;

  private String getMagic() {
    return Long.toHexString(magic);
  }

  private String getState() {
    return Utils.bytesToHex(state);
  }

  public static DbBlockInfo deserialize(CellSlice cs) {
    return null; // todo
    //    int magic = Integer.reverseBytes(cs.loadUint(32).intValue());
    //    assert (magic == DB_BLOCK_INFO_MAGIC)
    //        : "DbBlockInfo: magic not equal to 0x4ac6e727, found " + Long.toHexString(magic);
    //    DbBlockInfo dbBlockInfo =
    //
    // DbBlockInfo.builder().magic(DB_BLOCK_INFO_MAGIC).id(BlockIdExt.deserialize(cs)).build();
    //    int f = Integer.reverseBytes(cs.loadUint(32).intValue());
    //    BigInteger flags = BigInteger.valueOf(f);
    //
    //    // todo improve little endian reading
    //    dbBlockInfo.setFlags(flags);
    //    dbBlockInfo.setPrevLeft(flags.testBit(1) ? BlockIdExt.deserialize(cs) : null);
    //    dbBlockInfo.setPrevRight(flags.testBit(2) ? BlockIdExt.deserialize(cs) : null);
    //    dbBlockInfo.setNextLeft(flags.testBit(3) ? BlockIdExt.deserialize(cs) : null);
    //    dbBlockInfo.setNextRight(flags.testBit(4) ? BlockIdExt.deserialize(cs) : null);
    //    dbBlockInfo.setLt(
    //        flags.testBit(13)
    //            ? BigInteger.valueOf(Long.reverseBytes(cs.loadUint(64).longValue()))
    //            : null);
    //    dbBlockInfo.setTs(
    //        flags.testBit(14)
    //            ? BigInteger.valueOf(Integer.reverseBytes(cs.loadUint(32).intValue()))
    //            : null);
    //    dbBlockInfo.setState(flags.testBit(17) ? Utils.reverseByteArray(cs.loadBytes(256)) :
    // null);
    //    dbBlockInfo.setMasterChainRefSeqNo(
    //        flags.testBit(23)
    //            ? BigInteger.valueOf(Integer.reverseBytes(cs.loadUint(32).intValue()))
    //            : null);
    //    return dbBlockInfo;
  }
}
