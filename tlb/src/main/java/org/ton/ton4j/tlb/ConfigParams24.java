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
 *   msg_forward_prices#ea lump_price:uint64 bit_price:uint64 cell_price:uint64
 *   ihr_price_factor:uint32 first_frac:uint16 next_frac:uint16 = MsgForwardPrices;
 *
 * // used for messages to/from masterchain
 * config_mc_fwd_prices#_ MsgForwardPrices = ConfigParam 24;
 * </pre>
 */
@Builder
@Data
public class ConfigParams24 implements Serializable {
  MsgForwardPrices configMcFwdPrices;

  public Cell toCell() {
    return CellBuilder.beginCell().storeCell(configMcFwdPrices.toCell()).endCell();
  }

  public static ConfigParams24 deserialize(CellSlice cs) {
    return ConfigParams24.builder().configMcFwdPrices(MsgForwardPrices.deserialize(cs)).build();
  }
}
