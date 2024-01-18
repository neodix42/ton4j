package org.ton.java.tlb.types;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.ton.java.cell.Cell;
import org.ton.java.cell.CellBuilder;
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
                        k -> CellBuilder.beginCell().storeUint((Long) k, 256).bits,
                        v -> CellBuilder.beginCell().storeCell(((LibDescr) v).toCell())))
                .storeCell(masterRef.toCell())
                .endCell();
    }

}