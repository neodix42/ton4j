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
 *   msg_forward_prices#ea lump_price:uint64 bit_price:uint64 cell_price:uint64
 *   ihr_price_factor:uint32 first_frac:uint16 next_frac:uint16 = MsgForwardPrices;
 *
 * // used for all other messages
 * config_fwd_prices#_ MsgForwardPrices = ConfigParam 25;
 * </pre>
 */
@Builder
@Data
public class ConfigParams25 implements Serializable {
  MsgForwardPrices configFwdPrices;

  public Cell toCell() {
    return CellBuilder.beginCell().storeCell(configFwdPrices.toCell()).endCell();
  }

  public static ConfigParams25 deserialize(CellSlice cs) {
    return ConfigParams25.builder().configFwdPrices(MsgForwardPrices.deserialize(cs)).build();
  }
}
