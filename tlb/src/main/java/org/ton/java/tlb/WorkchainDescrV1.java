package org.ton.java.tlb;

import java.math.BigInteger;
import lombok.Builder;
import lombok.Data;
import org.ton.java.cell.Cell;
import org.ton.java.cell.CellBuilder;
import org.ton.java.cell.CellSlice;

@Builder
@Data
public class WorkchainDescrV1 implements WorkchainDescr {
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


    public Cell toCell() {
        return CellBuilder.beginCell()
                .storeUint(0xa6, 8)
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
                .endCell();
    }

    public static WorkchainDescrV1 deserialize(CellSlice cs) {
        WorkchainDescrV1 workchainDescrV1 = WorkchainDescrV1.builder()
                .workchain(cs.loadUint(8).intValue())
                .enabledSince(cs.loadUint(32).intValue())
                .actualMinSplit(cs.loadUint(8).intValue())
                .minSplit(cs.loadUint(8).intValue())
                .maxSplit(cs.loadUint(8).intValue())
                .build();
        boolean basic = cs.loadBit();
        workchainDescrV1.setBasic(basic);
        workchainDescrV1.setActive(cs.loadBit());
        workchainDescrV1.setAcceptMsgs(cs.loadBit());
        workchainDescrV1.setZeroStateRootHash(cs.loadUint(256));
        workchainDescrV1.setZeroStateFileHash(cs.loadUint(256));
        workchainDescrV1.setVersion(cs.loadUint(32).intValue());
        workchainDescrV1.setFormat(WorkchainFormat.deserialize(cs, basic));
        return workchainDescrV1;
    }
}
