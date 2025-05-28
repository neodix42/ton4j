package org.ton.ton4j.tlb;

import lombok.Builder;
import lombok.Data;
import org.ton.ton4j.cell.Cell;
import org.ton.ton4j.cell.CellBuilder;
import org.ton.ton4j.cell.CellSlice;

import java.io.Serializable;

/**
 *
 *
 * <pre>
 *   consensus_config#d6
 *   round_candidates:# { round_candidates &gt;= 1 }
 *   next_candidate_delay_ms:uint32
 *   consensus_timeout_ms:uint32
 *   fast_attempts:uint32
 *   attempt_duration:uint32
 *   catchain_max_deps:uint32
 *   max_block_bytes:uint32
 *   max_collated_bytes:uint32 = ConsensusConfig;
 *
 * consensus_config_new#d7
 * flags:(## 7) { flags = 0 } new_catchain_ids:Bool
 *   round_candidates:(## 8) { round_candidates &gt;= 1 }
 *   next_candidate_delay_ms:uint32
 *   consensus_timeout_ms:uint32
 *   fast_attempts:uint32
 *   attempt_duration:uint32
 *   catchain_max_deps:uint32
 *   max_block_bytes:uint32
 *   max_collated_bytes:uint32 = ConsensusConfig;
 *
 * consensus_config_v3#d8 flags:(## 7) { flags = 0 } new_catchain_ids:Bool
 *   round_candidates:(## 8) { round_candidates &gt;= 1 }
 *   next_candidate_delay_ms:uint32
 *   consensus_timeout_ms:uint32
 *   fast_attempts:uint32
 *   attempt_duration:uint32
 *   catchain_max_deps:uint32
 *   max_block_bytes:uint32
 *   max_collated_bytes:uint32
 *   proto_version:uint16 = ConsensusConfig;
 *
 * consensus_config_v4#d9 flags:(## 7) { flags = 0 } new_catchain_ids:Bool
 *   round_candidates:(## 8) { round_candidates &gt;= 1 }
 *   next_candidate_delay_ms:uint32
 *   consensus_timeout_ms:uint32
 *   fast_attempts:uint32
 *   attempt_duration:uint32
 *   catchain_max_deps:uint32
 *   max_block_bytes:uint32
 *   max_collated_bytes:uint32
 *   proto_version:uint16
 *   catchain_max_blocks_coeff:uint32 = ConsensusConfig;
 *
 * _ ConsensusConfig = ConfigParam 29;
 * </pre>
 */
@Builder
@Data
public class ConfigParams29 implements Serializable {
  ConsensusConfig consensusConfig;

  public Cell toCell() {
    return CellBuilder.beginCell().storeCell(consensusConfig.toCell()).endCell();
  }

  public static ConfigParams29 deserialize(CellSlice cs) {
    return ConfigParams29.builder().consensusConfig(ConsensusConfig.deserialize(cs)).build();
  }
}
