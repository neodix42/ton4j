package org.ton.ton4j.tlb;

import java.io.Serializable;
import java.math.BigInteger;
import lombok.Builder;
import lombok.Data;
import org.ton.ton4j.cell.Cell;
import org.ton.ton4j.cell.CellBuilder;
import org.ton.ton4j.cell.CellSlice;
import org.ton.ton4j.utils.Utils;

/**
 *
 *
 * <pre>
 *   burning_config#01
 *   blackhole_addr:(Maybe bits256)
 *   fee_burn_num:#
 *   fee_burn_denom:# { fee_burn_num &lt;= fee_burn_denom } { fee_burn_denom &gt;= 1 } = BurningConfig;
 * _ BurningConfig = ConfigParam 5;
 * </pre>
 */
@Builder
@Data
public class ConfigParams5 implements Serializable {
  long magic;
  public BigInteger blackholeAddr;
  long feeBurnNum;
  long feeBurnDenom;

  public String getBlackholeAddr() {
    if (blackholeAddr == null) {
      return "";
    }
    return Utils.bytesToHex(Utils.to32ByteArray(blackholeAddr));
  }

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
