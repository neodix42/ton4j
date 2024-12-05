package org.ton.java.tlb.types;

import java.math.BigInteger;
import lombok.Builder;
import lombok.Data;
import org.ton.java.cell.Cell;
import org.ton.java.cell.CellBuilder;
import org.ton.java.cell.CellSlice;

@Builder
@Data
/**
 *
 *
 * <pre>{@code
 *     burning_config#01
 *   blackhole_addr:(Maybe bits256)
 *   fee_burn_num:#
 *   fee_burn_denom:# { fee_burn_num <= fee_burn_denom } { fee_burn_denom >= 1 } = BurningConfig;
 * _ BurningConfig = ConfigParam 5;
 * }</pre>
 */
public class ConfigParams5 {
  long magic;
  BigInteger blackholeAddr;
  long feeBurnNum;
  long feeBurnDenom;

  public Cell toCell() {
    return CellBuilder.beginCell()
        .storeUint(01, 8)
        .storeBit(true)
        .storeUint(blackholeAddr, 256)
        .storeUint(feeBurnNum, 32)
        .storeUint(feeBurnDenom, 32)
        .endCell();
  }

  public static ConfigParams5 deserialize(CellSlice cs) {
    return ConfigParams5.builder()
        .magic(cs.loadUint(8).longValue())
        .blackholeAddr(cs.loadBit() ? cs.loadUint(256) : null)
        .feeBurnNum(cs.loadUint(32).longValue())
        .feeBurnDenom(cs.loadUint(32).longValue())
        .build();
  }
}
