package org.ton.java.tlb.types;

import static java.util.Objects.isNull;

import java.math.BigInteger;
import lombok.Builder;
import lombok.Data;
import org.ton.java.cell.Cell;
import org.ton.java.cell.CellBuilder;
import org.ton.java.cell.CellSlice;

/**
 *
 *
 * <pre>
 * smc_info#076ef1ea
 * actions:uint16
 * msgs_sent:uint16
 * unixtime:uint32
 * block_lt:uint64
 * trans_lt:uint64
 * rand_seed:bits256
 * balance_remaining:CurrencyCollection
 * myself:MsgAddressInt
 * global_config:(Maybe Cell) = SmartContractInfo;
 *   </pre>
 */
@Builder
@Data
public class SmcInfo {
  int magic;
  long actions;
  long msgsSent;
  long unixtime;
  BigInteger blockLt;
  BigInteger transLt;
  BigInteger randSeed;
  CurrencyCollection balanceRemaining;
  MsgAddressInt myself;
  Cell globalConfig;

  public Cell toCell() {
    return CellBuilder.beginCell()
        .storeUint(0x076ef1ea, 32)
        .storeUint(actions, 16)
        .storeUint(msgsSent, 16)
        .storeUint(unixtime, 32)
        .storeUint(isNull(blockLt) ? BigInteger.ZERO : blockLt, 64)
        .storeUint(isNull(transLt) ? BigInteger.ZERO : transLt, 64)
        .storeUint(isNull(randSeed) ? BigInteger.ZERO : randSeed, 256)
        .storeCell(balanceRemaining.toCell())
        .storeCell(myself.toCell())
        .storeCellMaybe(globalConfig)
        .endCell();
  }

  public static SmcInfo deserialize(CellSlice cs) {
    return SmcInfo.builder()
        .magic(cs.loadUint(32).intValue())
        .actions(cs.loadUint(16).longValue())
        .msgsSent(cs.loadUint(16).longValue())
        .unixtime(cs.loadUint(32).longValue())
        .blockLt(cs.loadUint(64))
        .transLt(cs.loadUint(64))
        .randSeed(cs.loadUint(256))
        .balanceRemaining(CurrencyCollection.deserialize(cs))
        .myself(MsgAddressInt.deserialize(cs))
        .globalConfig(CellSlice.beginParse(cs).sliceToCell()) // todo
        .build();
  }
}
