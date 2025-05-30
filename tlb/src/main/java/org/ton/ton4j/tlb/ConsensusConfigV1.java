package org.ton.ton4j.tlb;

import java.io.Serializable;
import lombok.Builder;
import lombok.Data;
import org.ton.ton4j.cell.Cell;
import org.ton.ton4j.cell.CellBuilder;
import org.ton.ton4j.cell.CellSlice;

/**
 *
 *
 * <pre>
 *   consensus_config#d6 round_candidates:# { round_candidates &gt;= 1 }
 *   next_candidate_delay_ms:uint32 consensus_timeout_ms:uint32
 *   fast_attempts:uint32 attempt_duration:uint32 catchain_max_deps:uint32
 *   max_block_bytes:uint32 max_collated_bytes:uint32 = ConsensusConfig;
 *
 */
@Builder
@Data
public class ConsensusConfigV1 implements ConsensusConfig, Serializable {
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
