package org.ton.java.tlb.types;

import lombok.Builder;
import lombok.Data;
import org.ton.java.cell.Cell;
import org.ton.java.cell.CellBuilder;
import org.ton.java.cell.CellSlice;

import java.math.BigInteger;

@Builder
@Data
public class WorkchainDescrV2 implements WorkchainDescr {
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
                .endCell();
    }

    public static WorkchainDescrV2 deserialize(CellSlice cs) {
        WorkchainDescrV2 workchainDescrV2 = WorkchainDescrV2.builder()
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
        return workchainDescrV2;
    }
}
