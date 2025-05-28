package org.ton.ton4j.tlb;

import java.io.Serializable;
import java.math.BigInteger;
import lombok.Builder;
import lombok.Data;
import org.ton.ton4j.cell.Cell;
import org.ton.ton4j.cell.CellBuilder;
import org.ton.ton4j.cell.CellSlice;
import org.ton.ton4j.cell.TonHashMapE;

/**
 *
 *
 * <pre>
 * precompiled_smc#b0 gas_usage:uint64 = PrecompiledSmc;
 * precompiled_contracts_config#c0 list:(HashmapE 256 PrecompiledSmc) = PrecompiledContractsConfig;
 * _ PrecompiledContractsConfig = ConfigParam 45;
 * </pre>
 */
@Builder
@Data
public class ConfigParams45 implements Serializable {
  int magic;
  TonHashMapE precompiledContractsList;
  long suspendedUntil;

  public Cell toCell() {

    return CellBuilder.beginCell()
        .storeUint(0xc0, 8)
        .storeDict(
            precompiledContractsList.serialize(
                k -> CellBuilder.beginCell().storeUint((BigInteger) k, 256).endCell().getBits(),
                v -> CellBuilder.beginCell().storeCell(((PrecompiledSmc) v).toCell()).endCell()))
        .storeUint(suspendedUntil, 32)
        .endCell();
  }

  public static ConfigParams45 deserialize(CellSlice cs) {
    return ConfigParams45.builder()
        .magic(cs.loadUint(8).intValue())
        .precompiledContractsList(
            cs.loadDictE(
                256,
                k -> k.readUint(256),
                v -> PrecompiledSmc.deserialize(CellSlice.beginParse(v))))
        .build();
  }
}
