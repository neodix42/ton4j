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
 * cfg_vote_cfg#36
 * min_tot_rounds:uint8
 * max_tot_rounds:uint8
 * min_wins:uint8
 * max_losses:uint8 min_store_sec:uint32 max_store_sec:uint32
 * bit_price:uint32 cell_price:uint32 = ConfigProposalSetup;
 *
 * </pre>
 */
@Builder
@Data
public class ConfigProposalSetup implements Serializable {
  int cfgVoteCfg;
  int minTotRounds;
  int maxTotRounds;
  int minWins;
  int maxLosses;
  int minStoreSec;
  int maxStoreSec;
  int bitPrice;
  int cellPrice;

  public Cell toCell() {
    return CellBuilder.beginCell()
        .storeUint(0x36, 1)
        .storeUint(minTotRounds, 8)
        .storeUint(maxTotRounds, 8)
        .storeUint(minWins, 8)
        .storeUint(maxLosses, 8)
        .storeUint(minStoreSec, 32)
        .storeUint(maxStoreSec, 32)
        .storeUint(bitPrice, 32)
        .storeUint(cellPrice, 32)
        .endCell();
  }

  public static ConfigProposalSetup deserialize(CellSlice cs) {
    return ConfigProposalSetup.builder()
        .cfgVoteCfg(cs.loadUint(8).intValue())
        .minTotRounds(cs.loadUint(8).intValue())
        .maxTotRounds(cs.loadUint(8).intValue())
        .minWins(cs.loadUint(8).intValue())
        .maxLosses(cs.loadUint(8).intValue())
        .minStoreSec(cs.loadUint(32).intValue())
        .maxStoreSec(cs.loadUint(32).intValue())
        .bitPrice(cs.loadUint(32).intValue())
        .cellPrice(cs.loadUint(32).intValue())
        .build();
  }
}
