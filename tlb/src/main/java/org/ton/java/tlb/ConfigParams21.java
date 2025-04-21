package org.ton.java.tlb;

import lombok.Builder;
import lombok.Data;
import org.ton.java.cell.Cell;
import org.ton.java.cell.CellBuilder;
import org.ton.java.cell.CellSlice;

/**
 *
 *
 * <pre>
 * gas_prices#dd gas_price:uint64 gas_limit:uint64 gas_credit:uint64
 *   block_gas_limit:uint64 freeze_due_limit:uint64 delete_due_limit:uint64
 *   = GasLimitsPrices;
 *
 * gas_prices_ext#de gas_price:uint64 gas_limit:uint64 special_gas_limit:uint64 gas_credit:uint64
 *   block_gas_limit:uint64 freeze_due_limit:uint64 delete_due_limit:uint64
 *   = GasLimitsPrices;
 *
 * // same fields as gas_prices_ext; behavior differs
 * gas_prices_v3#df gas_price:uint64 gas_limit:uint64 special_gas_limit:uint64 gas_credit:uint64
 *   block_gas_limit:uint64 freeze_due_limit:uint64 delete_due_limit:uint64
 *   = GasLimitsPrices;
 *
 * gas_flat_pfx#d1 flat_gas_limit:uint64 flat_gas_price:uint64 other:GasLimitsPrices
 *   = GasLimitsPrices;
 *
 * config_gas_prices#_ GasLimitsPrices = ConfigParam 21;
 * </pre>
 */
@Builder
@Data
public class ConfigParams21 {
  GasLimitsPrices configGasPrices;

  public Cell toCell() {
    return CellBuilder.beginCell().storeCell(configGasPrices.toCell()).endCell();
  }

  public static ConfigParams21 deserialize(CellSlice cs) {
    return ConfigParams21.builder().configGasPrices(GasLimitsPrices.deserialize(cs)).build();
  }
}
