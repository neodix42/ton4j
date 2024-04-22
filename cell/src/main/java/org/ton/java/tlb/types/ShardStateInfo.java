package org.ton.java.tlb.types;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.ton.java.cell.Cell;
import org.ton.java.cell.CellBuilder;
import org.ton.java.cell.CellSlice;
import org.ton.java.cell.TonHashMapE;

import java.math.BigInteger;

@Builder
@Getter
@Setter
@ToString
/**
 *   ^[ overload_history:uint64
 *     underload_history:uint64
 *     total_balance:CurrencyCollection
 *     total_validator_fees:CurrencyCollection
 *     libraries:(HashmapE 256 LibDescr)
 *     master_ref:(Maybe BlkMasterInfo) ]
 */
public class ShardStateInfo {

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
                .storeDict(libraries.serialize(
                        k -> CellBuilder.beginCell().storeUint((Long) k, 256).endCell().bits,
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
                    .libraries(cs.loadDictE(256,
                            k -> k.readInt(256),
                            v -> LibDescr.deserialize(CellSlice.beginParse(v))))
                    .masterRef(cs.loadBit() ? ExtBlkRef.deserialize(CellSlice.beginParse(cs)) : null)
                    .build();
        } else {
            return ShardStateInfo.builder().build();
        }
    }
}