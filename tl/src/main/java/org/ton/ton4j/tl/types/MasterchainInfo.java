package org.ton.ton4j.tl.types;

import java.math.BigInteger;
import lombok.Builder;
import lombok.Data;
import org.ton.ton4j.cell.Cell;
import org.ton.ton4j.cell.CellBuilder;
import org.ton.ton4j.cell.CellSlice;

@Builder
@Data
/**
 * liteServer.masterchainInfo last:tonNode.blockIdExt state_root_hash:int256
 * init:tonNode.zeroStateIdExt = liteServer.MasterchainInfo;
 */
public class MasterchainInfo {
  BlockIdExt last;
  BigInteger state_root_hash;
  ZeroStateIdExt init;

  public Cell toCell() {
    return CellBuilder.beginCell()
        .storeCell(last.toCell())
        .storeUint(state_root_hash, 256)
        .storeCell(init.toCell())
        .endCell();
  }

  public static MasterchainInfo deserialize(CellSlice cs) {
    MasterchainInfo blockIdExt =
        MasterchainInfo.builder()
            .last(BlockIdExt.deserialize(cs))
            .state_root_hash(cs.loadUint(256))
            .init(ZeroStateIdExt.deserialize(cs))
            .build();
    return blockIdExt;
  }
}
