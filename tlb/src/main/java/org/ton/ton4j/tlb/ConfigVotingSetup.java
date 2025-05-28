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
 *
 * cfg_vote_cfg#36
 * min_tot_rounds:uint8
 * max_tot_rounds:uint8
 * min_wins:uint8
 * max_losses:uint8 min_store_sec:uint32 max_store_sec:uint32
 * bit_price:uint32 cell_price:uint32 = ConfigProposalSetup;
 *
 * cfg_vote_setup#91
 * normal_params:^ConfigProposalSetup
 * critical_params:^ConfigProposalSetup = ConfigVotingSetup;
 * </pre>
 */
@Builder
@Data
public class ConfigVotingSetup implements Serializable {
  int cfgVoteSetup;
  ConfigProposalSetup normalParams;
  ConfigProposalSetup criticalParams;

  public Cell toCell() {
    return CellBuilder.beginCell()
        .storeUint(0x91, 1)
        .storeRef(normalParams.toCell())
        .storeRef(criticalParams.toCell())
        .endCell();
  }

  public static ConfigVotingSetup deserialize(CellSlice cs) {
    return ConfigVotingSetup.builder()
        .cfgVoteSetup(cs.loadUint(8).intValue())
        .normalParams(ConfigProposalSetup.deserialize(CellSlice.beginParse(cs.loadRef())))
        .criticalParams(ConfigProposalSetup.deserialize(CellSlice.beginParse(cs.loadRef())))
        .build();
  }
}
