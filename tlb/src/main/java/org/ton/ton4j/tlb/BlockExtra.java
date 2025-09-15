package org.ton.ton4j.tlb;

import static java.util.Objects.isNull;

import java.io.Serializable;
import java.math.BigInteger;
import lombok.Builder;
import lombok.Data;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.ton.ton4j.cell.Cell;
import org.ton.ton4j.cell.CellBuilder;
import org.ton.ton4j.cell.CellSlice;

/**
 *
 *
 * <pre>
 * block_extra
 * in_msg_descr:^InMsgDescr
 * out_msg_descr:^OutMsgDescr
 * account_blocks:^ShardAccountBlocks
 * rand_seed:bits256
 * created_by:bits256
 * custom:(Maybe ^McBlockExtra) = BlockExtra;
 * </pre>
 */
@Builder
@Data
public class BlockExtra implements Serializable {
  InMsgDescr inMsgDesc;
  OutMsgDescr outMsgDesc;
  ShardAccountBlocks shardAccountBlocks;
  public BigInteger randSeed;
  public BigInteger createdBy;
  McBlockExtra mcBlockExtra;

  private String getRandSeed() {
    return randSeed.toString(16);
  }

  private String getCreatedBy() {
    return createdBy.toString(16);
  }

  public String toJson() {
    return ToStringBuilder.reflectionToString(this, ToStringStyle.JSON_STYLE);
  }

  public Cell toCell() {
    return CellBuilder.beginCell()
        .storeRef(inMsgDesc.toCell())
        .storeRef(outMsgDesc.toCell())
        .storeRef(shardAccountBlocks.toCell())
        .storeUint(randSeed, 256)
        .storeUint(createdBy, 256)
        .storeRefMaybe(isNull(mcBlockExtra) ? null : mcBlockExtra.toCell())
        .endCell();
  }

  public static BlockExtra deserialize(CellSlice cs) {
    if (cs.isExotic()) {
      return null;
    }
    long magic = cs.loadUint(32).longValue();
    assert (magic == 0x4a33f6fdL)
        : "Block: magic not equal to 0x4a33f6fdL, found 0x" + Long.toHexString(magic);

    InMsgDescr inMsgDescr = InMsgDescr.deserialize(CellSlice.beginParse(cs.loadRef()));
    OutMsgDescr outMsgDescr = OutMsgDescr.deserialize(CellSlice.beginParse(cs.loadRef()));
    ShardAccountBlocks shardAccountBlocks =
        ShardAccountBlocks.deserialize(CellSlice.beginParse(cs.loadRef()));
    BlockExtra blockExtra =
        BlockExtra.builder()
            .inMsgDesc(inMsgDescr)
            .outMsgDesc(outMsgDescr)
            .shardAccountBlocks(shardAccountBlocks)
            .randSeed(cs.loadUint(256))
            .createdBy(cs.loadUint(256))
            .build();

    blockExtra.setMcBlockExtra(
        cs.loadBit() ? McBlockExtra.deserialize(CellSlice.beginParse(cs.loadRef())) : null);

    return blockExtra;
  }
}
