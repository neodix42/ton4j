package org.ton.java.tlb;

import org.ton.java.cell.Cell;
import org.ton.java.cell.CellSlice;

/**
 *
 *
 * <pre>
 *   consensus_config#d6 round_candidates:# { round_candidates >= 1 }
 *   next_candidate_delay_ms:uint32 consensus_timeout_ms:uint32
 *   fast_attempts:uint32 attempt_duration:uint32 catchain_max_deps:uint32
 *   max_block_bytes:uint32 max_collated_bytes:uint32 = ConsensusConfig;
 *
 * consensus_config_new#d7 flags:(## 7) { flags = 0 } new_catchain_ids:Bool
 *   round_candidates:(## 8) { round_candidates >= 1 }
 *   next_candidate_delay_ms:uint32 consensus_timeout_ms:uint32
 *   fast_attempts:uint32 attempt_duration:uint32 catchain_max_deps:uint32
 *   max_block_bytes:uint32 max_collated_bytes:uint32 = ConsensusConfig;
 *
 * consensus_config_v3#d8 flags:(## 7) { flags = 0 } new_catchain_ids:Bool
 *   round_candidates:(## 8) { round_candidates >= 1 }
 *   next_candidate_delay_ms:uint32 consensus_timeout_ms:uint32
 *   fast_attempts:uint32 attempt_duration:uint32 catchain_max_deps:uint32
 *   max_block_bytes:uint32 max_collated_bytes:uint32
 *   proto_version:uint16 = ConsensusConfig;
 *
 * consensus_config_v4#d9 flags:(## 7) { flags = 0 } new_catchain_ids:Bool
 *   round_candidates:(## 8) { round_candidates >= 1 }
 *   next_candidate_delay_ms:uint32 consensus_timeout_ms:uint32
 *   fast_attempts:uint32 attempt_duration:uint32 catchain_max_deps:uint32
 *   max_block_bytes:uint32 max_collated_bytes:uint32
 *   proto_version:uint16 catchain_max_blocks_coeff:uint32 = ConsensusConfig;
 * </pre>
 */
public interface ConsensusConfig {

  Cell toCell();

  static ConsensusConfig deserialize(CellSlice cs) {
    int magic = cs.preloadUint(8).intValue();
    if (magic == 0xd6) {
      return ConsensusConfigV1.deserialize(cs);
    } else if (magic == 0xd7) {
      return ConsensusConfigNew.deserialize(cs);
    } else if (magic == 0xd8) {
      return ConsensusConfigV3.deserialize(cs);
    } else if (magic == 0xd9) {
      return ConsensusConfigV4.deserialize(cs);
    } else {
      throw new Error("Wrong magic in ConsensusConfig: " + magic);
    }
  }
}
