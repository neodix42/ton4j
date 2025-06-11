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
 *   ^[ overload_history:uint64
 *     underload_history:uint64
 *     total_balance:CurrencyCollection
 *     total_validator_fees:CurrencyCollection
 *     libraries:(HashmapE 256 LibDescr)
 *     master_ref:(Maybe BlkMasterInfo) ]
 * </pre>
 */
@Builder
@Data
public class ShardStateInfo implements Serializable {

  BigInteger overloadHistory;
  BigInteger underloadHistory;
  CurrencyCollection totalBalance;
  CurrencyCollection totalValidatorFees;
  TonHashMapE libraries;
  ExtBlkRef masterRef;

  public Cell toCell() {
    return CellBuilder.beginCell()
        .storeUint(overloadHistory, 64)
        .storeUint(underloadHistory, 64)
        .storeCell(totalBalance.toCell())
        .storeCell(totalValidatorFees.toCell())
        .storeDict(
            libraries.serialize(
                k -> CellBuilder.beginCell().storeUint((BigInteger) k, 256).endCell().getBits(),
                v -> CellBuilder.beginCell().storeCell(((LibDescr) v).toCell()).endCell()))
        .storeCellMaybe(masterRef.toCell())
        .endCell();
  }

  public static ShardStateInfo deserialize(CellSlice cs) {
    if (!cs.isExotic()) {
      return ShardStateInfo.builder()
          .overloadHistory(cs.loadUint(64))
          .underloadHistory(cs.loadUint(64))
          .totalBalance(CurrencyCollection.deserialize(cs))
          .totalValidatorFees(CurrencyCollection.deserialize(cs))
          .libraries(
              cs.loadDictE(
                  256, k -> k.readUint(256), v -> LibDescr.deserialize(CellSlice.beginParse(v))))
          .masterRef(cs.loadBit() ? ExtBlkRef.deserialize(CellSlice.beginParse(cs)) : null)
          .build();
    } else {
      return ShardStateInfo.builder().build();
    }
  }
}
