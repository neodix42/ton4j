package org.ton.java.tlb;

import lombok.Builder;
import lombok.Data;
import org.ton.java.cell.Cell;
import org.ton.java.cell.CellSlice;

@Builder
@Data
public class ConfigParams11 {
  ConfigVotingSetup configVotingSetup;

  public Cell toCell() {

    return configVotingSetup.toCell();
  }

  public static ConfigParams11 deserialize(CellSlice cs) {
    return ConfigParams11.builder().configVotingSetup(ConfigVotingSetup.deserialize(cs)).build();
  }
}
