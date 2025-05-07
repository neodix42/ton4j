package org.ton.java.tlb;

import java.io.Serializable;
import lombok.Builder;
import lombok.Data;
import org.ton.java.cell.Cell;
import org.ton.java.cell.CellBuilder;
import org.ton.java.cell.CellSlice;

/**
 *
 *
 * <pre>
 * consensus_config_v4#d9 flags:(## 7) { flags = 0 } new_catchain_ids:Bool
 *   round_candidates:(## 8) { round_candidates &gt;= 1 }
 *   next_candidate_delay_ms:uint32 consensus_timeout_ms:uint32
 *   fast_attempts:uint32 attempt_duration:uint32 catchain_max_deps:uint32
 *   max_block_bytes:uint32 max_collated_bytes:uint32
 *   proto_version:uint16 catchain_max_blocks_coeff:uint32 = ConsensusConfig;
 * </pre>
 */
@Builder
@Data
public class ConsensusConfigV4 implements ConsensusConfig, Serializable {
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
  long catchainMaxBlocksCoeff;

  public Cell toCell() {
    return CellBuilder.beginCell()
        .storeUint(0xd8, 8)
        .storeUint(flags, 7)
        .storeBit(newCatchainIds)
        .storeUint(roundCandidates, 8)
        .storeUint(nextCandidateDelayMs, 32)
        .storeUint(consensusTimeoutMs, 32)
        .storeUint(fastAttempts, 32)
        .storeUint(attemptDuration, 32)
        .storeUint(catchainNaxDeps, 32)
        .storeUint(maxBlockBytes, 32)
        .storeUint(maxCollatedBytes, 32)
        .storeUint(protoVersion, 16)
        .storeUint(catchainMaxBlocksCoeff, 32)
        .endCell();
  }

  public static ConsensusConfigV4 deserialize(CellSlice cs) {
    return ConsensusConfigV4.builder()
        .magic(cs.loadUint(8).intValue())
        .flags(cs.loadUint(7).intValue())
        .newCatchainIds(cs.loadBit())
        .roundCandidates(cs.loadUint(8).intValue())
        .nextCandidateDelayMs(cs.loadUint(32).longValue())
        .consensusTimeoutMs(cs.loadUint(32).longValue())
        .fastAttempts(cs.loadUint(32).longValue())
        .attemptDuration(cs.loadUint(32).longValue())
        .catchainNaxDeps(cs.loadUint(32).longValue())
        .maxBlockBytes(cs.loadUint(32).longValue())
        .maxCollatedBytes(cs.loadUint(32).longValue())
        .protoVersion(cs.loadUint(16).intValue())
        .catchainMaxBlocksCoeff(cs.loadUint(32).longValue())
        .build();
  }
}
