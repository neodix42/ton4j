package org.ton.ton4j.tlb;

import lombok.Builder;
import lombok.Data;
import org.ton.ton4j.cell.Cell;
import org.ton.ton4j.cell.CellSlice;

import java.io.Serializable;

/**
 *
 *
 * <pre>
 * cfg_vote_setup#91
 * normal_params:^ConfigProposalSetup
 * critical_params:^ConfigProposalSetup = ConfigVotingSetup;
 * _ ConfigVotingSetup = ConfigParam 11;
 * </pre>
 */
@Builder
@Data
public class ConfigParams11 implements Serializable {
  ConfigVotingSetup configVotingSetup;

  public Cell toCell() {

    return configVotingSetup.toCell();
  }

  public static ConfigParams11 deserialize(CellSlice cs) {
    return ConfigParams11.builder().configVotingSetup(ConfigVotingSetup.deserialize(cs)).build();
  }
}
