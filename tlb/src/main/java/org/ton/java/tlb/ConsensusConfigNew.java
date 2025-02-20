package org.ton.java.tlb;

import lombok.Builder;
import lombok.Data;
import org.ton.java.cell.Cell;
import org.ton.java.cell.CellBuilder;
import org.ton.java.cell.CellSlice;

@Builder
@Data
public class ConsensusConfigNew implements ConsensusConfig {
  int magic;
  int flags;
  boolean newCatchainIds;
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
        .storeUint(0xd7, 8)
        .storeUint(flags, 7)
        .storeBit(newCatchainIds)
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

  public static ConsensusConfigNew deserialize(CellSlice cs) {
    return ConsensusConfigNew.builder()
        .magic(cs.loadUint(8).intValue())
        .flags(cs.loadUint(7).intValue())
        .newCatchainIds(cs.loadBit())
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
