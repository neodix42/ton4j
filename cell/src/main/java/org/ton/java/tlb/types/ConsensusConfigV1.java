package org.ton.java.tlb.types;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.ton.java.cell.Cell;
import org.ton.java.cell.CellBuilder;
import org.ton.java.cell.CellSlice;

@Builder
@Getter
@Setter
@ToString
public class ConsensusConfigV1 implements ConsensusConfig {
    int magic;
    int roundCandidates;
    long nextCandidateDelayMs;
    long consensusTimeoutMs;
    long fastAttempts;
    long attemptDuration;
    long catchainNaxDeps;
    long maxBlockBytes;
    long maxCollatedBytes;

    public Cell toCell() {
        return CellBuilder.beginCell()
                .storeUint(0xd6, 8)
                .storeUint(roundCandidates, 32)
                .storeUint(nextCandidateDelayMs, 32)
                .storeUint(consensusTimeoutMs, 32)
                .storeUint(fastAttempts, 32)
                .storeUint(attemptDuration, 32)
                .storeUint(catchainNaxDeps, 32)
                .storeUint(maxBlockBytes, 32)
                .storeUint(maxCollatedBytes, 32)
                .endCell();
    }

    public static ConsensusConfigV1 deserialize(CellSlice cs) {
        return ConsensusConfigV1.builder()
                .magic(cs.loadUint(8).intValue())
                .roundCandidates(cs.loadUint(32).intValue())
                .nextCandidateDelayMs(cs.loadUint(32).longValue())
                .consensusTimeoutMs(cs.loadUint(32).longValue())
                .fastAttempts(cs.loadUint(32).longValue())
                .attemptDuration(cs.loadUint(32).longValue())
                .catchainNaxDeps(cs.loadUint(32).longValue())
                .maxBlockBytes(cs.loadUint(32).longValue())
                .maxCollatedBytes(cs.loadUint(32).longValue())
                .build();
    }
}
