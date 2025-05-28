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
 *   consensus_config_v3#d8 flags:(## 7) { flags = 0 } new_catchain_ids:Bool
 *   round_candidates:(## 8) { round_candidates &gt;= 1 }
 *   next_candidate_delay_ms:uint32 consensus_timeout_ms:uint32
 *   fast_attempts:uint32 attempt_duration:uint32 catchain_max_deps:uint32
 *   max_block_bytes:uint32 max_collated_bytes:uint32
 *   proto_version:uint16 = ConsensusConfig;
 * </pre>
 */
@Builder
@Data
public class ConsensusConfigV3 implements ConsensusConfig, Serializable {
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
  int protoVersion;

  public Cell toCell() {
    return CellBuilder.beginCell()
        .storeUint(0xd8, 8)
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
        .storeUint(protoVersion, 16)
        .endCell();
  }

  public static ConsensusConfigV3 deserialize(CellSlice cs) {
    return ConsensusConfigV3.builder()
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
        .protoVersion(cs.loadUint(16).intValue())
        .build();
  }
}
