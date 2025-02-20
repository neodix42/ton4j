package org.ton.java.tlb;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.Data;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.apache.commons.lang3.tuple.Pair;
import org.ton.java.cell.Cell;
import org.ton.java.cell.CellBuilder;
import org.ton.java.cell.CellSlice;
import org.ton.java.cell.TonHashMapAugE;

/**
 *
 *
 * <pre>
 * block_extra
 * in_msg_descr:^InMsgDescr
 * out_msg_descr:^OutMsgDescr
 * account_blocks:^ShardAccountBlocks // _ (HashmapAugE 256 AccountBlock CurrencyCollection) = ShardAccountBlocks;
 * rand_seed:bits256
 * created_by:bits256
 * custom:(Maybe ^McBlockExtra) = BlockExtra;
 * </pre>
 */
@Builder
@Data
public class BlockExtra {
  InMsgDescr inMsgDesc;
  OutMsgDescr outMsgDesc;
  TonHashMapAugE shardAccountBlocks;
  BigInteger randSeed;
  BigInteger createdBy;
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
        .storeRef(
            shardAccountBlocks.serialize(
                k -> CellBuilder.beginCell().storeUint((Long) k, 256).endCell().getBits(),
                v -> CellBuilder.beginCell().storeCell(((AccountBlock) v).toCell()),
                e -> CellBuilder.beginCell().storeCell(((CurrencyCollection) e).toCell()),
                (fk, fv) -> CellBuilder.beginCell().storeUint(((Long) fk) + ((Long) fv), 32)))
        .storeUint(randSeed, 256)
        .storeUint(createdBy, 256)
        .storeRefMaybe(mcBlockExtra.toCell())
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
    BlockExtra blockExtra =
        BlockExtra.builder()
            .inMsgDesc(inMsgDescr)
            .outMsgDesc(outMsgDescr)
            .shardAccountBlocks(
                CellSlice.beginParse(cs.loadRef())
                    .loadDictAugE(
                        256,
                        k -> k.readUint(256),
                        v -> v, // AccountBlock.deserialize(v),
                        e -> e)) // CurrencyCollection.deserialize(e)))
            .randSeed(cs.loadUint(256))
            .createdBy(cs.loadUint(256))
            .build();

    blockExtra.setMcBlockExtra(
        cs.loadBit() ? McBlockExtra.deserialize(CellSlice.beginParse(cs.loadRef())) : null);

    return blockExtra;
  }

  public List<AccountBlock> getShardAccountBlocksAsList() {
    List<AccountBlock> accountBlocks = new ArrayList<>();
    for (Map.Entry<Object, Pair<Object, Object>> entry : shardAccountBlocks.elements.entrySet()) {
      accountBlocks.add((AccountBlock) entry.getValue().getLeft());
    }
    return accountBlocks;
  }
}
