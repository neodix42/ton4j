package org.ton.ton4j.tlb;

import java.io.Serializable;
import java.math.BigInteger;
import lombok.Builder;
import lombok.Data;
import org.ton.ton4j.cell.Cell;
import org.ton.ton4j.cell.CellBuilder;
import org.ton.ton4j.cell.CellSlice;

@Builder
@Data
public class WorkchainDescrV2 implements WorkchainDescr, Serializable {
  int workchain;
  long enabledSince;
  int actualMinSplit;
  int minSplit;
  int maxSplit;
  boolean basic;
  boolean active;
  boolean acceptMsgs;
  int flags;
  BigInteger zeroStateRootHash;
  BigInteger zeroStateFileHash;
  long version;
  WorkchainFormat format;
  WcSplitMergeTimings wcSplitMergeTimings;
  int persistentStateSplitDepth;

  public Cell toCell() {
    return CellBuilder.beginCell()
        .storeUint(0xa7, 8)
        .storeUint(enabledSince, 32)
        .storeUint(actualMinSplit, 8)
        .storeUint(minSplit, 8)
        .storeUint(maxSplit, 8)
        .storeBit(basic)
        .storeBit(active)
        .storeBit(acceptMsgs)
        .storeUint(flags, 13)
        .storeUint(zeroStateRootHash, 256)
        .storeUint(zeroStateFileHash, 256)
        .storeUint(version, 32)
        .storeCell(format.toCell(true))
        .storeCell(wcSplitMergeTimings.toCell())
        .storeUint(persistentStateSplitDepth, 8)
        .endCell();
  }

  public static WorkchainDescrV2 deserialize(CellSlice cs) {
    WorkchainDescrV2 workchainDescrV2 =
        WorkchainDescrV2.builder()
            .workchain(cs.loadUint(8).intValue())
            .enabledSince(cs.loadUint(32).intValue())
            .actualMinSplit(cs.loadUint(8).intValue())
            .minSplit(cs.loadUint(8).intValue())
            .maxSplit(cs.loadUint(8).intValue())
            .build();
    boolean basic = cs.loadBit();
    workchainDescrV2.setBasic(basic);
    workchainDescrV2.setActive(cs.loadBit());
    workchainDescrV2.setAcceptMsgs(cs.loadBit());
    workchainDescrV2.setZeroStateRootHash(cs.loadUint(256));
    workchainDescrV2.setZeroStateFileHash(cs.loadUint(256));
    workchainDescrV2.setVersion(cs.loadUint(32).intValue());
    workchainDescrV2.setFormat(WorkchainFormat.deserialize(cs, basic));
    workchainDescrV2.setWcSplitMergeTimings(WcSplitMergeTimings.deserialize(cs));
    workchainDescrV2.setPersistentStateSplitDepth(cs.loadUint(8).intValue());
    return workchainDescrV2;
  }
}
